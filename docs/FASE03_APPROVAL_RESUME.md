# Fase 3 — Retomada de aprovações

A retomada deve reutilizar o mesmo `run_id`, `task_id`, `step_id`, capability, provider, recurso e decisão de Policy que originaram o pedido. `ExecutionBinding` é imutável e seu digest é verificado antes da execução; qualquer alteração do plano ou do recurso autorizado resulta em `PermissionError`.

O estado do pedido continua persistido no `ApprovalStore`, e os eventos `ApprovalRequested`, `ApprovalGranted` e `ApprovalDenied` permitem reconstrução após reinício. A execução retomada não cria uma autorização genérica nova nem permite troca silenciosa de provider.
