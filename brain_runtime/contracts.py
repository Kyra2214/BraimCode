from __future__ import annotations
from dataclasses import fields, is_dataclass, asdict
from typing import Any, Mapping

CURRENT_CONTRACT_VERSION = 1

class ContractError(ValueError):
    pass

def validate_contract(value: Any, required: tuple[str, ...] = (), version: int = CURRENT_CONTRACT_VERSION) -> Any:
    if not is_dataclass(value):
        raise ContractError("contract must be a dataclass instance")
    if version != CURRENT_CONTRACT_VERSION:
        raise ContractError(f"unsupported contract version: {version}")
    missing = [name for name in required if not hasattr(value, name) or getattr(value, name, None) in (None, "")]
    if missing:
        raise ContractError(f"missing required fields: {', '.join(missing)}")
    return value

def validate_payload(payload: Mapping[str, Any], required: tuple[str, ...], version: int = CURRENT_CONTRACT_VERSION) -> dict[str, Any]:
    if not isinstance(payload, Mapping):
        raise ContractError("payload must be an object")
    actual = payload.get("schemaVersion", version)
    if actual != version:
        raise ContractError(f"unsupported schema version: {actual}")
    missing = [name for name in required if name not in payload or payload[name] in (None, "")]
    if missing:
        raise ContractError(f"missing required fields: {', '.join(missing)}")
    return dict(payload)

def contract_payload(value: Any, required: tuple[str, ...] = ()) -> dict[str, Any]:
    validate_contract(value, required)
    result = asdict(value)
    result.setdefault("schemaVersion", CURRENT_CONTRACT_VERSION)
    return result

def migrate_payload(payload: Mapping[str, Any], from_version: int, to_version: int = CURRENT_CONTRACT_VERSION) -> dict[str, Any]:
    if from_version == to_version:
        return dict(payload)
    if from_version == 0 and to_version == 1:
        result = dict(payload)
        result.setdefault("schemaVersion", 1)
        return result
    raise ContractError(f"no migration from schema version {from_version} to {to_version}")

def redact_secrets(payload: Any) -> Any:
    if isinstance(payload, dict):
        secret = {"secret", "token", "password", "api_key", "apikey", "credential", "authorization"}
        return {key: "[REDACTED]" if str(key).lower() in secret else redact_secrets(value) for key, value in payload.items()}
    if isinstance(payload, (list, tuple)):
        return [redact_secrets(item) for item in payload]
    return payload

CONTRACT_REQUIRED_FIELDS = {
    "TaskSpec": ("task_id", "session_id", "objective"),
    "Plan": ("plan_id", "task_id", "steps"),
    "ExecutionRequest": ("request_id", "run_id", "task_id", "step_id", "capability", "objective", "policy_decision_id"),
    "ExecutionResult": ("request_id", "status", "success"),
    "SandboxJob": ("job_id", "run_id", "session_id", "cwd"),
    "SandboxJobResult": ("job_id", "status", "run_id", "session_id"),
}


def validate_named_contract(name: str, value: Any) -> Any:
    if name not in CONTRACT_REQUIRED_FIELDS:
        raise ContractError(f"unknown contract: {name}")
    return validate_contract(value, CONTRACT_REQUIRED_FIELDS[name])
