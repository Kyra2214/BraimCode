# Addendum de Roadmap — Security Test Lab

## Posição no roadmap

O **Security Test Lab** entra como etapa posterior à estabilização do runtime e das fronteiras de execução, com foco em validação adversarial e regressão de segurança.

### P4 — Validação adversarial

```text
ProjectScanner
      ↓
Security Test Lab
      ↓
Attack Simulation
      ↓
Sandbox
      ↓
Policy / QA / Detection
      ↓
Evidence Engine
      ↓
Fix → Verify → Learn
      ↓
Regression Corpus
      ↓
ReadinessGate
```

## Integrações obrigatórias

- `ProjectScanner` fornece arquitetura, módulos, dependências, providers, APIs, testes e riscos.
- `PolicyBroker` autoriza cada capability necessária ao cenário.
- `Sandbox` fornece isolamento para execução adversarial.
- `EventStore` registra tentativas, decisões e resultados.
- `Evidence Engine` transforma observações em evidências verificáveis.
- `Fix → Verify → Learn` converte falhas corrigidas em conhecimento e testes de regressão.
- `ReadinessGate` considera os resultados de segurança antes da entrega.

## Critério de conclusão

A etapa não é considerada concluída por quantidade de testes. Deve existir uma bateria reproduzível capaz de:

1. selecionar cenários pelo perfil de risco;
2. executar apenas em alvos autorizados;
3. detectar e registrar tentativas de bypass;
4. preservar evidência sem expor secrets;
5. gerar falha quando uma defesa esperada não funcionar;
6. transformar vulnerabilidades corrigidas em regressões;
7. integrar o resultado ao ReadinessGate;
8. sobreviver a restart/replay sem perder a rastreabilidade do ataque.

## Regra arquitetural

O Lab é um **consumidor adversarial das interfaces do Braim**, não uma nova autoridade. Ele não pode contornar PolicyBroker, CredentialVault, Sandbox, EventStore ou ReadinessGate para executar um teste.

Por padrão, testes contra sistemas externos são bloqueados. O ambiente de teste do próprio projeto, fixtures, mocks e serviços locais controlados são os alvos padrão.

## Prioridade inicial

```text
1. Scanner/risco
2. Corpus de cenários
3. Executor controlado
4. Detection + Policy assertions
5. Evidence
6. Regression generation
7. Red Team → Blue Team
8. Integração final com Readiness
```
