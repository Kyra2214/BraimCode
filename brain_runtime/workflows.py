from __future__ import annotations

import json
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Callable


@dataclass(frozen=True)
class WorkflowNode:
    node_id: str
    capability: str
    next_nodes: tuple[str, ...] = ()
    retry_limit: int = 1


@dataclass(frozen=True)
class WorkflowManifest:
    workflow_id: str
    workflow_version: str
    nodes: tuple[WorkflowNode, ...]
    enabled: bool = True


class WorkflowEngine:
    def __init__(self, state_path: str | Path | None = None):
        self.state_path = Path(state_path) if state_path else None
        self._runs: dict[str, dict] = {}
        if self.state_path and self.state_path.exists():
            self._runs = json.loads(self.state_path.read_text(encoding="utf-8"))

    def _save(self):
        if self.state_path:
            self.state_path.parent.mkdir(parents=True, exist_ok=True)
            self.state_path.write_text(json.dumps(self._runs, sort_keys=True), encoding="utf-8")

    def run(self, manifest: WorkflowManifest, run_id: str, idempotency_key: str, handler: Callable[[WorkflowNode], bool]) -> dict:
        if not manifest.enabled:
            raise PermissionError("workflow desabilitado")
        existing = self._runs.get(idempotency_key)
        if existing and existing["status"] == "completed":
            return existing
        nodes = {node.node_id: node for node in manifest.nodes}
        completed = set(existing.get("completed", [])) if existing else set()
        pending = [node for node in manifest.nodes if node.node_id not in completed]
        attempts = existing.get("attempts", {}) if existing else {}
        for node in pending:
            attempt = attempts.get(node.node_id, 0)
            success = False
            while attempt <= node.retry_limit:
                attempt += 1
                attempts[node.node_id] = attempt
                if handler(node):
                    success = True
                    completed.add(node.node_id)
                    break
            if not success:
                state = {"run_id": run_id, "workflow_id": manifest.workflow_id, "workflow_version": manifest.workflow_version, "status": "failed", "completed": sorted(completed), "attempts": attempts}
                self._runs[idempotency_key] = state
                self._save()
                return state
        state = {"run_id": run_id, "workflow_id": manifest.workflow_id, "workflow_version": manifest.workflow_version, "status": "completed", "completed": sorted(completed), "attempts": attempts}
        self._runs[idempotency_key] = state
        self._save()
        return state
