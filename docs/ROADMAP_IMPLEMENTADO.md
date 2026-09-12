# Roadmap implementado

O roadmap consolidado foi executado em sequência. A implementação executável de referência está em `brain_runtime/` e usa apenas a biblioteca padrão do Python 3, mantendo o código Kotlin existente como esqueleto de contratos e extensões.

| Fase | Implementação | Commit |
|---|---|---|
| A | Contrato Brain ↔ Sandbox com risco, aprovação, orçamento, cancelamento, idempotência, secrets por referência, artefatos e eventos | `168424c` |
| B | Estudos GPT × Claude × Manus documentados e marcados como concluídos | `a025fbf` |
| C1/C2 | `PolicyBroker` deny-by-default e `EventStore` append-only/replay com redaction | `406e163` |
| C3 | Pipeline substituível Secretary → Router → Prompt → Policy/Event trace → Dispatch | `2757708` |
| D | Memória persistente SQLite com provenance e recuperação | `1ef0d85` |
| E | `SkillManifest`, registro confiável, exclusões e roteamento de especialista | `07c009b` |
| F | Workflows com versão, estado persistente, retry limitado e idempotência | `693b20a` |
| G | Catálogo dinâmico, health, quota, score e waterfall de fallback | `d758c84` |
| H | Executor Sandbox allowlisted, timeout, artefatos, QA gate e fluxo E2E de entrega | `9ba00d2` |

Todos os commits foram enviados para `origin/main`. A suíte de testes em `tests/` valida as invariantes de segurança, persistência, roteamento, retry, fallback e E2E.

## Limites mantidos

O Sandbox não chama IA e só executa comandos explicitamente allowlisted. Catálogos e Skills não concedem autorização. Secrets são representados por referências e redigidos nos eventos. A entrega só acontece depois da validação do QA gate. O executor real foi liberado apenas depois dos componentes P0–P2 desta implementação estarem presentes.
