from __future__ import annotations

from dataclasses import dataclass, field
from typing import Callable


@dataclass
class ApiCatalogEntry:
    provider: str
    model: str
    capabilities: tuple[str, ...]
    fallback_group: str
    quota_remaining: int | None = None
    latency_ms: float | None = None
    reliability: float = 0.5
    quality: float = 0.5
    healthy: bool = True
    errors: int = 0

    def score(self) -> float:
        quota = 0.0 if self.quota_remaining == 0 else 1.0
        return self.quality * .45 + self.reliability * .35 + quota * .1 + (0 if self.latency_ms is None else max(0, 1 - self.latency_ms / 10000)) * .1


class DynamicApiCatalog:
    def __init__(self, entries: list[ApiCatalogEntry] = []):
        self._entries = list(entries)

    def register(self, entry: ApiCatalogEntry) -> None:
        self._entries.append(entry)

    def candidates(self, capability: str) -> list[ApiCatalogEntry]:
        return sorted((entry for entry in self._entries if capability in entry.capabilities and entry.healthy and entry.quota_remaining != 0), key=lambda entry: entry.score(), reverse=True)

    def record(self, provider: str, model: str, success: bool, latency_ms: float, quota_remaining: int | None = None) -> None:
        for entry in self._entries:
            if entry.provider == provider and entry.model == model:
                entry.latency_ms = latency_ms
                entry.quota_remaining = quota_remaining
                entry.reliability = min(1.0, entry.reliability * .8 + (1.0 if success else 0.0) * .2)
                if not success:
                    entry.errors += 1
                    if entry.errors >= 3:
                        entry.healthy = False
                return
        raise KeyError(f"API não registrada: {provider}/{model}")

    def waterfall(self, capability: str, call: Callable[[ApiCatalogEntry], object]) -> tuple[ApiCatalogEntry, object]:
        candidates = self.candidates(capability)
        if not candidates:
            raise LookupError(f"nenhuma API saudável para {capability}")
        last_error = None
        for entry in candidates:
            try:
                result = call(entry)
                self.record(entry.provider, entry.model, True, entry.latency_ms or 0)
                return entry, result
            except Exception as error:
                last_error = error
                self.record(entry.provider, entry.model, False, entry.latency_ms or 0)
        raise RuntimeError("todas as APIs do grupo de fallback falharam") from last_error
