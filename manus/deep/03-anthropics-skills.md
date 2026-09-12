# Auditoria aprofundada — anthropics/skills

**Snapshot:** `34040c9c568585f6929bedeaad110ad08f079624` · **Arquivos:** 419 · **Fonte:** https://github.com/anthropics/skills

## Conclusão
O repositório implementa uma unidade de extensibilidade, não somente prompts. Cada Skill combina `SKILL.md`, scripts, referências, recursos e licença. `spec/` contém a especificação Agent Skills; `template/` fornece o esqueleto; `skills/` contém exemplos reais. A adoção correta no Brain é de formato, loader e contratos de execução, não de cópia indiscriminada do conteúdo.

## Evidência arquivo a arquivo
`README.md` define o carregamento dinâmico e frontmatter mínimo (`name` e `description`). `spec/` separa o padrão portável da implementação. `template/` mostra a criação inicial. `skills/claude-api/` contém `SKILL.md`, exemplos em Python, TypeScript, Go, Java, C#, PHP, Ruby e curl, além de referências de streaming, batches, files e tool use. `skills/docx/scripts/` contém `accept_changes.py`, `comment.py`, `merge_runs.py` e helpers OOXML; isso prova uma Skill com execução real. `skills/pptx`, `xlsx` e `pdf` incluem schemas OOXML e scripts. `skills/skill-creator/agents/` contém `analyzer.md`, `comparator.md`, `grader.md` e `references/schemas.md`. `.claude-plugin/marketplace.json` agrupa Skills em plugins.

## Fluxo operacional
O catálogo torna nome e descrição disponíveis; o roteador seleciona por intenção; o conteúdo completo entra no contexto somente quando necessário; referências são lidas de forma progressiva; scripts são chamados como ferramentas. O catálogo não é isolamento: no Brain, cada script precisa de `ToolPolicy`, timeout, filesystem permitido, rede e auditoria.

## Absorção no Brain
Adotar `skills/<slug>/SKILL.md`, `manifest.json`, `references/`, `scripts/`, `tests/` e `LICENSE`. Acrescentar `version`, `origin`, `required_tools`, `permissions`, `network`, `side_effects`, `input_schema`, `output_schema` e `trust_level`. Criar loader progressivo e validador de frontmatter. Criar Skill-creator com análise, comparação, geração e avaliação. Pinçar dependências e exigir licença por Skill.

## Riscos
Frontmatter não representa autorização; scripts são código não confiável; marketplace pode carregar conteúdo fora do escopo; as Skills de documentos são source-available, não devem ser redistribuídas sem revisão. A instalação via plugin não substitui verificação de hash e SBOM.

## Veredito
**P0:** formato, loader, schema e executor isolado. **P1:** Skill-creator e testes de contrato. **Não absorver:** Skills source-available ou dependências sem proveniência.

## Referências
[1]: https://github.com/anthropics/skills "Anthropic Skills"
[2]: https://agentskills.io "Agent Skills specification"
[3]: https://github.com/anthropics/skills/tree/main/skills/docx "Document skill scripts"
