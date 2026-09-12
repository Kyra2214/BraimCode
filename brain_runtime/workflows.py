from __future__ import annotations
import json, os, threading
from datetime import datetime, timedelta, timezone
from dataclasses import dataclass
from pathlib import Path
from typing import Callable
from .models import PolicyContext, Decision
from .policy import PolicyBroker

@dataclass(frozen=True)
class WorkflowNode:
    node_id: str; capability: str; next_nodes: tuple[str, ...] = (); retry_limit: int = 1; dependencies: tuple[str, ...] = ()
@dataclass(frozen=True)
class WorkflowManifest:
    workflow_id: str; workflow_version: str; nodes: tuple[WorkflowNode, ...]; enabled: bool = True
    required_capabilities: tuple[str, ...] = ()

class WorkflowEngine:
    def __init__(self, state_path: str | Path | None = None, policy: PolicyBroker | None = None, actor: str = "workflow"):
        self.state_path = Path(state_path) if state_path else None; self._lock = threading.RLock(); self._runs: dict[str, dict] = {}
        self.policy, self.actor = policy, actor
        if self.state_path and self.state_path.exists():
            try: self._runs = json.loads(self.state_path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as exc: raise ValueError("workflow state is corrupted") from exc
    def _save(self) -> None:
        if not self.state_path: return
        self.state_path.parent.mkdir(parents=True, exist_ok=True); tmp = self.state_path.with_suffix(self.state_path.suffix + ".tmp")
        tmp.write_text(json.dumps(self._runs, sort_keys=True), encoding="utf-8"); os.replace(tmp, self.state_path)
    @staticmethod
    def _validate(manifest: WorkflowManifest) -> dict[str, WorkflowNode]:
        nodes = {n.node_id: n for n in manifest.nodes}
        if len(nodes) != len(manifest.nodes) or any(n.retry_limit < 0 for n in manifest.nodes): raise ValueError("invalid workflow nodes")
        for node in manifest.nodes:
            if any(dep not in nodes for dep in (*node.dependencies, *node.next_nodes)): raise ValueError("workflow dependency references unknown node")
        visiting, visited = set(), set()
        def visit(node_id: str):
            if node_id in visiting: raise ValueError("workflow graph contains a cycle")
            if node_id in visited: return
            visiting.add(node_id)
            for dep in nodes[node_id].dependencies: visit(dep)
            visiting.remove(node_id); visited.add(node_id)
        for node in nodes: visit(node)
        return nodes
    def run(self, manifest: WorkflowManifest, run_id: str, idempotency_key: str, handler: Callable[[WorkflowNode], bool], owner: str = "local", lease_seconds: int = 300) -> dict:
        with self._lock:
            if not manifest.enabled: raise PermissionError("workflow desabilitado")
            nodes = self._validate(manifest); existing = self._runs.get(idempotency_key)
            if existing and existing["status"] == "completed": return existing
            if lease_seconds <= 0: raise ValueError("lease must be positive")
            if existing and existing.get("status") == "running" and existing.get("owner") != owner and existing.get("lease_until", "") > datetime.now(timezone.utc).isoformat():
                raise RuntimeError("workflow run is leased by another owner")
            if self.policy:
                capabilities = set(manifest.required_capabilities) | {node.capability for node in manifest.nodes}
                for capability in capabilities:
                    decision = self.policy.authorize(self.actor, capability, manifest.workflow_id, PolicyContext(run_id, manifest.workflow_id, self.actor))
                    if decision.decision is not Decision.ALLOW:
                        raise PermissionError(f"workflow policy denied '{capability}': {decision.reason}")
            completed = set(existing.get("completed", [])) if existing else set(); attempts = existing.get("attempts", {}) if existing else {}
            pending = [n for n in manifest.nodes if n.node_id not in completed]
            for node in pending:
                if any(dep not in completed for dep in node.dependencies):
                    state = {"run_id": run_id, "workflow_id": manifest.workflow_id, "workflow_version": manifest.workflow_version, "status": "blocked", "completed": sorted(completed), "attempts": attempts}
                    self._runs[idempotency_key] = state; self._save(); return state
                success = False; attempt = attempts.get(node.node_id, 0)
                while attempt <= node.retry_limit:
                    attempt += 1; attempts[node.node_id] = attempt
                    if handler(node): success = True; completed.add(node.node_id); break
                if not success:
                    state = {"run_id": run_id, "workflow_id": manifest.workflow_id, "workflow_version": manifest.workflow_version, "status": "failed", "completed": sorted(completed), "attempts": attempts}
                    self._runs[idempotency_key] = state; self._save(); return state
                self._runs[idempotency_key] = {"run_id": run_id, "workflow_id": manifest.workflow_id, "workflow_version": manifest.workflow_version, "status": "running", "owner": owner, "lease_until": (datetime.now(timezone.utc) + timedelta(seconds=lease_seconds)).isoformat(), "completed": sorted(completed), "attempts": attempts}; self._save()
            state = {"run_id": run_id, "workflow_id": manifest.workflow_id, "workflow_version": manifest.workflow_version, "status": "completed", "completed": sorted(completed), "attempts": attempts}
            self._runs[idempotency_key] = state; self._save(); return state
