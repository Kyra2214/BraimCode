# BrainCode — Legado e Decisões

## Objetivo

Impedir que documentação histórica seja confundida com arquitetura atual.

## Removido nesta limpeza

### Referência IaBrain

Foram removidos os snapshots copiados do IaBrain em `reference/iabrain/skills-tools/`, incluindo:

- catálogos JSON;
- schemas Room;
- AppDatabase/Entities/DAOs;
- resolvers e parser de comandos;
- IACapabilityRegistry original;
- ExecutionSecurityPolicy original;
- testes e validador específico.

Motivo: os conceitos úteis já foram adaptados para os contratos nativos do BrainCode. Manter o código original aumentava duplicação e dava aparência de integração que não existe.

### Arquitetura local LLM

O produto não usa um engine/LLM local baixado como cérebro do Chat. A documentação antiga que descrevia esse caminho não é válida para o estado atual.

### Agent autônomo

O modelo atual é Agent bounded. Um Agent não possui objetivo próprio nem LLM obrigatório. Ele executa uma missão delimitada pelas capabilities e pela policy.

## Preservado

Nenhum código atual do runtime foi removido nesta limpeza por estar simplesmente "órfão" ou por não aparecer no fluxo principal.

Componentes atuais somente devem ser removidos com confirmação específica ou quando forem claramente identificados como legado de uma arquitetura já descartada.

## Conceitos externos adotados

Projetos externos serviram como inspiração para:

- capability registry/discovery;
- policy e governança;
- agentes reutilizáveis e bounded;
- skills;
- conhecimento persistente/proveniência;
- workflows e jobs.

Nenhum projeto externo é dependência arquitetural do BrainCode.

## Regra para futuras auditorias

Classificar cada componente como:

- **ATUAL** — usado ou parte explícita do contrato atual;
- **PARCIAL** — existe, mas falta wiring/evidência;
- **LEGADO** — pertence a uma arquitetura descartada;
- **REFERÊNCIA** — documentação externa sem papel no runtime;
- **DÚVIDA** — não remover sem confirmação.

Somente **LEGADO** pode ser removido automaticamente nesta classe de limpeza. **DÚVIDA** exige confirmação.
