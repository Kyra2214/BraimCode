from __future__ import annotations
import hashlib, json, os, re, threading, fcntl
from pathlib import Path
from typing import Any
from .models import Event, EventType, now_iso, new_id

_SECRET_KEYS = {"secret", "token", "password", "api_key", "apikey", "credential", "authorization"}
_SECRET_PATTERN = re.compile(r"(?i)(bearer\s+|sk-|ghp_|api[_-]?key\s*[=:])[^\s,;]+")

def redact(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: "[REDACTED]" if str(key).lower() in _SECRET_KEYS else redact(item) for key, item in value.items()}
    if isinstance(value, list): return [redact(item) for item in value]
    if isinstance(value, tuple): return [redact(item) for item in value]
    if isinstance(value, str): return _SECRET_PATTERN.sub(lambda m: m.group(1) + "[REDACTED]", value)
    return value

def _digest(event_data: dict[str, Any]) -> str:
    body = {k: v for k, v in event_data.items() if k != "hash"}
    return hashlib.sha256(json.dumps(body, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()).hexdigest()

class EventStore:
    """Log JSONL append-only, com persistência crash-safe e cadeia criptográfica."""
    def __init__(self, path: str | Path | None = None):
        self.path = Path(path) if path else None
        self._lock = threading.RLock(); self._events: list[Event] = []; self._idempotency: dict[str, Event] = {}
        if self.path and self.path.exists():
            with self.path.open(encoding="utf-8") as stream:
                for number, line in enumerate(stream, 1):
                    if not line.strip(): continue
                    try: data = json.loads(line)
                    except json.JSONDecodeError as exc: raise ValueError(f"invalid event at line {number}") from exc
                    event = Event(**data)
                    if event.hash and event.hash != _digest(data): raise ValueError(f"event hash mismatch at line {number}")
                    if self._events and event.previous_hash != self._events[-1].hash: raise ValueError(f"event chain mismatch at line {number}")
                    self._events.append(event)
                    if event.idempotency_key: self._idempotency[event.idempotency_key] = event

    def append(self, run_id: str, session_id: str, task_id: str, event_type: EventType | str,
               payload: dict[str, Any], idempotency_key: str | None = None) -> Event:
        with self._lock:
            lock_stream = None
            if self.path:
                self.path.parent.mkdir(parents=True, exist_ok=True)
                lock_stream = self.path.open("a+", encoding="utf-8")
                fcntl.flock(lock_stream.fileno(), fcntl.LOCK_EX)
                lock_stream.seek(0)
                self._events, self._idempotency = [], {}
                for line in lock_stream:
                    if line.strip():
                        event = Event(**json.loads(line)); self._events.append(event)
                        if event.idempotency_key: self._idempotency[event.idempotency_key] = event
            if idempotency_key and idempotency_key in self._idempotency:
                existing = self._idempotency[idempotency_key]
                if lock_stream:
                    fcntl.flock(lock_stream.fileno(), fcntl.LOCK_UN); lock_stream.close()
                return existing
            event_name = event_type.value if isinstance(event_type, EventType) else str(event_type)
            safe_payload = redact(payload)
            previous = self._events[-1].hash if self._events else "GENESIS"
            event = Event(new_id("event"), run_id, session_id, task_id, now_iso(), event_name, 1,
                          len(self._events), safe_payload, True, previous, "", idempotency_key)
            data = event.__dict__.copy(); data["hash"] = _digest(data); event = Event(**data)
            if self.path:
                lock_stream.seek(0, os.SEEK_END)
                lock_stream.write(json.dumps(event.__dict__, ensure_ascii=False, sort_keys=True) + "\n")
                lock_stream.flush(); os.fsync(lock_stream.fileno())
                fcntl.flock(lock_stream.fileno(), fcntl.LOCK_UN); lock_stream.close()
            self._events.append(event)
            if idempotency_key: self._idempotency[idempotency_key] = event
            return event

    def all(self) -> tuple[Event, ...]:
        with self._lock: return tuple(self._events)
    def replay(self, run_id: str | None = None, task_id: str | None = None) -> tuple[Event, ...]:
        return tuple(e for e in self.all() if (run_id is None or e.run_id == run_id) and (task_id is None or e.task_id == task_id))
    def count(self) -> int: return len(self._events)
    def verify_integrity(self) -> bool:
        previous = "GENESIS"
        for index, event in enumerate(self._events):
            data = event.__dict__.copy()
            if event.sequence != index or event.previous_hash != previous or event.hash != _digest(data): return False
            previous = event.hash
        return True
