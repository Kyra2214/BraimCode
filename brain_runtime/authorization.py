from __future__ import annotations

from dataclasses import dataclass
from secrets import token_urlsafe
from typing import Any

from .modes import RuntimeMode


@dataclass(frozen=True)
class ExecutionAuthorization:
    """Capability opaca vinculada a uma instância específica de BrainPipeline."""

    _pipeline_identity: int
    mode: RuntimeMode
    nonce: str

    @classmethod
    def _issue(cls, pipeline: Any, mode: RuntimeMode) -> "ExecutionAuthorization":
        return cls(id(pipeline), mode, token_urlsafe(32))

    def _valid_for(self, pipeline: Any, mode: RuntimeMode) -> bool:
        return self._pipeline_identity == id(pipeline) and self.mode is mode and bool(self.nonce)


class ExecutionAuthorizationError(PermissionError):
    """Tentativa de executar o pipeline sem capability válida."""
