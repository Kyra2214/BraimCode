# Auditoria pesada do Braim

**Escopo:** commits `168424c` até `f4fe726`, runtime Python em `brain_runtime/`, contratos Kotlin/Markdown, testes em `tests/` e claims em `README.md`/`docs/ROADMAP_IMPLEMENTADO.md`.

**Data:** 2026-09-12. **Método:** leitura linha a linha, comparação com o roadmap, suíte nominal, compilação Python e probes adversariais concorrentes. A suíte nominal executou 12 testes com sucesso. Os probes confirmaram cinco falhas estruturais.

## Veredito executivo

A implementação é um **protótipo funcional superficial**, não um runtime seguro pronto para execução real. Há bons esqueletos de interfaces, eventos, persistência e fluxo nominal, mas controles declarados como Policy, Sandbox, secrets, isolamento, idempotência e QA não são aplicados de forma vinculante.

O risco mais grave é o Sandbox: a allowlist de comandos é fornecida pelo próprio `SandboxJob`, o `cwd` é arbitrário e `python3` permite código nativo Python com acesso ao host. Portanto, a afirmação de “executor seguro” não é sustentada pelo código. O PolicyBroker também autoriza um recurso sem validar o recurso, o budget, os roots, a rede ou a expiração da decisão.

## Sumário de severidade

| ID | Severidade | Área | Situação |
|---|---|---|---|
| F-01 | **Crítica** | Sandbox | O job controla `allowed_commands`; é possível autorizar `sh` no próprio pedido. |
| F-02 | **Crítica** | Sandbox | `python3` é tratado como sandbox, mas permite acesso ao filesystem, processos e rede do host. |
| F-03 | **Crítica** | Sandbox | Não há isolamento de filesystem, rede, usuário, capabilities, memória, CPU ou descendentes. |
| F-04 | **Alta** | Policy | `resource` nunca é validado contra roots, capability ou escopo autorizado. |
| F-05 | **Alta** | Policy | `budget`, `network_allowed`, `risk_class` e `expires_at` são principalmente metadados; não são gates executáveis. |
| F-06 | **Alta** | Policy | `approval=ASK` não possui mecanismo de aprovação nem token vinculante; o chamador pode escolher outro contexto/ator. |
| F-07 | **Alta** | EventStore | Instâncias concorrentes geram sequências duplicadas e podem perder/corromper eventos. |
| F-08 | **Alta** | EventStore/Secrets | Redaction por nomes é incompleto; valores sensíveis em strings, headers e referências podem vazar. |
| F-09 | **Alta** | Workflow | A mesma idempotency key executa duas vezes sob concorrência; não existe lock/claim transacional. |
| F-10 | **Alta** | Workflow | Persistência usa sobrescrita não atômica, sem fsync, sem recovery e sem validação de versão/grafo. |
| F-11 | **Alta** | E2E | O E2E testado não passa pelo pipeline, Policy, routing, memória ou workflow; valida apenas executor direto. |
| F-12 | **Média/Alta** | QA | QA aceita apenas a presença de substring em stdout; não verifica contrato, artefatos, hashes, invariantes ou critérios completos. |
| F-13 | **Média** | Routing/API | `fallback_group` é ignorado; quota conhecida é apagada após cada chamada (`record(... quota_remaining=None)`). |
| F-14 | **Média** | Routing/API | Qualquer exceção do provider vira fallback; erros de programação podem ser mascarados. Não existe timeout/circuit breaker real. |
| F-15 | **Média** | Skills | `required_permissions`, `resources` e `tools` não são aplicados; `body_path` não é validado, lido, hashado ou assinado. |
| F-16 | **Média** | Memória | Lock é apenas por instância; conexões concorrentes não coordenam escrita. Não há limites/validação de qualidade ou recovery. |
| F-17 | **Média** | Pipeline | Não existe `PlanCreated`, validação, retry, correction, delivery ou registro de experiência. O Policy é chamado, mas a decisão não governa um executor real. |
| F-18 | **Média** | Build/testes | Não há Gradle/Kotlin build nem testes Kotlin; a implementação Python é uma segunda arquitetura não integrada ao código Kotlin. |

## Auditoria fase por fase

### Fase A — Contrato Brain ↔ Sandbox

O contrato Kotlin em `app/src/main/kotlin/com/brain/execution/SandboxContract.kt` melhorou significativamente o rascunho: possui identidade, idempotência, risco, aprovação, budget, cancelamento, referências e manifesto. Porém, é apenas um modelo de dados. Não há serialização/schema versionado, validação de paths, semântica de aprovação, enforcement de budget, cancelamento real ou contract tests. O runtime Python cria um segundo contrato (`SandboxJob`) incompatível e não consome o contrato Kotlin. Isso é uma divergência arquitetural e aumenta o risco de bypass.

**Conclusão:** contrato parcialmente implementado; enforcement ausente.

### Fase B — Estudos

A fase é documental. Os estudos existem, mas “marcar como concluída” não produz comportamento executável nem critérios de aceite. Isso não é um problema de segurança por si só, mas a conclusão foi usada como base para afirmar que as fases seguintes estavam completas sem gates técnicos.

**Conclusão:** documentação concluída; não deve ser confundida com implementação.

### C1 — PolicyBroker

Em `brain_runtime/policy.py:17-43`, a decisão ALLOW depende apenas de capability registrada, capability associada ao actor, algumas strings de risco e aprovação. O parâmetro `resource` só aparece na mensagem de motivo. Um actor autorizado para `filesystem_write` em `/safe` recebe ALLOW para `/etc`, `/proc` ou qualquer outro caminho. O probe `policy_resource_probe` confirmou isso.

A condição da linha 27 é logicamente inadequada: verifica `network_allowed` junto com uma string `"network"` em `filesystem_roots`; isso não protege filesystem nem rede. `budget` é copiado para a decisão, mas nunca consumido. `expires_at` é calculado, mas não existe função que valide a decisão ou a invalide. `approval=ASK` apenas muda o retorno; não existe estado de aprovação, identidade do aprovador, nonce, expiração de aprovação ou vínculo entre aprovação e execução. `with_actor_capability` permite ao código que já possui o broker criar novas permissões sem trilha de auditoria.

**Breach principal:** Policy pode dizer ALLOW para um recurso não autorizado e o Sandbox não exige uma PolicyDecision verificável.

### C2 — EventStore

`EventStore` protege somente a lista em memória de uma instância com `RLock`. Duas instâncias apontando para o mesmo JSONL calculam `sequence=len(self._events)` independentemente. O probe concorrente confirmou sequências duplicadas. Escritas em append não têm lock de processo, `fsync`, checksum, transação, arquivo temporário ou recuperação de linha parcial. Um crash durante a escrita pode tornar todo o store ilegível no startup.

O redactor só redige chaves exatamente iguais a `secret`, `token`, `password`, `api_key` e `credential`. Não cobre `authorization`, `access_token`, `refresh_token`, `private_key`, `secret_ref`, valores em texto livre, listas de strings ou dados serializados dentro de uma string. A documentação afirma “sem secrets”, mas a garantia não existe.

### C3 — Pipeline

O pipeline registra alguns eventos e chama Policy, mas não cria `Plan`, não valida o resultado, não implementa correction/retry/fallback, não registra memória/aprendizado e não entrega artefato. `KeywordSecretary` usa busca por substring, portanto há classificação frágil e possível confusão de capacidades. O actor vem do argumento do chamador e o provider escolhido pelo router é apenas texto.

A decisão Policy não é anexada ao `ExecutionRequest` nem verificada pelo dispatcher. Qualquer dispatcher recebido por injeção pode ignorá-la. O teste nominal usa um dispatcher fake, sem enforcement.

### Fase D — Memória persistente

A memória SQLite tem schema mínimo e usa parâmetros SQL adequadamente para a busca. Entretanto, `check_same_thread=False` combinado com um lock apenas local não cria segurança para múltiplas instâncias/conexões. Não há `busy_timeout`, retry de lock, migração/schema version, checksum, limites de payload, validação de `quality` ou controle de duplicidade idempotente além da PK que apenas lança exceção.

A implementação registra experiências somente quando chamada diretamente; o pipeline não chama `SQLiteExperienceMemory`. Portanto, a afirmação de que cada execução gera aprendizado não se verifica.

### Fase E — Skills e agentes

O registro rejeita `community`, o que é positivo, mas confiança é apenas uma string fornecida pelo próprio código que registra. Não há assinatura, provenance, hash do body, allowlist de origem ou revisão. `body_path`, `tools`, `resources` e `required_permissions` não são resolvidos nem validados. A Skill pode declarar permissões que nunca passam pelo PolicyBroker. O roteamento usa substring e comparação lexical de versão (`"10" < "2"` em versões), sem score de evidência, QA ou agente especialista real.

### Fase F — Workflows

`next_nodes` é armazenado e nunca usado: o engine executa todos os nós na ordem da tupla, não o grafo. Não valida nós órfãos, ciclos, capacidades ou manifesto. A mesma key é idempotente apenas depois que o estado `completed` foi salvo. Duas threads entram antes da gravação e executam o handler duas vezes; o probe confirmou. A key não é vinculada ao hash do payload/manifesto, então pode ser reutilizada para outro workflow ou versão.

O estado é gravado com `write_text` diretamente. Interrupção pode truncar o JSON. Não há lock, fsync, atomic rename ou journal. Uma falha depois do efeito externo e antes do save repete o efeito. Falhas são persistidas, mas uma chamada posterior pode tentar novamente sem política explícita de recuperação/compensação.

### Fase G — APIs, routing e fallback

O catálogo calcula score, mas não valida faixas de qualidade/confiabilidade, não filtra `fallback_group` no waterfall e não registra provenance, custo, termos, health check ou quota real. Em `waterfall`, toda exceção é capturada como falha do provider; bug interno, erro de schema e timeout podem ser mascarados como fallback. Não há timeout no callback.

A linha 56 chama `record(..., quota_remaining=None)`, apagando uma quota conhecida. Falhas sucessivas deixam o provider permanentemente unhealthy, sem janela de recuperação. O catálogo não é protegido para concorrência. Routing do pipeline usa `StaticRouter`, ignorando o catálogo dinâmico completamente.

### Fase H — Sandbox, QA e E2E

O executor valida `job.argv[0]` contra `job.allowed_commands`, mas essa allowlist faz parte do job não confiável. Assim, um job pode enviar `allowed_commands=("sh",)` e executar shell; o probe confirmou. Mesmo com a allowlist padrão, `python3 -c` é execução arbitrária. O probe confirmou execução de código Python, e o código não desabilita socket, acesso a `/etc`, criação de processos ou leitura/escrita fora do `cwd`.

`cwd` é apenas resolvido e verificado como diretório; não é comparado com uma raiz autorizada. `network_allowed` nunca é usado. Não há namespace, container, seccomp, usuário não privilegiado, limites de CPU/memória, nofile, pids, cgroup ou kill do grupo de processos. `subprocess.run(timeout=...)` não garante matar descendentes. `env` mínimo não é isolamento.

A coleta de artefatos percorre todo o root e lê arquivos sem filtrar manifesto, symlink, segredo ou limite agregado. Em timeout, não há coleta/limpeza e processos descendentes podem sobreviver. `QAGate` aceita o resultado quando só existe o diagnóstico `duration_ms=...`; não verifica artefatos esperados, hashes, contrato, exit code além do status ou invariantes. O E2E cobre somente `Orchestrator → SandboxExecutor → QAGate`, não o fluxo completo `TaskSpec → Policy → Router → Agent → Sandbox → Memory → Delivery`.

## Evidências executadas

Comando nominal:

```bash
python3 -m unittest discover -s tests
# 12 testes: OK
```

Probes adversariais:

```bash
PYTHONPATH=. python3 tests/audit_probes.py
```

Resultado confirmado:

```text
CONFIRMED: event_concurrent_instances_probe
CONFIRMED: policy_resource_probe
CONFIRMED: sandbox_command_probe
CONFIRMED: sandbox_python_escape_probe
CONFIRMED: workflow_concurrent_probe
```

O primeiro modo de execução sem `PYTHONPATH=.` falhou com `ModuleNotFoundError`, revelando também que a forma indicada não é diretamente executável a partir do diretório `tests` sem configuração de empacotamento.

## Correções obrigatórias antes de chamar o sistema de seguro

1. **Congelar o executor atual:** não permitir execução real com essa implementação. Substituir por backend isolado de verdade (container/rootless VM/proot com limites comprovados), allowlist server-side imutável e PolicyDecision assinada/verificada.
2. **Reprojetar Policy:** canonicalizar recursos e paths, validar roots, capability/resource matrix, network e budgets, exigir aprovação vinculada a `decision_id`, impor TTL no executor e remover capacidade de autoelevação.
3. **Unificar contratos:** escolher Kotlin ou Python como fonte de verdade, adicionar schema versionado, contract tests e integração real entre Policy, request, Sandbox e resultado.
4. **Corrigir persistência:** usar SQLite transacional para eventos/workflows, sequência por transação, WAL/busy timeout, atomicidade, fsync/recovery, schema migration e idempotency claim com unique constraint.
5. **Implementar idempotência real:** key vinculada a hash do pedido/manifesto, estado `running` com lease, fencing token e política clara para crash após efeito externo.
6. **Endurecer secrets:** secret manager por referência, redaction estruturada por schema, bloqueio de secrets em stdout/erro/artefato/memória e testes de vazamento.
7. **Corrigir routing:** usar catálogo real no pipeline, fallback por grupo, timeout, classificação de erros, quota decremental, health recovery, provenance e validação de resposta.
8. **Construir E2E real:** executar o fluxo inteiro com Policy ALLOW/ASK/DENY, aprovação, retry, correction, persistência, replay, QA de contratos/artefatos e delivery somente após evidência verificável.
9. **Adicionar concorrência e recovery tests:** múltiplos processos no EventStore/SQLite, crash injection, duplicação de chamadas, timeout com descendentes, symlink/path traversal e reinício durante cada transição.

## Conclusão

O código demonstra interfaces e happy paths, mas **não atende ainda aos critérios de segurança, auditoria, concorrência e execução segura do roadmap**. Os claims em `docs/ROADMAP_IMPLEMENTADO.md` devem ser rebaixados de “implementado” para “protótipo parcial” até que os achados críticos e altos sejam corrigidos e validados por testes adversariais e E2E real.
