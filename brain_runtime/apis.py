from __future__ import annotations
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import Callable
import threading

@dataclass
class ApiCatalogEntry:
    provider: str; model: str; capabilities: tuple[str, ...]; fallback_group: str
    quota_remaining: int | None = None; latency_ms: float | None = None; reliability: float = .5; quality: float = .5
    healthy: bool = True; errors: int = 0; cooldown_until: str | None = None; cost_per_call: float = 0.0; credential_ref: str | None = None
    provenance: tuple[str, ...] = (); license: str = ""; reserved_quota: int = 0; last_probe_at: str | None = None
    def score(self, *, cost_weight: float = .15, quality_weight: float = .35, risk_weight: float = .1) -> float:
        quota = 0.0 if self.quota_remaining == 0 else 1.0
        latency = 0.0 if self.latency_ms is None else max(0.0, 1 - self.latency_ms / 10000)
        cost = 1.0 / (1.0 + max(0.0, self.cost_per_call))
        risk = 1.0 if self.credential_ref else .8
        return self.quality * quality_weight + self.reliability * .25 + quota * .1 + latency * .1 + cost * cost_weight + risk * risk_weight
    def available(self) -> bool:
        cooldown_over = not self.cooldown_until or datetime.now(timezone.utc) >= datetime.fromisoformat(self.cooldown_until)
        return self.healthy and self.quota_remaining != 0 and cooldown_over and self.reserved_quota >= 0
    def equivalent_to(self, capability: str) -> bool: return capability in self.capabilities

class DynamicApiCatalog:
    def __init__(self, entries: list[ApiCatalogEntry] | None = None): self._entries = list(entries or []); self._lock = threading.RLock()
    def register(self, entry: ApiCatalogEntry) -> None:
        if not entry.capabilities or not entry.provider or not entry.model: raise ValueError("invalid API catalog entry")
        with self._lock: self._entries.append(entry)
    def candidates(self, capability: str, fallback_group: str | None = None, *, risk_limit: float = 1.0) -> list[ApiCatalogEntry]:
        with self._lock:
            return sorted((e for e in self._entries if e.equivalent_to(capability) and e.available() and (fallback_group is None or e.fallback_group == fallback_group)), key=lambda e: e.score(), reverse=True)
    def select(self, capability: str, fallback_group: str | None = None) -> ApiCatalogEntry:
        candidates = self.candidates(capability, fallback_group)
        if not candidates: raise LookupError(f"nenhuma API saudável para {capability}")
        return candidates[0]
    def reserve(self, provider: str, model: str, amount: int = 1) -> ApiCatalogEntry:
        if amount <= 0: raise ValueError("reservation amount must be positive")
        with self._lock:
            entry = next((e for e in self._entries if e.provider == provider and e.model == model), None)
            if entry is None: raise KeyError(f"API não registrada: {provider}/{model}")
            available = None if entry.quota_remaining is None else entry.quota_remaining - entry.reserved_quota
            if available is not None and available < amount: raise LookupError("quota exhausted")
            entry.reserved_quota += amount; return entry
    def reconcile(self, provider: str, model: str, *, used: int = 1, quota_remaining: int | None = None) -> None:
        with self._lock:
            entry = next((e for e in self._entries if e.provider == provider and e.model == model), None)
            if entry is None: raise KeyError(f"API não registrada: {provider}/{model}")
            entry.reserved_quota = max(0, entry.reserved_quota - max(0, used))
            if quota_remaining is not None: entry.quota_remaining = max(0, quota_remaining)
    def record(self, provider: str, model: str, success: bool, latency_ms: float, quota_remaining: int | None = None) -> None:
        with self._lock:
            entry = next((e for e in self._entries if e.provider == provider and e.model == model), None)
            if entry is None: raise KeyError(f"API não registrada: {provider}/{model}")
            entry.latency_ms = latency_ms
            if quota_remaining is not None: entry.quota_remaining = max(0, quota_remaining)
            entry.reliability = min(1.0, entry.reliability * .8 + (.2 if success else 0))
            if success: entry.errors = 0; entry.healthy = True; entry.cooldown_until = None
            else:
                entry.errors += 1; entry.cooldown_until = (datetime.now(timezone.utc) + timedelta(seconds=min(300, 2 ** entry.errors))).isoformat()
                if entry.errors >= 3: entry.healthy = False
    def probe(self, provider: str, model: str, check: Callable[[ApiCatalogEntry], bool]) -> bool:
        entry = next((e for e in self._entries if e.provider == provider and e.model == model), None)
        if entry is None: raise KeyError(f"API não registrada: {provider}/{model}")
        try: ok = bool(check(entry))
        except Exception: ok = False
        entry.last_probe_at = datetime.now(timezone.utc).isoformat(); self.record(provider, model, ok, entry.latency_ms or 0); return ok
    def waterfall(self, capability: str, call: Callable[[ApiCatalogEntry], object]) -> tuple[ApiCatalogEntry, object]:
        candidates = self.candidates(capability)
        if not candidates: raise LookupError(f"nenhuma API saudável para {capability}")
        last_error = None
        for entry in candidates:
            try:
                self.reserve(entry.provider, entry.model); result = call(entry); self.reconcile(entry.provider, entry.model); self.record(entry.provider, entry.model, True, entry.latency_ms or 0); return entry, result
            except Exception as error:
                last_error = error; self.reconcile(entry.provider, entry.model); self.record(entry.provider, entry.model, False, entry.latency_ms or 0)
        raise RuntimeError("todas as APIs equivalentes do grupo de fallback falharam") from last_error
