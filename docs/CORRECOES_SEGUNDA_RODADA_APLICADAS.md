# Correções da segunda rodada pós-build

A busca da thread agora funciona como toggle: o botão exibe **Buscar** quando fechada e **Fechar** enquanto o campo de busca está aberto. O segundo toque retorna à visualização normal da thread sem depender do botão físico ou do gesto de voltar do Android.

O acesso a **Config** permanece independente de `SandboxPhase` e foi mantido como ação separada na topbar. Assim, Provedores, Extensões, Workspace e Toolchains continuam acessíveis mesmo quando o RootFS ainda não foi preparado.

A tagline da topbar foi atualizada para **“Converse. Execute. Comprove.”**, com `maxLines = 1` para evitar quebra em telas menores.

O fluxo `NotReady → Downloading → Preparing → Ready` e a aparição do **Teste geral** em `Ready` continuam implementados no `StatusSection`; a confirmação visual final depende de executar o APK em um dispositivo/emulador com o download do RootFS configurado.
