from __future__ import annotations
import hashlib, os, resource, signal, subprocess, time
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence

@dataclass(frozen=True)
class SandboxJob:
    job_id: str; run_id: str; session_id: str; argv: tuple[str, ...]; cwd: str
    timeout_seconds: int = 30; allowed_commands: tuple[str, ...] = ("python3",); network_allowed: bool = False
    filesystem_roots: tuple[str, ...] = (); max_stdout_bytes: int = 1_048_576; max_stderr_bytes: int = 1_048_576
    max_artifacts: int = 100; max_artifact_bytes: int = 10 * 1024 * 1024; memory_mb: int = 512
    cancel_event: object | None = None

@dataclass(frozen=True)
class SandboxJobResult:
    job_id: str; status: str; exit_code: int | None; stdout: str; stderr: str
    artifacts: tuple[dict, ...] = (); diagnostics: tuple[str, ...] = ()
    run_id: str = ""; session_id: str = ""; duration_ms: int = 0

def _limit_process(memory_mb: int) -> None:
    try:
        resource.setrlimit(resource.RLIMIT_CPU, (60, 60))
        resource.setrlimit(resource.RLIMIT_AS, (memory_mb * 1024 * 1024, memory_mb * 1024 * 1024))
        resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
    except (OSError, ValueError): pass

def _clip(value: str | bytes | None, limit: int) -> str:
    if value is None: return ""
    if isinstance(value, bytes): value = value.decode(errors="replace")
    return value[:limit]

class SandboxExecutor:
    def execute(self, job: SandboxJob) -> SandboxJobResult:
        if not job.argv or job.argv[0] not in job.allowed_commands or any(token in job.argv[0] for token in ("/", "\\")):
            return SandboxJobResult(job.job_id, "REJECTED", None, "", "", diagnostics=("command not allowlisted",), run_id=job.run_id, session_id=job.session_id)
        if job.timeout_seconds <= 0 or job.max_stdout_bytes < 0 or job.max_stderr_bytes < 0:
            return SandboxJobResult(job.job_id, "REJECTED", None, "", "", diagnostics=("invalid resource limits",), run_id=job.run_id, session_id=job.session_id)
        root = Path(job.cwd).resolve()
        if not root.exists() or not root.is_dir() or any(part.is_symlink() for part in [root, *root.parents]):
            return SandboxJobResult(job.job_id, "REJECTED", None, "", "", diagnostics=("cwd is unsafe",), run_id=job.run_id, session_id=job.session_id)
        if job.filesystem_roots and not any(root == Path(item).resolve() or Path(item).resolve() in root.parents for item in job.filesystem_roots):
            return SandboxJobResult(job.job_id, "REJECTED", None, "", "", diagnostics=("cwd outside filesystem policy",), run_id=job.run_id, session_id=job.session_id)
        started = time.monotonic()
        process = None
        try:
            process = subprocess.Popen(list(job.argv), cwd=root, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                env={"PATH": "/usr/bin:/bin", "PYTHONNOUSERSITE": "1", "HOME": str(root)},
                start_new_session=True, preexec_fn=lambda: _limit_process(job.memory_mb))
            deadline = started + job.timeout_seconds
            while True:
                if job.cancel_event is not None and getattr(job.cancel_event, "is_set", lambda: False)():
                    os.killpg(process.pid, signal.SIGKILL)
                    stdout, stderr = process.communicate()
                    return SandboxJobResult(job.job_id, "CANCELLED", process.returncode, _clip(stdout, job.max_stdout_bytes), _clip(stderr, job.max_stderr_bytes), diagnostics=("cancelled by caller",), run_id=job.run_id, session_id=job.session_id)
                if time.monotonic() >= deadline:
                    os.killpg(process.pid, signal.SIGKILL)
                    stdout, stderr = process.communicate()
                    return SandboxJobResult(job.job_id, "TIMEOUT", process.returncode, _clip(stdout, job.max_stdout_bytes), _clip(stderr, job.max_stderr_bytes), diagnostics=("execution deadline exceeded",), run_id=job.run_id, session_id=job.session_id)
                try:
                    stdout, stderr = process.communicate(timeout=min(.1, max(.01, deadline - time.monotonic())))
                    break
                except subprocess.TimeoutExpired:
                    continue
            class Completed:
                returncode = process.returncode
            completed = Completed(); completed.stdout, completed.stderr = stdout, stderr
            stdout, stderr = _clip(completed.stdout, job.max_stdout_bytes), _clip(completed.stderr, job.max_stderr_bytes)
            diagnostics = []
            if len(completed.stdout or b"") > job.max_stdout_bytes: diagnostics.append("stdout limit exceeded")
            if len(completed.stderr or b"") > job.max_stderr_bytes: diagnostics.append("stderr limit exceeded")
            artifacts = []
            for path in root.rglob("*"):
                if path.is_symlink(): return SandboxJobResult(job.job_id, "REJECTED", completed.returncode, stdout, stderr, diagnostics=("symlink artifact rejected",), run_id=job.run_id, session_id=job.session_id)
                if path.is_file():
                    size = path.stat().st_size
                    if size > job.max_artifact_bytes: diagnostics.append(f"artifact limit exceeded: {path.name}"); continue
                    if len(artifacts) >= job.max_artifacts: diagnostics.append("artifact count limit exceeded"); break
                    artifacts.append({"path": str(path.relative_to(root)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "size": size})
            status = "SUCCEEDED" if completed.returncode == 0 and not diagnostics else ("FAILED" if completed.returncode != 0 else "REJECTED")
            diagnostics.append(f"duration_ms={int((time.monotonic()-started)*1000)}")
            return SandboxJobResult(job.job_id, status, completed.returncode, stdout, stderr, tuple(artifacts), tuple(diagnostics), job.run_id, job.session_id, int((time.monotonic()-started)*1000))
        except subprocess.TimeoutExpired as error:
            return SandboxJobResult(job.job_id, "TIMEOUT", None, _clip(error.stdout, job.max_stdout_bytes), _clip(error.stderr, job.max_stderr_bytes), diagnostics=("execution deadline exceeded",), run_id=job.run_id, session_id=job.session_id)

class QAGate:
    def validate(self, result: SandboxJobResult, required_output: str | None = None) -> tuple[bool, tuple[str, ...]]:
        diagnostics = [item for item in result.diagnostics if not item.startswith("duration_ms=")]
        if result.status != "SUCCEEDED": diagnostics.append(f"sandbox status is {result.status}")
        if required_output and required_output not in result.stdout: diagnostics.append("required output not found")
        return not diagnostics, tuple(diagnostics)
