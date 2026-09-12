from __future__ import annotations
from dataclasses import fields, is_dataclass
from typing import Any, Mapping

CURRENT_CONTRACT_VERSION = 1

class ContractError(ValueError):
    pass

def validate_contract(value: Any, required: tuple[str, ...] = (), version: int = CURRENT_CONTRACT_VERSION) -> Any:
    if not is_dataclass(value):
        raise ContractError("contract must be a dataclass instance")
    names = {item.name for item in fields(value)}
    missing = [name for name in required if name not in names or getattr(value, name, None) in (None, "")]
    if missing: raise ContractError(f"missing required fields: {', '.join(missing)}")
    if version != CURRENT_CONTRACT_VERSION: raise ContractError(f"unsupported contract version: {version}")
    return value

def validate_payload(payload: Mapping[str, Any], required: tuple[str, ...], version: int = CURRENT_CONTRACT_VERSION) -> dict[str, Any]:
    if not isinstance(payload, Mapping): raise ContractError("payload must be an object")
    if payload.get("schemaVersion", version) != version: raise ContractError("unsupported schema version")
    missing = [name for name in required if name not in payload]
    if missing: raise ContractError(f"missing required fields: {', '.join(missing)}")
    return dict(payload)

def redact_secrets(payload: Any) -> Any:
    if isinstance(payload, dict):
        secret = {"secret", "token", "password", "api_key", "credential", "authorization"}
        return {key: "[REDACTED]" if str(key).lower() in secret else redact_secrets(value) for key, value in payload.items()}
    if isinstance(payload, list): return [redact_secrets(item) for item in payload]
    return payload
