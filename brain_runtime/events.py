from __future__ import annotations

import json
import threading
from pathlib import Path
from typing import Any, Iterable

from .models import Event, EventType, now_iso, new_id


_SECRET_KEYS = {"secret", "token", "password", "api_key", "credential"}


def redact(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: "[REDACTED]" if key.lower() in _SECRET_KEYS else redact(item) for key, item in value.items()}
    if isinstance(value, list):
        return [redact(item) for item in value]
    return value


class EventStore:
    """Event log append-only; JSONL é durável e naturalmente replayável."""

    def __init__(self, path: str | Path | None = None):
        self.path = Path(path) if path else None
        self._lock = threading.RLock()
        self._events: list[Event] = []
        if self.path and self.path.exists():
            for line in self.path.read_text(encoding="utf-8").splitlines():
                if line.strip():
                    self._events.append(Event(**json.loads(line)))

    def append(self, run_id: str, session_id: str, task_id: str, event_type: EventType | str, payload: dict[str, Any]) -> Event:
        with self._lock:
            sequence = len(self._events)
            event = Event(new_id("event"), run_id, session_id, task_id, now_iso(), str(event_type.value if isinstance(event_type, EventType) else event_type), 1, sequence, redact(payload), True)
            self._events.append(event)
            if self.path:
                self.path.parent.mkdir(parents=True, exist_ok=True)
                with self.path.open("a", encoding="utf-8") as stream:
                    stream.write(json.dumps(event.__dict__, ensure_ascii=False, sort_keys=True) + "\n")
            return event

    def all(self) -> tuple[Event, ...]:
        with self._lock:
            return tuple(self._events)

    def replay(self, run_id: str | None = None, task_id: str | None = None) -> tuple[Event, ...]:
        return tuple(event for event in self.all() if (run_id is None or event.run_id == run_id) and (task_id is None or event.task_id == task_id))

    def count(self) -> int:
        return len(self._events)
