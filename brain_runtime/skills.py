from __future__ import annotations
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
import hashlib, json, re, threading

@dataclass(frozen=True)
class SkillManifest:
    id: str; version: str; description: str; triggers: tuple[str, ...]; exclusions: tuple[str, ...] = (); body_path: str = ""; resources: tuple[str, ...] = (); tools: tuple[str, ...] = (); required_permissions: tuple[str, ...] = (); trust_level: str = "community"; license: str = ""; content_hash: str = ""; source_commit: str = ""; source_url: str = ""; signature: str = ""; signature_key_id: str = ""

class SkillRegistry:
    def __init__(self, trusted_levels: Iterable[str] = ("builtin", "verified"), quarantine_path: str | Path | None = None):
        self._trusted = set(trusted_levels); self._skills: dict[str, SkillManifest] = {}; self._revoked: set[str] = set(); self._revoked_versions: set[tuple[str, str]] = set(); self._lock = threading.RLock(); self.quarantine_path = Path(quarantine_path) if quarantine_path else None
        if self.quarantine_path and self.quarantine_path.exists():
            for line in self.quarantine_path.read_text(encoding="utf-8").splitlines():
                try:
                    item = json.loads(line); item.get("version") and self._revoked_versions.add((item["skill_id"], item["version"])) or self._revoked.add(item["skill_id"])
                except (ValueError, KeyError): pass
    def register(self, manifest: SkillManifest) -> None:
        if manifest.trust_level not in self._trusted: raise PermissionError(f"skill '{manifest.id}' não é confiável")
        if not manifest.id or not manifest.version or not manifest.body_path: raise ValueError("manifesto exige id, version e body_path")
        if manifest.id in self._revoked or (manifest.id, manifest.version) in self._revoked_versions: raise PermissionError("skill is revoked")
        with self._lock: self._skills[manifest.id] = manifest
    def get(self, skill_id: str) -> SkillManifest: return self._skills[skill_id]
    def revoke(self, skill_id: str, reason: str = "", version: str | None = None) -> None:
        with self._lock:
            version and self._revoked_versions.add((skill_id, version)) or self._revoked.add(skill_id); self._skills.pop(skill_id, None)
            if self.quarantine_path:
                self.quarantine_path.parent.mkdir(parents=True, exist_ok=True); with_file = self.quarantine_path.open("a", encoding="utf-8"); with_file.write(json.dumps({"skill_id": skill_id, "version": version, "reason": reason}) + "\n"); with_file.close()
    def is_revoked(self, skill_id: str, version: str | None = None) -> bool: return skill_id in self._revoked or (version is not None and (skill_id, version) in self._revoked_versions)
    def quarantine(self, skill_id: str, reason: str) -> None:
        if self.quarantine_path:
            self.quarantine_path.parent.mkdir(parents=True, exist_ok=True); self.quarantine_path.open("a", encoding="utf-8").write(json.dumps({"skill_id": skill_id, "reason": reason}) + "\n")
    def match(self, text: str) -> list[SkillManifest]:
        lowered = text.lower(); return [skill for skill in self._skills.values() if skill.id not in self._revoked and any(trigger.lower() in lowered for trigger in skill.triggers) and not any(exclusion.lower() in lowered for exclusion in skill.exclusions)]
    def all(self) -> tuple[SkillManifest, ...]: return tuple(self._skills.values())
    def validate_body(self, skill_id: str, base_path: str | Path) -> bool:
        manifest = self.get(skill_id); root, body = Path(base_path).resolve(), (Path(base_path) / manifest.body_path).resolve()
        if root not in body.parents or not body.is_file() or body.is_symlink(): return False
        return not manifest.content_hash or hashlib.sha256(body.read_bytes()).hexdigest() == manifest.content_hash
    def authorize(self, skill_id: str, available_permissions: Iterable[str]) -> bool: return skill_id not in self._revoked and set(self.get(skill_id).required_permissions).issubset(set(available_permissions))

class SpecialistRouter:
    def __init__(self, registry: SkillRegistry): self.registry = registry
    def route(self, objective: str) -> SkillManifest:
        matches = self.registry.match(objective)
        if not matches: raise LookupError("nenhuma skill confiável corresponde ao objetivo")
        return sorted(matches, key=lambda skill: (skill.trust_level == "builtin", skill.version), reverse=True)[0]
