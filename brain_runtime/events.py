from __future__ import annotations
import hashlib, json, os, re, threading, fcntl
from pathlib import Path
from typing import Any
from .models import Event, EventType, now_iso, new_id

_SECRET_KEYS = {"secret", "token", "password", "api_key", "apikey", "credential", "authorization"}
_SECRET_PATTERN = re.compile(r"(?i)(bearer\s+|sk-|ghp_|api[_-]?key\s*[=:])[^\s,;]+")

def redact(value: Any) -> Any:
    if isinstance(value, dict): return {key: "[REDACTED]" if str(key).lower() in _SECRET_KEYS else redact(item) for key, item in value.items()}
    if isinstance(value, (list, tuple)): return [redact(item) for item in value]
    if isinstance(value, str): return _SECRET_PATTERN.sub(lambda m: m.group(1) + "[REDACTED]", value)
    return value

def _digest(event_data: dict[str, Any]) -> str:
    body = {k: v for k, v in event_data.items() if k != "hash"}
    return hashlib.sha256(json.dumps(body, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()).hexdigest()

def _validate_event(data: dict[str, Any]) -> None:
    required = ("event_id", "run_id", "session_id", "task_id", "timestamp", "type", "version", "sequence", "payload", "previous_hash", "hash")
    missing = [key for key in required if key not in data]
    if missing: raise ValueError(f"event missing fields: {', '.join(missing)}")
    if data["version"] != 1 or not isinstance(data["payload"], dict): raise ValueError("invalid event schema")
    if data["hash"] and data["hash"] != _digest(data): raise ValueError("event hash mismatch")

class EventStore:
    """Append-only JSONL store with hash chain, idempotency, schema checks and crash recovery."""
    def __init__(self, path: str | Path | None = None):
        self.path = Path(path) if path else None; self._lock = threading.RLock(); self._events = []; self._idempotency = {}
        if self.path and self.path.exists(): self._load(recover_partial=True)

    def _load(self, recover_partial: bool = False) -> None:
        self._events, self._idempotency = [], {}
        with self.path.open("rb") as stream:
            lines = stream.readlines()
        for number, raw in enumerate(lines, 1):
            if not raw.strip(): continue
            try: data = json.loads(raw)
            except json.JSONDecodeError:
                if recover_partial and number == len(lines) and not raw.endswith(b"\n"):
                    with self.path.open("rb+") as repair: repair.truncate(sum(len(item) for item in lines[:-1]))
                    break
                raise ValueError(f"invalid event at line {number}")
            data.setdefault("correlation_id", data.get("run_id", "")); _validate_event(data); event = Event(**data)
            if self._events and (event.previous_hash != self._events[-1].hash or event.sequence != self._events[-1].sequence + 1): raise ValueError(f"event chain mismatch at line {number}")
            self._events.append(event)
            if event.idempotency_key: self._idempotency[event.idempotency_key] = event

    def append(self, run_id: str, session_id: str, task_id: str, event_type: EventType | str, payload: dict[str, Any], idempotency_key: str | None = None, correlation_id: str | None = None) -> Event:
        if not isinstance(payload, dict): raise ValueError("event payload must be an object")
        with self._lock:
            lock_stream = None
            if self.path:
                self.path.parent.mkdir(parents=True, exist_ok=True); lock_stream = self.path.open("a+", encoding="utf-8"); fcntl.flock(lock_stream.fileno(), fcntl.LOCK_EX); self._load(recover_partial=True)
            if idempotency_key and idempotency_key in self._idempotency:
                existing = self._idempotency[idempotency_key]
                if lock_stream: fcntl.flock(lock_stream.fileno(), fcntl.LOCK_UN); lock_stream.close()
                return existing
            event_name = event_type.value if isinstance(event_type, EventType) else str(event_type); previous = self._events[-1].hash if self._events else "GENESIS"
            event = Event(new_id("event"), run_id, session_id, task_id, now_iso(), event_name, 1, len(self._events), redact(payload), True, previous, "", idempotency_key, correlation_id or run_id)
            data = event.__dict__.copy(); data["hash"] = _digest(data); event = Event(**data); _validate_event(event.__dict__)
            if lock_stream:
                lock_stream.seek(0, os.SEEK_END); lock_stream.write(json.dumps(event.__dict__, ensure_ascii=False, sort_keys=True) + "\n"); lock_stream.flush(); os.fsync(lock_stream.fileno()); fcntl.flock(lock_stream.fileno(), fcntl.LOCK_UN); lock_stream.close()
            self._events.append(event)
            if idempotency_key: self._idempotency[idempotency_key] = event
            return event

    def all(self) -> tuple[Event, ...]:
        with self._lock: return tuple(self._events)
    def replay(self, run_id: str | None = None, task_id: str | None = None) -> tuple[Event, ...]:
        return tuple(e for e in self.all() if (run_id is None or e.run_id == run_id) and (task_id is None or e.task_id == task_id))
    def reconstruct(self, run_id: str) -> dict[str, Any]:
        state: dict[str, Any] = {"run_id": run_id, "status": "created", "approvals": {}, "steps": {}}
        for event in self.replay(run_id=run_id):
            state["last_event"] = event.type; payload = event.payload
            if event.type == EventType.APPROVAL_REQUESTED.value: state["approvals"][payload.get("approval_id", "")] = "PENDING"
            elif event.type == EventType.APPROVAL_GRANTED.value: state["approvals"][payload.get("approval_id", "")] = "APPROVED"
            elif event.type == EventType.APPROVAL_DENIED.value: state["approvals"][payload.get("approval_id", "")] = "DENIED"
            elif event.type in (EventType.DELIVERED.value, EventType.AGENT_COMPLETED.value): state["status"] = "completed" if payload.get("success", True) else "failed"
            elif event.type == EventType.RETRY.value: state["status"] = "retrying"
        return state
    def count(self) -> int: return len(self._events)
    def verify_integrity(self) -> bool:
        previous = "GENESIS"
        for index, event in enumerate(self._events):
            data = event.__dict__.copy()
            if event.sequence != index or event.previous_hash != previous or event.hash != _digest(data): return False
            previous = event.hash
        return True
