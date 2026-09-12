from __future__ import annotations
from dataclasses import dataclass
from typing import Any, Mapping
from .contracts import ContractError, validate_payload

@dataclass(frozen=True)
class ContractSchema:
    name: str; version: int; required: tuple[str, ...]; deprecated: tuple[str, ...] = ()
    types: dict[str, type] | None = None

class ContractRegistry:
    def __init__(self): self._schemas: dict[tuple[str, int], ContractSchema] = {}
    def register(self, schema: ContractSchema) -> None:
        if schema.version < 1 or not schema.name: raise ValueError("invalid contract schema")
        key = (schema.name, schema.version)
        if key in self._schemas: raise ValueError("contract schema already registered")
        self._schemas[key] = schema
    def validate(self, name: str, payload: Mapping[str, Any], version: int | None = None) -> dict[str, Any]:
        versions = [item for (schema_name, _), item in self._schemas.items() if schema_name == name]
        if not versions: raise ContractError(f"unknown contract: {name}")
        selected = next((schema for schema in versions if schema.version == version), max(versions, key=lambda item: item.version))
        result = validate_payload(payload, selected.required, selected.version)
        if any(field in result for field in selected.deprecated): raise ContractError("payload uses deprecated field")
        for field, expected in (selected.types or {}).items():
            if field in result and not isinstance(result[field], expected): raise ContractError(f"invalid type for field: {field}")
        return result
    def latest(self, name: str) -> ContractSchema:
        matches = [schema for (schema_name, _), schema in self._schemas.items() if schema_name == name]
        if not matches: raise KeyError(name)
        return max(matches, key=lambda item: item.version)
    @classmethod
    def standard(cls) -> "ContractRegistry":
        registry = cls()
        registry.register(ContractSchema("TaskSpec", 1, ("task_id", "session_id", "objective"), types={"task_id": str, "session_id": str, "objective": str}))
        registry.register(ContractSchema("Plan", 1, ("plan_id", "task_id", "steps"), types={"plan_id": str, "task_id": str, "steps": (list, tuple)}))
        registry.register(ContractSchema("PlanStep", 1, ("step_id", "capability"), types={"step_id": str, "capability": str}))
        registry.register(ContractSchema("ExecutionRequest", 1, ("request_id", "run_id", "task_id", "step_id", "capability", "objective", "policy_decision_id"), types={"request_id": str, "run_id": str, "task_id": str, "step_id": str, "capability": str, "objective": str, "policy_decision_id": str}))
        registry.register(ContractSchema("ExecutionResult", 1, ("request_id", "success", "status"), types={"request_id": str, "success": bool, "status": str}))
        registry.register(ContractSchema("SandboxJob", 1, ("job_id", "run_id", "session_id", "cwd"), types={"job_id": str, "run_id": str, "session_id": str, "cwd": str}))
        registry.register(ContractSchema("SandboxJobResult", 1, ("job_id", "status", "run_id", "session_id"), types={"job_id": str, "status": str, "run_id": str, "session_id": str}))
        registry.register(ContractSchema("Event", 1, ("event_id", "run_id", "session_id", "task_id", "type", "version", "sequence", "payload"), types={"event_id": str, "run_id": str, "session_id": str, "task_id": str, "type": str, "version": int, "sequence": int, "payload": dict}))
        return registry
