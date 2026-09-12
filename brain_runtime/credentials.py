from __future__ import annotations
import os, re
from dataclasses import dataclass, replace
from typing import Mapping, Callable, Any

_REF = re.compile(r"^[A-Za-z_][A-Za-z0-9_]{0,127}$")
@dataclass(frozen=True)
class CredentialRef:
    name: str; provider: str = ""; scope: str = "runtime"
    def __post_init__(self):
        if not _REF.match(self.name): raise ValueError("invalid credential reference")

class CredentialVault:
    """Resolve secrets only at execution time; refs and sanitized views remain safe for events."""
    def __init__(self, values: Mapping[str, str] | None = None): self._values = dict(values or {})
    def resolve(self, ref: CredentialRef) -> str:
        value = self._values.get(ref.name, os.environ.get(ref.name, ""))
        if not value: raise PermissionError(f"credential unavailable: {ref.name}")
        return value
    def resolve_reference(self, reference: str | CredentialRef) -> str:
        return self.resolve(reference if isinstance(reference, CredentialRef) else CredentialRef(reference))
    @staticmethod
    def sanitize(payload: Any):
        if isinstance(payload, dict):
            return {key: "[CREDENTIAL_REF]" if str(key).lower() in {"secret", "token", "password", "api_key", "credential", "authorization"} else CredentialVault.sanitize(value) for key, value in payload.items()}
        if isinstance(payload, list): return [CredentialVault.sanitize(item) for item in payload]
        if isinstance(payload, tuple): return tuple(CredentialVault.sanitize(item) for item in payload)
        return payload
    def dispatch(self, dispatcher: Callable, request: Any, credential_ref: CredentialRef | None = None) -> Any:
        """Injects a credential only into the call, never mutates the request or its event representation."""
        if credential_ref is None: return dispatcher(request)
        secret = self.resolve(credential_ref)
        try: return dispatcher(request, credential=secret)
        except TypeError: return dispatcher(request)
