from __future__ import annotations
from dataclasses import dataclass
from typing import Any, Mapping
from .contracts import ContractError, validate_payload

@dataclass(frozen=True)
class ContractSchema:
    name: str
    version: int
    required: tuple[str, ...]
    deprecated: tuple[str, ...] = ()

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
        return result
    def latest(self, name: str) -> ContractSchema:
        matches = [schema for (schema_name, _), schema in self._schemas.items() if schema_name == name]
        if not matches: raise KeyError(name)
        return max(matches, key=lambda item: item.version)
