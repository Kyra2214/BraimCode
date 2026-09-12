from __future__ import annotations
import json, sqlite3, threading
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from .models import new_id

@dataclass(frozen=True)
class LearningRecord:
    record_id: str
    run_id: str
    task_id: str
    problem: str
    strategy: str
    references: tuple[str, ...]
    result: str
    quality: float
    cost: float = 0.0
    duration_ms: int = 0
    errors: tuple[str, ...] = ()
    validation_evidence: tuple[str, ...] = ()
    created_at: str = ""

class LearningStore:
    def __init__(self, path: str | Path = ":memory:"):
        self._lock = threading.RLock(); self._db = sqlite3.connect(str(path), check_same_thread=False)
        self._db.execute("CREATE TABLE IF NOT EXISTS learning (id TEXT PRIMARY KEY, payload TEXT NOT NULL)"); self._db.commit()
    def record(self, item: LearningRecord) -> LearningRecord:
        if not item.validation_evidence: raise ValueError("learning record requires validation evidence")
        if not 0 <= item.quality <= 1 or item.cost < 0 or item.duration_ms < 0: raise ValueError("invalid learning metrics")
        value = item if item.created_at else LearningRecord(**{**item.__dict__, "created_at": datetime.now(timezone.utc).isoformat()})
        with self._lock: self._db.execute("INSERT INTO learning VALUES (?, ?)", (value.record_id, json.dumps(value.__dict__))); self._db.commit()
        return value
    def search(self, problem: str, limit: int = 10) -> list[LearningRecord]:
        rows = self._db.execute("SELECT payload FROM learning WHERE payload LIKE ? ORDER BY rowid DESC LIMIT ?", (f"%{problem}%", limit)).fetchall()
        return [LearningRecord(**json.loads(row[0])) for row in rows]
    def count(self) -> int: return self._db.execute("SELECT COUNT(*) FROM learning").fetchone()[0]

class FeedbackLoop:
    def __init__(self, store: LearningStore): self.store = store
    def learn(self, item: LearningRecord, validated: bool, evidence: tuple[str, ...] = ()) -> LearningRecord:
        quality = max(0.0, min(1.0, item.quality + (.1 if validated else -.1)))
        updated = LearningRecord(new_id("learning"), item.run_id, item.task_id, item.problem, item.strategy, item.references, item.result, quality, item.cost, item.duration_ms, item.errors, tuple(dict.fromkeys((*item.validation_evidence, *evidence))), item.created_at)
        return self.store.record(updated)
