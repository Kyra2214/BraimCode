# Correção da topbar e retorno da busca

A topbar foi reorganizada em duas linhas para evitar que as ações sejam comprimidas ou cortadas em telas Android estreitas. A primeira linha exibe `Tarefas`, `BrainCode` e a tagline. A segunda linha ocupa toda a largura e exibe explicitamente `Buscar`/`Fechar busca`, `Configurações` e o estado do Sandbox.

Enquanto a busca estiver aberta, o campo agora possui um botão visível `← Voltar`, além do toggle `Fechar busca` na topbar. O usuário não depende mais do botão físico ou gesto do Android para retornar à thread.

O botão `Configurações` chama a rota já existente para `SettingsScreen` e permanece independente do estado do Sandbox.
