# Roadmap implementado

## Estado atual

A implementação executável de referência está em `brain_runtime/` e utiliza a biblioteca padrão do Python 3. O código Kotlin em `app/` permanece como referência de contratos e extensões; o aplicativo Android ainda não foi iniciado.

A suíte atual possui **112 testes aprovados** e é executada com:

```bash
python3 -m unittest discover -s tests -q
```

## Componentes implementados

| Área | Componentes principais | Estado |
|---|---|---|
| Contratos | `contracts.py`, `contract_registry.py` | Schemas formais, tipos aninhados, round-trip e migrações |
| Policy | `policy.py`, `approval.py`, `binding.py` | Deny-by-default, TTL, approval persistente, anti-replay e binding imutável |
| Eventos | `events.py`, `recovery.py`, `golden_events.py` | Append-only, hash chain, redaction, idempotência, replay, recovery parcial, rotação e retenção |
| Pipeline | `pipeline.py`, `planner.py`, `research.py` | Secretary, research, plan, policy, router, dispatch, critic, correction, retry, QA e learning opcional |
| Sandbox | `sandbox.py`, `host_controls.py` | Allowlist, argumentos, limites, timeout, cancelamento, artefatos, namespaces e fail-closed estrito |
| Workflows | `workflows.py` | Estado persistente, leases, fencing, retry/backoff, timeout, cancelamento, validação e compensação |
| APIs | `apis.py`, `api_discovery.py` | Catálogo, score, quota, cooldown, probes, persistência, reserva e fallback equivalente |
| Skills | `skills.py`, `skill_adapters.py`, `signatures.py` | Allowlist de licença, provenance, scan, assinatura opcional, quarentena e revogação |
| Memória | `memory.py`, `learning.py` | SQLite, deduplicação, retenção, provenance e bridge de execução |
| QA/Delivery | `delivery.py`, `orchestrator.py` | Evidência obrigatória, QA gate, validation e delivery condicionado |
| Observabilidade | `observability.py`, `instrumentation.py` | Spans, counters, gauges, alertas e export estruturado |
| Project Intelligence | `project_intelligence.py` | Scanner, riscos, providers, releases e `.projectbrain/` |
| Readiness | `readiness.py` | Score, blockers, warnings, stages e exit code |
| Release Intelligence | `release_intelligence.py` | Comparação Git e heurística inicial de regressões |
| Evidence | `evidence.py` | Claim, evidence, external state, confidence e decision |
| Context | `context_pack.py` | Context pack para Planner e agentes |
| Fix/Verify/Learn | `fix_verify_learn.py` | Ciclo scan, task, fix, verify e learn |
| Runtime | `runtime.py` | Integração opcional de scanner/readiness com pipeline, delivery e replay |

## Linha de commits atual

| Commit | Entrega |
|---|---|
| `d271f64` | Isolamento de rede padrão e retomada de approval |
| `589f340` | Contratos formais, APIs, skills e credenciais |
| `619189c` | Workflows, Sandbox e telemetria operacional |
| `bf24bf8` | Project Intelligence, Readiness, Evidence, Context e Fix/Verify/Learn |
| `32e805a` | Integração de scanner e readiness ao RuntimeCoordinator |

Todos os commits estão publicados em `origin/main`.

## Limites explícitos

O runtime não afirma possuir isolamento OS-level quando o host não fornece essa capacidade. cgroups graváveis, Bubblewrap efetivo, seccomp, capabilities, autoridade remota de chaves e backends distribuídos continuam dependências da implantação.

O catálogo de APIs e o Evidence Engine não comprovam que um provider externo esteja operacional. Eles registram provenance, estado desconhecido e evidência verificável separadamente.

O Android Mobile não está implementado como aplicativo. Não há Gradle, `AndroidManifest.xml`, UI, Keystore, Service ou APK. Essa etapa permanece planejada para depois da estabilização do runtime.

## Critério de conclusão

Uma etapa somente deve ser chamada de concluída quando possuir implementação no runtime, testes automatizados e comportamento documentado. Integrações externas devem ser marcadas como dependências de implantação, não como funcionalidades simuladas.
