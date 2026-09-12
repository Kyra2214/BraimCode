# Auditoria aprofundada — nikilster/clawflows

**Snapshot:** `f1e4094752b0359c7a3089720457a536a4ae2813` · **Arquivos:** 182 · **Fonte:** https://github.com/nikilster/clawflows

## Conclusão
Clawflows é uma camada de workflows orientada a arquivos. A definição é `WORKFLOW.md` com frontmatter YAML e corpo em linguagem natural; o executor é um CLI Bash; ativação usa symlink; execução e persistência ficam separadas do catálogo. É simples, legível e adequado como formato inicial para tarefas persistentes do Brain, mas o Brain deve substituir symlinks frágeis por estado explícito e adicionar idempotência, lock e políticas.

## Evidência direta
`README.md` declara 113 workflows. `workflows/available/community/` contém workflows como `process-email`, `check-calendar`, `review-prs`, `update-clawflows` e `build-nightly-project`. `workflows/custom/` contém `track-doordash-delivery`, `track-time` e `update-timezone`; `workflows/enabled/.gitkeep` representa a ativação. `community-submissions/_template/WORKFLOW.md` é o template. `tests/core/` cobre `create`, `enable`, `disable`, `list`, `run`, `logs`, `backup`, `restore`, `import` e `workspace_detection`; `tests/edge_cases/` cobre YAML inválido, nomes ausentes e symlinks quebrados. `system/cli/clawflows` concentra a lógica e os scripts de instalação/importação/compartilhamento.

## Modelo de execução
O catálogo é somente leitura; customizações são locais; habilitar cria link em `enabled`; executar lê frontmatter, resolve schedule e passa o corpo para o agente; logs ficam em `system/runs`, fora do conteúdo versionado; backup/restore transporta estado. O workflow `update-clawflows` demonstra autoatualização do próprio catálogo.

## Absorção no Brain
Adotar `WORKFLOW.md` como formato humano e `WorkflowManifest` como representação parseada. Separar `available`, `custom`, `enabled` e `runs`, mas registrar ativação em banco/arquivo de estado com hash do conteúdo. Criar scheduler com timezone, próxima execução, retry, timeout, lock, idempotency key e política de aprovação. O agente pode criar workflow, mas a ativação e ações destrutivas devem exigir validação.

## Riscos
Linguagem natural não é uma DSL determinística; o mesmo workflow pode variar por modelo. Symlink não fornece autorização. Agendamentos que tratam e-mail, finanças, saúde ou mensagens exigem escopo e confirmação. Catálogo comunitário precisa de pinagem, revisão e assinatura.

## Veredito
**Absorver o formato, separação catálogo/estado e suíte de casos de borda. Não absorver o CLI monolítico inteiro.**

## Referências
[1]: https://github.com/nikilster/clawflows "Clawflows"
[2]: https://github.com/nikilster/clawflows/tree/main/tests "Clawflows tests"
