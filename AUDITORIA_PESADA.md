# Auditoria técnica atual do Braim / BrainCode

**Data da auditoria:** 2026-09-12 (atualizada na mesma data após varredura de instanciação real do código Kotlin). **Escopo:** runtime Python em `brain_runtime/`, módulos Kotlin `brain/`, `android-module/` e `app/`, documentação, configuração, releases e testes. **Método:** inspeção dos módulos, execução da suíte Python, compilação, verificação de diff, e — passo adicional desta rodada — rastreamento de instanciação real de cada classe Kotlin (quem chama quem fora do próprio arquivo/teste), não só leitura de código isolado.

## Veredito executivo

O Braim/BrainCode tem hoje **três camadas executáveis, com integração parcial entre o app Android e o Brain Kotlin**:

1. **Runtime Python (`brain_runtime/`)** — reference runtime funcional, testável e auditável, com 134 testes automatizados. Continua não devendo ser classificado como plataforma de produção plenamente isolada (isolamento OS-level depende do host).
2. **App Android (`:app` + `:android-module`)** — roda de verdade num device: baixa o rootfs, executa comandos via `proot`, instala plugins de um catálogo local. É o único caminho que um usuário final realmente aciona.
3. **Módulo `:brain` (Kotlin)** — porta do Braim original para Kotlin/JVM (Policy, Router, Skills, Workflows, Memory, Discovery, Events). A primeira fatia agora é acionada pelo app através de `BrainSandboxController.healthCheck()` (`sandbox.health`); o restante do módulo continua isolado e sem integração de planos de usuário.

Isso corrige uma alegação desta auditoria em sua versão anterior ("implementação Android permanece fora do escopo executável"): hoje **existe** app Android real, com Gradle, `AndroidManifest.xml`, Activity e testes rodando (`:app:testDebugUnitTest`, `:app:assembleDebug` "BUILD SUCCESSFUL", ver `ROADMAP_UNIFICADO.md`). A ponte inicial existe para `sandbox.health`; a lacuna restante é integrar planos de usuário e os demais subsistemas sem manter caminhos paralelos.

Detalhamento completo dos componentes órfãos (implementados, testados, nunca chamados fora do próprio arquivo) e o plano para resolver isso está em `PLANO_DE_ACAO.md`.

## Evidências atuais

```text
python3 -m unittest discover -s tests -q
Ran 134 tests
OK

python3 -m compileall -q brain_runtime tests
OK
```

Kotlin: `:brain` e `:app` compilam e testam (ver sessões registradas em `ROADMAP_UNIFICADO.md`); `:android-module` exige Android SDK configurado (`ANDROID_HOME`/`local.properties`) para rodar seus próprios testes neste ambiente de auditoria.

## Matriz de estado

| Área | Estado atual | Evidência principal | Limite restante |
|---|---|---|---|
| Contratos Python | Implementado | `contracts.py`, `contract_registry.py`, contract tests | Unificação futura com o contrato Kotlin |
| Policy e approval | Implementado no runtime | `policy.py`, `approval.py`, testes de anti-replay | Assinatura externa de decisões e autorização centralizada |
| EventStore | Implementado com hash chain, redaction, recovery parcial, rotação e retenção | `events.py`, security/golden tests | Escala multi-host e storage transacional externo |
| Pipeline | Implementado com research, plan, policy, binding, correction, retry e learning opcional | `pipeline.py`, integration tests | Planner/router ainda são substituíveis e simplificados |
| Sandbox | Hardening defensivo e namespaces quando suportados | `sandbox.py`, isolation/security tests | cgroups, Bubblewrap e kernel capabilities dependem do host |
| Workflows | Implementado com persistência, leases, retry, timeout, cancelamento e compensação | `workflows.py`, workflow tests | backend distribuído real e fencing multi-host |
| APIs | Catálogo persistente com quota, cooldown, probes, score e fallback equivalente | `apis.py`, routing tests | health real e credenciais dependem dos providers |
| Skills | Licença, assinatura opcional, provenance, quarentena e revogação | `skills.py`, adapters e tests | autoridade remota de chaves e execução isolada |
| Memória/Learning | SQLite, deduplicação, retenção, provenance e bridge de execução | `memory.py`, `learning.py` | semântica vetorial e storage distribuído |
| QA/Delivery | QA gate, validation evidence e delivery condicionado | `delivery.py`, E2E tests | cobertura de providers reais e artefatos externos |
| Observabilidade | Spans, counters, gauges, alerts e export JSON | `observability.py`, integration tests | exporter/collector externo |
| Project Intelligence | Scanner e contexto `.projectbrain/` | `project_intelligence.py` | análise semântica profunda e CI remoto |
| Readiness | Gate com score, blockers, warnings e exit code | `readiness.py`, runtime E2E | políticas de release específicas da organização |
| Release Intelligence | Comparação Git e heurística de regressão | `release_intelligence.py` | diagnóstico causal e histórico de produção |
| Android Mobile (Sandbox) | Implementado e acionável pela UI | `:app`, `:android-module`, `AndroidManifest.xml`, Activity Compose, aba Operações | validação em device/emulador, RootFS/proot real, assinatura e gates de produção |
| Integração `:brain` ↔ `:app` | Primeira fatia real (`sandbox.health`) | `SandboxViewModel` → `BrainSandboxController` → `BrainSandboxExecutionBridge` | integrar planos de usuário, aprovação/retomada e demais subsistemas — ver `PLANO_DE_ACAO.md` |

## Riscos remanescentes

### Isolamento do Sandbox

O Sandbox não deve ser interpretado como container completo apenas por executar `python3` ou `unshare`. O runtime valida comandos, argumentos, paths, symlinks, extensões, artefatos, recursos, cancelamento e timeout. Quando solicitado, tenta namespaces de usuário, montagem, PID e rede. O modo estrito rejeita a execução quando o host não fornece o controle exigido.

Filesystem jail via Bubblewrap, cgroups graváveis, seccomp, capabilities mínimas e políticas de kernel continuam responsabilidades da implantação. O código não simula essas garantias.

### Contrato Kotlin e integração Brain ↔ Sandbox

O contrato Kotlin em `brain/src/main/kotlin/com/brain/execution/SandboxContract.kt` é útil como referência e não é consumido pelo runtime Python (isso é esperado — são duas implementações paralelas, não uma dependendo da outra). A primeira operação real agora instancia `BrainSandboxController` após o preparo do runtime e percorre `BrainSandboxExecutionBridge`/`CicloExecucaoPlano` para `sandbox.health`. Isso ainda não constitui um "Ciclo Android unificado": planos de usuário, aprovação/retomada e os demais componentes do `:brain` não são acionados pelo app (ver `PLANO_DE_ACAO.md`, Fase B).

Dentro do próprio `app`, `SandboxPlatform` instancia `WorkspaceManager`, `GitManager`, `ServiceManager`, `TestLab`, segurança e toolchains; a aba **Operações** aciona os fluxos básicos de Workspace, Git status, SQLite, TestLab, avaliação de segurança e toolchains. Operações Git completas, terminal dedicado, executor adversarial e validação em device continuam pendentes.

### Routing e providers

O catálogo implementa seleção e contabilidade local. A disponibilidade real, a qualidade semântica da resposta, os termos do provider e a validade de uma credential reference dependem de integrações externas. O dispatcher deve continuar validando binding, policy e schema antes de executar.

### Readiness e evidência

O Readiness Gate é um mecanismo de decisão local. Ele não prova estado externo de providers. O Evidence Engine mantém essa distinção por meio do campo `external_state`; a presença de código ou dependência não é tratada como funcionamento externo confirmado.

## Recomendações de implantação

1. Executar em container rootless ou sandbox OS-level configurado pelo operador.
2. Exigir `isolation_required=True` e `cgroup_path` em ambientes que demandem isolamento forte.
3. Usar backend transacional distribuído para leases e workflows multi-host.
4. Configurar autoridade de chaves para skills e providers antes de permitir conteúdo externo.
5. Ativar `enforce_readiness=True` em pipelines de release.
6. Manter o Android como cliente/controlador futuro, sem duplicar a Policy no dispositivo.

## Conclusão

O código atual sustenta três classificações distintas, que não devem ser misturadas: (1) **runtime de referência funcional com hardening significativo** no Python; (2) **app Android de validação funcional**, que roda no device mas expõe só uma fração do que está implementado (rootfs + plugins locais); (3) **biblioteca Kotlin (`:brain`) funcional e testada, agora com uma primeira operação integrada ao app**, mas ainda sem unificação de planos, aprovação/retomada e demais subsistemas. Nenhuma das três sustenta a classificação de "plataforma de produção plenamente isolada" ou "Brain e Sandbox unificados" — a próxima evolução está detalhada em `PLANO_DE_ACAO.md`.

## Referências internas

- [README.md](README.md)
- [PLANO_DE_ACAO.md](PLANO_DE_ACAO.md) — o que fazer com cada achado desta auditoria
- [ROADMAP_UNIFICADO.md](ROADMAP_UNIFICADO.md)
- [brain_runtime/](brain_runtime/)
- [brain/](brain/)
- [tests/](tests/)
