from __future__ import annotations
import os, re
from dataclasses import dataclass
from typing import Mapping

_REF = re.compile(r"^[A-Za-z_][A-Za-z0-9_]{0,127}$")

@dataclass(frozen=True)
class CredentialRef:
    name: str
    provider: str = ""
    scope: str = "runtime"
    def __post_init__(self):
        if not _REF.match(self.name): raise ValueError("invalid credential reference")

class CredentialVault:
    """Resolve secrets only at execution time; references are safe for events and prompts."""
    def __init__(self, values: Mapping[str, str] | None = None): self._values = dict(values or {})
    def resolve(self, ref: CredentialRef) -> str:
        value = self._values.get(ref.name, os.environ.get(ref.name, ""))
        if not value: raise PermissionError(f"credential unavailable: {ref.name}")
        return value
    @staticmethod
    def sanitize(payload):
        if isinstance(payload, dict):
            return {key: "[CREDENTIAL_REF]" if key.lower() in {"secret", "token", "password", "api_key", "credential"} else CredentialVault.sanitize(value) for key, value in payload.items()}
        if isinstance(payload, list): return [CredentialVault.sanitize(item) for item in payload]
        return payload
