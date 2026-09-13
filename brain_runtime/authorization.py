from __future__ import annotations

from secrets import token_urlsafe
from typing import Any

from .modes import RuntimeMode


class ExecutionAuthorization:
    """Capability opaca vinculada a uma instância específica de BrainPipeline.

    A autorização não pode ser criada diretamente por consumidores. A única
    emissão normal é feita pelo módulo de runtime através de ``_issue``.
    """

    __slots__ = ("__pipeline_identity", "__mode", "__nonce")

    def __init__(self, *_: Any, **__: Any) -> None:
        raise TypeError("ExecutionAuthorization só pode ser emitida pelo runtime")

    @classmethod
    def _issue(cls, pipeline: Any, mode: RuntimeMode) -> "ExecutionAuthorization":
        obj = object.__new__(cls)
        obj.__pipeline_identity = id(pipeline)
        obj.__mode = mode
        obj.__nonce = token_urlsafe(32)
        return obj

    def _valid_for(self, pipeline: Any, mode: RuntimeMode) -> bool:
        return (
            self.__pipeline_identity == id(pipeline)
            and self.__mode is mode
            and bool(self.__nonce)
        )


class ExecutionAuthorizationError(PermissionError):
    """Tentativa de executar o pipeline sem capability válida."""
