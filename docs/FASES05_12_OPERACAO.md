# Fases 5–12 — Integração operacional

## Fase 5 — Quota persistente
`PersistentQuotaStore` mantém consumo, falhas, cooldown, latência e taxa de sucesso em SQLite, permitindo recuperação após reinício.

## Fase 6 — Fallback rigoroso
`strict_fallback` elimina candidatos sem a capability equivalente, contexto suficiente, orçamento, autorização, credencial ou qualidade mínima.

## Fase 7 — EventStore de produção
O EventStore já usa lock de processo, `fsync`, recuperação de linha parcial, rotação, retenção, hash chain e correlação. A subscrição por cursor permite consumidores incrementais.

## Fase 8 — Memory SQLite
A memória usa SQLite/WAL, transações, deduplicação, retenção, provenance, bloqueio de secrets/injection e busca por estratégia/contexto.

## Fase 9 — QA/correction
O correction loop repete somente até o limite configurado e mantém o mesmo identificador de passo; o diagnóstico decide se há nova tentativa.

## Fase 10 — Execução paralela
Ramos sem dependências são executados com limite de workers; resultados são reunidos na ordem original e ciclos de dependência falham fechado.

## Fase 11 — Isolamento forte
A sondagem distingue `unshare`, Bubblewrap e cgroups disponíveis. Jobs com isolamento obrigatório recusam execução quando o host não oferece os requisitos.

## Fase 12 — Delivery observável Kotlin
`ObservableDelivery` calcula SHA-256 dos artefatos e emite recibo com run/session/task/step/event, provider, latência e custo.
