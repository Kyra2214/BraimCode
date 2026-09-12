from __future__ import annotations
from pathlib import Path
from typing import Any, Mapping
from .contracts import ContractError

REQUIRED_SECTIONS = ("policy", "execution", "providers", "routing")

def validate_configuration(config: Mapping[str, Any]) -> dict[str, Any]:
    missing = [section for section in REQUIRED_SECTIONS if section not in config]
    if missing: raise ContractError(f"missing configuration sections: {', '.join(missing)}")
    policy = config["policy"]
    if not isinstance(policy, Mapping) or policy.get("deny_by_default") is not True: raise ContractError("policy must be deny-by-default")
    if "secrets" in config: raise ContractError("raw secrets cannot be part of configuration")
    return dict(config)

def assert_isolated_path(path: str | Path, allowed_root: str | Path) -> Path:
    root, candidate = Path(allowed_root).resolve(), Path(path).resolve()
    if candidate != root and root not in candidate.parents: raise PermissionError("path outside configured root")
    parts = [candidate] + list(candidate.parents) if candidate.exists() else []
    if any(part.is_symlink() for part in parts): raise PermissionError("symlink path is not allowed")
    return candidate
