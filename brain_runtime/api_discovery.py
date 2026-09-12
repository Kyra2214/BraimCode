from __future__ import annotations
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Callable, Iterable
from .apis import ApiCatalogEntry, DynamicApiCatalog

@dataclass(frozen=True)
class ApiCandidate:
    provider: str
    model: str
    endpoint: str
    capabilities: tuple[str, ...]
    fallback_group: str
    source: str
    license: str = ""
    cost_per_call: float = 0.0
    credential_ref: str | None = None

class ApiDiscovery:
    def __init__(self, catalog: DynamicApiCatalog, probe: Callable[[ApiCandidate], bool] | None = None):
        self.catalog, self.probe = catalog, probe or (lambda candidate: True)

    def ingest(self, candidates: Iterable[ApiCandidate]) -> tuple[ApiCatalogEntry, ...]:
        registered = []
        for candidate in candidates:
            if not candidate.provider or not candidate.model or not candidate.source: continue
            if not candidate.license or candidate.cost_per_call < 0: continue
            if not self.probe(candidate): continue
            entry = ApiCatalogEntry(candidate.provider, candidate.model, candidate.capabilities, candidate.fallback_group,
                cost_per_call=candidate.cost_per_call, credential_ref=candidate.credential_ref,
                provenance=(candidate.source, datetime.now(timezone.utc).isoformat()))
            self.catalog.register(entry); registered.append(entry)
        return tuple(registered)
