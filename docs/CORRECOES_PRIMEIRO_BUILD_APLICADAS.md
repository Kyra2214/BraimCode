# Correções do primeiro build real da UI

## Correções aplicadas

O card de status do Sandbox deixou de ser um evento misturado à thread e passou a ser renderizado como header fixo imediatamente abaixo da topbar. Isso mantém o estado `NotReady`, o botão **Preparar sandbox**, o progresso de `Downloading`, o estado `Preparing`, o botão **Teste geral** em `Ready` e o retry de estados bloqueados sempre visíveis no mesmo lugar.

A lista de eventos agora contém somente eventos da sessão, terminal, aprovações, relatórios e diagnósticos. Com isso, a thread pode ocupar o espaço disponível sem deixar o card de estado flutuando no meio do histórico.

Os chips de slash-command agora são renderizados apenas quando `SandboxPhase.Ready`. O campo do composer e o botão Executar já tinham a mesma proteção e permanecem inativos antes desse estado.

O botão **Config** já estava presente na topbar e a rota `SettingsScreen` já estava conectada em `SandboxMobileApp`; não foi necessário alterar essa parte.

## Validação

Foi executado `git diff --check` e a revisão confirmou que o fluxo existente de `prepareSandbox()`, progresso, self-check e navegação de Config continua sendo usado. A validação visual final deve ser feita no APK/CI com os cenários `NotReady → Downloading → Preparing → Ready` e com uma thread contendo múltiplos eventos reais.
