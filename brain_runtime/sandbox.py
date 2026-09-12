from __future__ import annotations

import hashlib
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence


@dataclass(frozen=True)
class SandboxJob:
    job_id: str
    run_id: str
    session_id: str
    argv: tuple[str, ...]
    cwd: str
    timeout_seconds: int = 30
    allowed_commands: tuple[str, ...] = ("python3",)
    network_allowed: bool = False


@dataclass(frozen=True)
class SandboxJobResult:
    job_id: str
    status: str
    exit_code: int | None
    stdout: str
    stderr: str
    artifacts: tuple[dict, ...] = ()
    diagnostics: tuple[str, ...] = ()


class SandboxExecutor:
    def execute(self, job: SandboxJob) -> SandboxJobResult:
        if not job.argv or job.argv[0] not in job.allowed_commands:
            return SandboxJobResult(job.job_id, "REJECTED", None, "", "", diagnostics=("command not allowlisted",))
        root = Path(job.cwd).resolve()
        if not root.exists() or not root.is_dir():
            return SandboxJobResult(job.job_id, "REJECTED", None, "", "", diagnostics=("cwd is not a directory",))
        started = time.monotonic()
        try:
            completed = subprocess.run(list(job.argv), cwd=root, capture_output=True, text=True, timeout=job.timeout_seconds, check=False, env={"PATH": "/usr/bin:/bin", "PYTHONNOUSERSITE": "1"})
            status = "SUCCEEDED" if completed.returncode == 0 else "FAILED"
            artifacts = []
            for path in root.rglob("*"):
                if path.is_file() and path.stat().st_size <= 10 * 1024 * 1024:
                    artifacts.append({"path": str(path.relative_to(root)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "size": path.stat().st_size})
            return SandboxJobResult(job.job_id, status, completed.returncode, completed.stdout, completed.stderr, tuple(artifacts), (f"duration_ms={int((time.monotonic()-started)*1000)}",))
        except subprocess.TimeoutExpired as error:
            return SandboxJobResult(job.job_id, "TIMEOUT", None, error.stdout or "", error.stderr or "", diagnostics=("execution deadline exceeded",))


class QAGate:
    def validate(self, result: SandboxJobResult, required_output: str | None = None) -> tuple[bool, tuple[str, ...]]:
        diagnostics = list(result.diagnostics)
        if result.status != "SUCCEEDED":
            diagnostics.append(f"sandbox status is {result.status}")
        if required_output and required_output not in result.stdout:
            diagnostics.append("required output not found")
        return not diagnostics or all(item.startswith("duration_ms=") for item in diagnostics), tuple(diagnostics)
