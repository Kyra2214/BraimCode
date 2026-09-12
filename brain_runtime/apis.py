from __future__ import annotations
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Callable

@dataclass
class ApiCatalogEntry:
    provider: str; model: str; capabilities: tuple[str, ...]; fallback_group: str
    quota_remaining: int | None = None; latency_ms: float | None = None; reliability: float = .5; quality: float = .5
    healthy: bool = True; errors: int = 0; cooldown_until: str | None = None; cost_per_call: float = 0.0; credential_ref: str | None = None
    provenance: tuple[str, ...] = ()
    def score(self) -> float:
        quota = 0.0 if self.quota_remaining == 0 else 1.0
        latency = 0.0 if self.latency_ms is None else max(0.0, 1 - self.latency_ms / 10000)
        return self.quality * .45 + self.reliability * .35 + quota * .1 + latency * .1
    def available(self) -> bool:
        cooldown_over = not self.cooldown_until or datetime.now(timezone.utc) >= datetime.fromisoformat(self.cooldown_until)
        return self.healthy and self.quota_remaining != 0 and cooldown_over

class DynamicApiCatalog:
    def __init__(self, entries: list[ApiCatalogEntry] | None = None): self._entries = list(entries or [])
    def register(self, entry: ApiCatalogEntry) -> None: self._entries.append(entry)
    def candidates(self, capability: str, fallback_group: str | None = None) -> list[ApiCatalogEntry]:
        return sorted((e for e in self._entries if capability in e.capabilities and e.available() and (fallback_group is None or e.fallback_group == fallback_group)), key=lambda e: e.score(), reverse=True)
    def record(self, provider: str, model: str, success: bool, latency_ms: float, quota_remaining: int | None = None) -> None:
        for entry in self._entries:
            if entry.provider == provider and entry.model == model:
                entry.latency_ms = latency_ms
                if quota_remaining is not None: entry.quota_remaining = max(0, quota_remaining)
                entry.reliability = min(1.0, entry.reliability * .8 + (.2 if success else 0))
                if success: entry.errors = 0; entry.healthy = True; entry.cooldown_until = None
                else:
                    entry.errors += 1
                    entry.cooldown_until = (datetime.now(timezone.utc) + timedelta(seconds=min(300, 2 ** entry.errors))).isoformat()
                    if entry.errors >= 3: entry.healthy = False
                return
        raise KeyError(f"API não registrada: {provider}/{model}")
    def probe(self, provider: str, model: str, check: Callable[[ApiCatalogEntry], bool]) -> bool:
        entry = next((e for e in self._entries if e.provider == provider and e.model == model), None)
        if entry is None: raise KeyError(f"API não registrada: {provider}/{model}")
        try: ok = bool(check(entry))
        except Exception: ok = False
        self.record(provider, model, ok, entry.latency_ms or 0)
        return ok
    def waterfall(self, capability: str, call: Callable[[ApiCatalogEntry], object]) -> tuple[ApiCatalogEntry, object]:
        candidates = self.candidates(capability)
        if not candidates: raise LookupError(f"nenhuma API saudável para {capability}")
        last_error = None
        for entry in candidates:
            try:
                result = call(entry); self.record(entry.provider, entry.model, True, entry.latency_ms or 0); return entry, result
            except Exception as error:
                last_error = error; self.record(entry.provider, entry.model, False, entry.latency_ms or 0)
        raise RuntimeError("todas as APIs do grupo de fallback falharam") from last_error
