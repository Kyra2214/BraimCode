from __future__ import annotations

from dataclasses import dataclass
from typing import Protocol

from .events import EventStore
from .models import EventType, ExecutionRequest, ExecutionResult, PolicyContext, TaskSpec, new_id
from .policy import PolicyBroker


class Secretary(Protocol):
    def normalize(self, objective: str, session_id: str) -> TaskSpec: ...


class Router(Protocol):
    def select(self, capability: str) -> str: ...


class PromptBuilder(Protocol):
    def build(self, task: TaskSpec, capability: str) -> str: ...


class Dispatcher(Protocol):
    def dispatch(self, request: ExecutionRequest) -> ExecutionResult: ...


@dataclass
class KeywordSecretary:
    capability_keywords: dict[str, tuple[str, ...]]

    def normalize(self, objective: str, session_id: str) -> TaskSpec:
        text = objective.strip()
        lowered = text.lower()
        capabilities = tuple(capability for capability, words in self.capability_keywords.items() if any(word in lowered for word in words))
        return TaskSpec(new_id("task"), session_id, text, capabilities=capabilities or ("general_analysis",), success_criteria=("resultado estruturado",))


@dataclass
class StaticRouter:
    providers: dict[str, str]
    def select(self, capability: str) -> str:
        return self.providers.get(capability, self.providers.get("default", "local"))


class DefaultPromptBuilder:
    def build(self, task: TaskSpec, capability: str) -> str:
        return f"Capability: {capability}\nObjective: {task.objective}\nSuccess: {', '.join(task.success_criteria)}"


class BrainPipeline:
    def __init__(self, secretary: Secretary, router: Router, prompt_builder: PromptBuilder, policy: PolicyBroker, events: EventStore, dispatcher: Dispatcher):
        self.secretary, self.router, self.prompt_builder = secretary, router, prompt_builder
        self.policy, self.events, self.dispatcher = policy, events, dispatcher

    def run(self, objective: str, session_id: str, actor: str = "brain") -> list[ExecutionResult]:
        task = self.secretary.normalize(objective, session_id)
        run_id = new_id("run")
        self.events.append(run_id, session_id, task.task_id, EventType.TASK_CREATED, {"objective": task.objective})
        self.events.append(run_id, session_id, task.task_id, EventType.TASK_CLASSIFIED, {"capabilities": task.capabilities})
        results = []
        for capability in task.capabilities:
            provider = self.router.select(capability)
            self.events.append(run_id, session_id, task.task_id, EventType.CAPABILITY_SELECTED, {"capability": capability, "provider": provider})
            context = PolicyContext(run_id, task.task_id, actor, risk_class="LOW")
            decision = self.policy.authorize(actor, capability, provider, context)
            self.events.append(run_id, session_id, task.task_id, EventType.POLICY_CHECKED, {"decision": decision.decision.value, "reason": decision.reason})
            if decision.decision.value != "ALLOW":
                self.events.append(run_id, session_id, task.task_id, EventType.APPROVAL_REQUESTED, {"capability": capability})
                results.append(ExecutionResult(new_id("request"), False, error=decision.reason))
                continue
            prompt = self.prompt_builder.build(task, capability)
            request = ExecutionRequest(new_id("request"), run_id, task.task_id, new_id("step"), capability, prompt, {"provider": provider})
            self.events.append(run_id, session_id, task.task_id, EventType.AGENT_DISPATCHED, {"request_id": request.request_id, "capability": capability})
            result = self.dispatcher.dispatch(request)
            results.append(result)
            self.events.append(run_id, session_id, task.task_id, EventType.AGENT_COMPLETED, {"success": result.success})
        return results
