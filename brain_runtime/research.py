from __future__ import annotations
from dataclasses import dataclass
from datetime import datetime, timezone
from hashlib import sha256
from typing import Callable, Iterable
from urllib.parse import urlparse
import re

_INJECTION = re.compile(r"(?i)(ignore\s+(?:all\s+)?previous|system\s+message|developer\s+instructions|reveal\s+(?:the\s+)?prompt|jailbreak)")

@dataclass(frozen=True)
class ResearchSource:
    source_id: str; url: str; title: str; publisher: str = ""; trust_score: float = .5; retrieved_at: str = ""

@dataclass(frozen=True)
class Evidence:
    evidence_id: str; source_id: str; claim: str; excerpt: str; confidence: float; content_hash: str
    rank: float = 0.0; independent_group: str = ""

@dataclass(frozen=True)
class ResearchResult:
    query: str; sources: tuple[ResearchSource, ...]; evidence: tuple[Evidence, ...]; limitations: tuple[str, ...] = ()
    @property
    def provenance(self) -> tuple[str, ...]: return tuple(source.url for source in self.sources)

class ResearchLayer:
    def __init__(self, loader: Callable[[ResearchSource], str] | None = None, allowed_schemes: tuple[str, ...] = ("https",)):
        self.loader = loader or (lambda source: ""); self.allowed_schemes = allowed_schemes

    def collect(self, query: str, sources: Iterable[ResearchSource]) -> ResearchResult:
        if not query.strip(): raise ValueError("research query cannot be empty")
        evidence, accepted, limitations = [], [], []
        for source in sources:
            parsed = urlparse(source.url)
            if parsed.scheme not in self.allowed_schemes or not parsed.netloc:
                limitations.append(f"source rejected: {source.source_id}"); continue
            try: content = self.loader(source)
            except Exception as exc:
                limitations.append(f"source unavailable: {source.source_id}: {type(exc).__name__}"); continue
            if not content.strip(): limitations.append(f"source empty: {source.source_id}"); continue
            if _INJECTION.search(content): limitations.append(f"source quarantined: {source.source_id}: prompt injection"); continue
            accepted.append(source); excerpt = content.strip()[:2000]; digest = sha256(content.encode()).hexdigest()
            trust = max(0.0, min(1.0, source.trust_score)); freshness = 1.0 if not source.retrieved_at else .8
            evidence.append(Evidence(f"evidence_{digest[:16]}", source.source_id, query, excerpt, trust, digest, trust * .7 + freshness * .3, source.publisher or parsed.netloc))
        evidence.sort(key=lambda item: item.rank, reverse=True)
        return ResearchResult(query, tuple(accepted), tuple(evidence), tuple(limitations))

    @staticmethod
    def validate(result: ResearchResult, minimum_sources: int = 1, minimum_independent_groups: int = 1) -> tuple[bool, tuple[str, ...]]:
        diagnostics = []
        if len(result.sources) < minimum_sources: diagnostics.append("insufficient independent sources")
        groups = {evidence.independent_group for evidence in result.evidence if evidence.independent_group}
        if len(groups) < minimum_independent_groups: diagnostics.append("insufficient independent evidence groups")
        if not result.evidence: diagnostics.append("no evidence collected")
        if any(not evidence.content_hash for evidence in result.evidence): diagnostics.append("evidence missing content hash")
        return not diagnostics, tuple(diagnostics)
