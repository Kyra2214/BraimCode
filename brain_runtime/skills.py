from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


@dataclass(frozen=True)
class SkillManifest:
    id: str
    version: str
    description: str
    triggers: tuple[str, ...]
    exclusions: tuple[str, ...] = ()
    body_path: str = ""
    resources: tuple[str, ...] = ()
    tools: tuple[str, ...] = ()
    required_permissions: tuple[str, ...] = ()
    trust_level: str = "community"


class SkillRegistry:
    def __init__(self, trusted_levels: Iterable[str] = ("builtin", "verified")):
        self._trusted = set(trusted_levels)
        self._skills: dict[str, SkillManifest] = {}

    def register(self, manifest: SkillManifest) -> None:
        if manifest.trust_level not in self._trusted:
            raise PermissionError(f"skill '{manifest.id}' não é confiável")
        if not manifest.id or not manifest.version or not manifest.body_path:
            raise ValueError("manifesto exige id, version e body_path")
        self._skills[manifest.id] = manifest

    def get(self, skill_id: str) -> SkillManifest:
        return self._skills[skill_id]

    def match(self, text: str) -> list[SkillManifest]:
        lowered = text.lower()
        return [skill for skill in self._skills.values() if any(trigger.lower() in lowered for trigger in skill.triggers) and not any(exclusion.lower() in lowered for exclusion in skill.exclusions)]

    def all(self) -> tuple[SkillManifest, ...]:
        return tuple(self._skills.values())


class SpecialistRouter:
    def __init__(self, registry: SkillRegistry):
        self.registry = registry

    def route(self, objective: str) -> SkillManifest:
        matches = self.registry.match(objective)
        if not matches:
            raise LookupError("nenhuma skill confiável corresponde ao objetivo")
        return sorted(matches, key=lambda skill: (skill.trust_level == "builtin", skill.version), reverse=True)[0]
