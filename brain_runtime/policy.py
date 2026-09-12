from __future__ import annotations

from dataclasses import replace
from datetime import datetime, timedelta, timezone
from typing import Iterable

from .models import ApprovalRequired, Decision, PolicyContext, PolicyDecision, new_id


class PolicyBroker:
    """Autoridade única de autorização; catálogos não concedem permissão."""

    def __init__(self, allowed_capabilities: Iterable[str] = (), actor_capabilities: dict[str, Iterable[str]] | None = None):
        self._allowed = set(allowed_capabilities)
        self._actors = {k: set(v) for k, v in (actor_capabilities or {}).items()}

    def authorize(self, actor: str, capability: str, resource: str, context: PolicyContext) -> PolicyDecision:
        reason = "denied by default"
        decision = Decision.DENY
        approval = context.approval
        if capability not in self._allowed:
            reason = f"capability '{capability}' is not registered"
        elif actor not in self._actors or capability not in self._actors[actor]:
            reason = f"actor '{actor}' is not authorized for '{capability}'"
        elif context.sandbox_required is False and context.risk_class.upper() not in {"LOW", "READ_ONLY"}:
            reason = "sandbox is mandatory for non-low-risk capability"
        elif context.network_allowed and "network" not in context.filesystem_roots and capability.startswith("filesystem"):
            reason = "filesystem capability cannot infer network authority"
        elif approval is not ApprovalRequired.NONE:
            decision = Decision.ASK
            reason = f"explicit {approval.value} approval is required"
        else:
            decision = Decision.ALLOW
            reason = f"registered capability authorized for resource '{resource}'"
        expires = datetime.now(timezone.utc) + timedelta(seconds=max(1, context.ttl_seconds))
        return PolicyDecision(
            decision_id=new_id("decision"), run_id=context.run_id, task_id=context.task_id,
            actor=actor, capability=capability, risk_class=context.risk_class,
            decision=decision, approval_required=approval,
            sandbox_required=context.sandbox_required, network_allowed=context.network_allowed,
            filesystem_roots=context.filesystem_roots, budget=dict(context.budget),
            expires_at=expires.isoformat(), reason=reason,
        )

    def with_actor_capability(self, actor: str, capability: str) -> "PolicyBroker":
        actors = {key: set(value) for key, value in self._actors.items()}
        actors.setdefault(actor, set()).add(capability)
        return PolicyBroker(self._allowed | {capability}, actors)
