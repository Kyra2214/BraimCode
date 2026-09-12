from __future__ import annotations
from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path
from typing import Callable
import re
from .skills import SkillManifest, SkillRegistry
from .signatures import SignatureVerifier

@dataclass(frozen=True)
class ExternalSkill:
    source: str; external_id: str; version: str; description: str; body: str; license: str; permissions: tuple[str, ...] = (); verified: bool = False
    source_url: str = ""; commit: str = ""; signature: str = ""; key_id: str = ""

class ExternalSkillAdapter:
    source_name = "external"; allowed_licenses = frozenset({"MIT", "Apache-2.0", "BSD-3-Clause"})
    def __init__(self, scanner: Callable[[str], bool] | None = None, allow_licenses: set[str] | None = None, signature_verifier: SignatureVerifier | None = None): self.scanner = scanner or self.scan_body; self.allow_licenses = frozenset(allow_licenses or self.allowed_licenses); self.signature_verifier = signature_verifier
    @staticmethod
    def scan_body(body: str) -> bool:
        return not re.search(r"(?i)(os\.system|subprocess|eval\s*\(|exec\s*\(|ignore\s+previous|api[_-]?key\s*=|curl\s+|wget\s+)", body)
    def import_skill(self, skill: ExternalSkill, registry: SkillRegistry, destination: str | Path) -> SkillManifest:
        if not skill.external_id or not skill.version or not skill.body.strip(): raise ValueError("external skill metadata is incomplete")
        if skill.license not in self.allow_licenses: registry.quarantine(skill.external_id, "license not allowed"); raise PermissionError("skill license is not allowlisted")
        if not self.scanner(skill.body): registry.quarantine(skill.external_id, "content scan failed"); raise PermissionError("skill content failed security scan")
        signature_present = bool(skill.commit or skill.signature)
        if not skill.verified or (signature_present and (not skill.commit or not skill.signature or skill.signature != sha256((skill.commit + skill.body).encode()).hexdigest())):
            registry.quarantine(skill.external_id, "signature or immutable commit missing"); raise PermissionError("skill signature verification failed")
        if self.signature_verifier:
            try: self.signature_verifier.require(skill.key_id, skill.body.encode(), bytes.fromhex(skill.signature))
            except (ValueError, PermissionError): registry.quarantine(skill.external_id, "authority signature failed"); raise PermissionError("authority signature verification failed")
        root = Path(destination).resolve(); root.mkdir(parents=True, exist_ok=True); safe_id = "".join(char if char.isalnum() or char in "-_" else "_" for char in skill.external_id)
        body_path = root / f"{safe_id}.md"; body_path.write_text(skill.body, encoding="utf-8")
        manifest = SkillManifest(safe_id, skill.version, skill.description, triggers=(), exclusions=(), body_path=body_path.name, required_permissions=skill.permissions, trust_level="verified", license=skill.license, content_hash=sha256(skill.body.encode()).hexdigest(), source_commit=skill.commit, source_url=skill.source_url, signature=skill.signature)
        registry.register(manifest); return manifest

class GitSkillsAdapter(ExternalSkillAdapter): source_name = "gitskills"
class AnthropicSkillsAdapter(ExternalSkillAdapter): source_name = "anthropic"
