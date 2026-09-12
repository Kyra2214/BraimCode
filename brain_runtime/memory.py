from __future__ import annotations

import json
import sqlite3
import threading
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class Experience:
    experience_id: str
    run_id: str
    task_id: str
    problem: str
    source: str
    strategy: str
    result: str
    quality: float
    errors: tuple[str, ...] = ()
    provenance: tuple[str, ...] = ()
    created_at: str = ""


class SQLiteExperienceMemory:
    def __init__(self, path: str | Path = ":memory:"):
        self.path = str(path)
        self._lock = threading.RLock()
        self._connection = sqlite3.connect(self.path, check_same_thread=False)
        self._connection.execute("PRAGMA journal_mode=WAL")
        self._connection.execute("""CREATE TABLE IF NOT EXISTS experiences (
            experience_id TEXT PRIMARY KEY, run_id TEXT NOT NULL, task_id TEXT NOT NULL,
            problem TEXT NOT NULL, source TEXT NOT NULL, strategy TEXT NOT NULL,
            result TEXT NOT NULL, quality REAL NOT NULL, errors_json TEXT NOT NULL,
            provenance_json TEXT NOT NULL, created_at TEXT NOT NULL
        )""")
        self._connection.commit()

    def record(self, experience: Experience) -> None:
        value = experience if experience.created_at else Experience(**{**asdict(experience), "created_at": datetime.now(timezone.utc).isoformat()})
        with self._lock:
            self._connection.execute("INSERT INTO experiences VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", (
                value.experience_id, value.run_id, value.task_id, value.problem, value.source,
                value.strategy, value.result, value.quality, json.dumps(value.errors),
                json.dumps(value.provenance), value.created_at))
            self._connection.commit()

    def search(self, problem: str, limit: int = 10) -> list[Experience]:
        pattern = f"%{problem}%"
        rows = self._connection.execute("SELECT * FROM experiences WHERE problem LIKE ? ORDER BY created_at DESC LIMIT ?", (pattern, limit)).fetchall()
        return [Experience(row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7], tuple(json.loads(row[8])), tuple(json.loads(row[9])), row[10]) for row in rows]

    def count(self) -> int:
        return int(self._connection.execute("SELECT COUNT(*) FROM experiences").fetchone()[0])

    def close(self) -> None:
        self._connection.close()


class SemanticMemory:
    """Índice local determinístico; um backend vetorial pode ser conectado depois."""
    def __init__(self, backend: SQLiteExperienceMemory):
        self.backend = backend

    def remember(self, experience: Experience) -> None:
        if any(marker in experience.result.lower() for marker in ("api_key", "token=", "password=")):
            raise ValueError("secret-like content cannot be persisted")
        if not experience.provenance:
            raise ValueError("semantic memory requires provenance")
        self.backend.record(experience)

    def retrieve(self, query: str, limit: int = 10) -> list[Experience]:
        return self.backend.search(query, limit)


class LearningLoop:
    def __init__(self, memory: SemanticMemory):
        self.memory = memory

    def record_feedback(self, experience: Experience, validated: bool, evidence: tuple[str, ...] = ()) -> Experience:
        quality = max(0.0, min(1.0, experience.quality + (0.1 if validated else -0.1)))
        updated = Experience(experience.experience_id, experience.run_id, experience.task_id,
            experience.problem, experience.source, experience.strategy, experience.result, quality,
            experience.errors, tuple(dict.fromkeys((*experience.provenance, *evidence))), experience.created_at)
        self.memory.remember(updated)
        return updated
