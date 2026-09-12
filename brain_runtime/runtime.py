from __future__ import annotations
from dataclasses import dataclass
from threading import Event
from time import monotonic
from typing import Callable, Iterable
from .delivery import DeliveryPipeline
from .events import EventStore
from .models import EventType, ExecutionResult, new_id
from .observability import Observability, TraceContext
from .pipeline import BrainPipeline
from .recovery import StateReconstructor, RunState

class FaultInjected(RuntimeError): pass

@dataclass(frozen=True)
class RuntimeRunResult:
    run_id: str; results: tuple[ExecutionResult, ...]; delivered: tuple[ExecutionResult, ...]; state: RunState

class RuntimeCoordinator:
    """Orquestra pipeline -> QA/delivery -> replay state com falhas observáveis e recuperação segura."""
    def __init__(self, pipeline: BrainPipeline, delivery: DeliveryPipeline, events: EventStore, observability: Observability | None = None, fault_injector: Callable[[str], None] | None = None):
        self.pipeline, self.delivery, self.events, self.observability, self.fault_injector = pipeline, delivery, events, observability, fault_injector
    def _fault(self, stage: str) -> None:
        if self.fault_injector: self.fault_injector(stage)
    def run(self, objective: str, session_id: str, actor: str = "brain", cancel_event: Event | None = None, timeout_seconds: float | None = None) -> RuntimeRunResult:
        started = monotonic(); run_id = ""; delivered: list[ExecutionResult] = []; results: list[ExecutionResult] = []; interrupted = False
        try:
            self._fault("before_pipeline"); results = self.pipeline.run(objective, session_id, actor); events = self.events.all(); run_id = events[-1].run_id if events else new_id("run"); task_id = events[-1].task_id if events else ""
            self._fault("after_pipeline")
            for result in results:
                if cancel_event is not None and cancel_event.is_set():
                    self.events.append(run_id, session_id, task_id, EventType.JOB_CANCELLED, {"stage": "delivery", "request_id": result.request_id}); interrupted = True; break
                if timeout_seconds is not None and monotonic() - started > timeout_seconds:
                    self.events.append(run_id, session_id, task_id, EventType.JOB_FAILED, {"stage": "delivery", "request_id": result.request_id, "error": "runtime timeout"}); interrupted = True; break
                self._fault("before_delivery"); context = TraceContext(run_id, run_id, session_id, task_id); delivered_result = self.delivery.deliver(result, context=context); delivered.append(delivered_result); self._fault("after_delivery")
            if not interrupted: self.events.append(run_id, session_id, task_id, EventType.JOB_COMPLETED, {"success": all(item.success for item in delivered) if delivered else all(item.success for item in results)})
        except FaultInjected as error:
            if run_id:
                self.events.append(run_id, session_id, task_id, EventType.JOB_FAILED, {"error": str(error), "stage": "fault_injection"})
            raise
        except Exception as error:
            if run_id:
                self.events.append(run_id, session_id, task_id, EventType.JOB_FAILED, {"error": str(error), "stage": "runtime"})
            raise
        state = StateReconstructor(self.events).reconstruct(run_id)
        return RuntimeRunResult(run_id, tuple(results), tuple(delivered), state)
    def recover(self, run_id: str, task_id: str | None = None) -> RunState:
        return StateReconstructor(self.events).reconstruct(run_id, task_id)
    def replay(self, run_id: str, task_id: str | None = None) -> tuple:
        return self.events.replay(run_id, task_id)
