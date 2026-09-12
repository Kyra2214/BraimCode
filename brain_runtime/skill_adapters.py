from __future__ import annotations
from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path
from typing import Any
from .skills import SkillManifest, SkillRegistry

@dataclass(frozen=True)
class ExternalSkill:
    source: str
    external_id: str
    version: str
    description: str
    body: str
    license: str
    permissions: tuple[str, ...] = ()
    verified: bool = False

class ExternalSkillAdapter:
    source_name = "external"
    def import_skill(self, skill: ExternalSkill, registry: SkillRegistry, destination: str | Path) -> SkillManifest:
        if not skill.external_id or not skill.version or not skill.body.strip(): raise ValueError("external skill metadata is incomplete")
        if not skill.license: raise ValueError("external skill license is required")
        root = Path(destination).resolve(); root.mkdir(parents=True, exist_ok=True)
        safe_id = "".join(char if char.isalnum() or char in "-_" else "_" for char in skill.external_id)
        body_path = root / f"{safe_id}.md"; body_path.write_text(skill.body, encoding="utf-8")
        manifest = SkillManifest(safe_id, skill.version, skill.description, triggers=(), exclusions=(), body_path=body_path.name, required_permissions=skill.permissions,
            trust_level="verified" if skill.verified else "unverified", license=skill.license, content_hash=sha256(skill.body.encode()).hexdigest())
        if not skill.verified:
            body_path.unlink(missing_ok=True)
            raise PermissionError("unverified skill is quarantined and cannot be registered")
        registry.register(manifest)
        return manifest

class GitSkillsAdapter(ExternalSkillAdapter):
    source_name = "gitskills"

class AnthropicSkillsAdapter(ExternalSkillAdapter):
    source_name = "anthropic"
