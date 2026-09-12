# Toolchains do Sandbox Mobile

## Escopo

`ToolchainProfile` descreve uma toolchain de forma declarativa: executável de detecção, argumentos de versão, pacotes permitidos e argumentos de validação. `ToolchainDetector` executa apenas a lista de argumentos definida pelo perfil e retorna um diagnóstico estruturado. Ele não aceita uma string de shell arbitrária.

`ToolchainDetector.planInstall` gera um plano explícito com `bash -c`, `apt-get update` e instalação dos pacotes allowlisted. O plano ainda não é executado automaticamente e não substitui a autorização da Policy nem os limites do Sandbox.

`ToolchainManager` executa esse plano somente por chamada explícita, persiste estados `INSTALLING`, `INSTALLED`, `FAILED`, `REMOVING` e `NOT_INSTALLED`, valida o executável após a instalação e remove apenas os pacotes declarados pelo perfil. Falhas ficam persistidas com diagnóstico para retry ou intervenção da Policy.

## Perfis incluídos

A base inicial cobre Java, Python, Node.js, C/C++, Rust e Go. Android permanece como tipo suportado pelo modelo, mas exige um perfil específico com SDK/NDK e licenças declaradas antes de ser habilitado no catálogo padrão.

## Segurança

Os IDs de perfis e pacotes aceitam somente caracteres de catálogo. O detector não concatena entrada de usuário no comando de detecção. A instalação deve ser realizada por uma camada superior que confira readiness do Sandbox, autorização, conectividade e disponibilidade de armazenamento antes de executar o plano.

## Validação

Foram adicionados testes para detecção bem-sucedida, diagnóstico de ausência, geração de plano com pacotes declarados e rejeição de metacaracteres em pacotes. A execução do módulo `:app` depende de Android SDK configurado; quando o SDK não estiver disponível, a etapa deve permanecer marcada como parcial no roadmap.
