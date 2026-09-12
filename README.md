# Braim — runtime de referência

O Braim é um runtime experimental para execução de tarefas com Policy, approval, eventos auditáveis, Sandbox, workflows, memória, routing de providers e gates de QA. A implementação executável atual está em `brain_runtime/` e usa Python 3 com a biblioteca padrão.

> **Estado de segurança:** o projeto possui hardening significativo e 116 testes aprovados, mas ainda depende de infraestrutura do host para isolamento OS-level completo. Não deve ser interpretado como container ou ambiente de produção isolado sem uma implantação adequada.

## Executar testes

```bash
python3 -m unittest discover -s tests -q
```

A validação atual inclui contratos formais, policy/approval, hash chain e recovery, sandbox, workflows, APIs, skills, memória, observabilidade, delivery, Project Intelligence, Readiness Gate e integração E2E.

## Runtime e módulos

| Módulo | Responsabilidade |
|---|---|
| `brain_runtime/pipeline.py` | Secretary, research, planner, policy, router, dispatch, critic, correction, retry e learning |
| `brain_runtime/events.py` | EventStore append-only com hash chain, redaction, replay, rotação e retenção |
| `brain_runtime/sandbox.py` | Execução allowlisted, limites, timeout, cancelamento e artefatos |
| `brain_runtime/workflows.py` | Workflows persistentes com retry, leases, cancelamento e compensação |
| `brain_runtime/apis.py` | Catálogo e seleção de providers com quota, cooldown e fallback equivalente |
| `brain_runtime/skills.py` | Registry de skills com licença, provenance, assinatura e revogação |
| `brain_runtime/memory.py` e `learning.py` | Memória local, learning records e bridge de execução |
| `brain_runtime/project_intelligence.py` | Scan arquitetural e contexto `.projectbrain/` |
| `brain_runtime/readiness.py` | Gate de implementação, testes, QA, segurança, arquitetura, regressão e release |
| `brain_runtime/release_intelligence.py` | Comparação de commits/releases e heurísticas de regressão |
| `brain_runtime/evidence.py` | Claims, evidências, estado externo, confiança e decisão |
| `brain_runtime/context_pack.py` | Contexto estruturado para Planner/agentes |
| `brain_runtime/fix_verify_learn.py` | Ciclo scan → task → fix → verify → learn |
| `brain_runtime/runtime.py` | Orquestração E2E, delivery, readiness e replay |

## Integração de Project Intelligence

O scanner e o readiness gate permanecem opcionais apenas no modo `development`:

```python
from brain_runtime.project_intelligence import ProjectScanner
from brain_runtime.readiness import ReadinessGate
from brain_runtime.runtime import RuntimeCoordinator

runtime = RuntimeCoordinator(
    pipeline=pipeline,
    delivery=delivery,
    events=events,
    project_scanner=ProjectScanner("/path/to/project"),
    readiness_gate=ReadinessGate(),
    enforce_readiness=True,
    mode="production",
)
```

Os modos `offline`, `sandboxed`, `strict` e `production` exigem `ReadinessGate` e não aceitam `enforce_readiness=False`. Em `development`, o default continua compatível (`False`). A configuração equivalente fica em `execution.mode` e `execution.enforce_readiness`; a validação rejeita a desativação em modos protegidos.

Quando configurado, o runtime atualiza `.projectbrain/`, emite `ProjectScanned` e `ReadinessEvaluated`, e bloqueia a conclusão quando existem blockers. O `RuntimeCoordinator` emite uma `ExecutionAuthorization` vinculada à instância e ao modo do pipeline; `BrainPipeline.run()` e `resume()` rejeitam chamadas sem essa capability. A rota `run_internal_for_tests()`/`resume_internal_for_tests()` existe apenas para `development` e é bloqueada em modos protegidos.

## Kotlin e Android

O repositório agora também contém o projeto Gradle do Sandbox Mobile integrado ao BrainCode. O módulo `:brain` é Kotlin/JVM puro para Policy, Router, Planner, Prompt, QA e contratos; `:android-module` fornece a sessão de agente, resolução de capabilities e runtime Sandbox; `:app` contém o cliente Android Compose e os recursos do RootFS. O plano de integração está em [`docs/PLANO_INTEGRACAO_BRAIN_SANDBOX.md`](docs/PLANO_INTEGRACAO_BRAIN_SANDBOX.md).

Validações locais disponíveis:

```bash
./gradlew :brain:test
python3 -m unittest discover -s tests -q
```

Os testes de `:android-module` exigem Android SDK configurado via `ANDROID_HOME` ou `local.properties`; sem esse SDK, o Gradle não consegue configurar a biblioteca Android.

## Documentação técnica

- [Auditoria técnica atual](AUDITORIA_PESADA.md)
- [Roadmap implementado](docs/ROADMAP_IMPLEMENTADO.md)
- [Status detalhado de implementação](docs/IMPLEMENTATION_STATUS.md)
- [Contratos](contracts/)
- [Testes](tests/)

## Limites de implantação

Para isolamento forte, a implantação deve fornecer container rootless ou sandbox OS-level, cgroups graváveis, política de rede, filesystem jail e, quando aplicável, autoridade de assinatura. O runtime rejeita controles estritos ausentes e não simula capacidades que o host não fornece.
