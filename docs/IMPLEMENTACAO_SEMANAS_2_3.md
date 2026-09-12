# Implementação das semanas 2 e 3 — Sandbox Mobile

## Objetivo

As semanas 2 e 3 transformam a tela de validação da Fase 0 em um catálogo utilizável de **plugins e ferramentas**, com instalação exclusivamente dentro do RootFS Ubuntu do Sandbox. O Android host continua responsável apenas pela interface e pelo ciclo de vida do runtime; nenhum pacote é instalado no sistema Android.

## Semana 2 — Catálogo e experiência de instalação

A semana 2 adiciona um catálogo interno de plugins opcionais. O catálogo atual inclui Ollama, Android NDK, Trivy, SOPS, grpcurl e websocat. Os plugins externos baixam binários ARM64 ou o toolchain NDK somente quando solicitados; cada item possui identificador estável, versão, descrição, comando de instalação, comando de remoção e validação pós-instalação. O Ollama não é embutido em nenhum RootFS.

A interface Compose foi separada em abas de **Plugins** e **Ferramentas**. O usuário pode pesquisar por nome, descrição ou ID, filtrar somente componentes instalados e visualizar os estados **Não instalado**, **Instalando**, **Instalado**, **Falhou** e **Removendo**. Instalações e remoções são executadas em `Dispatchers.IO`, com indicador de progresso e mensagem de erro sem bloquear a interface.

As ferramentas que já vêm no RootFS não são duplicadas no catálogo. O RootFS
base usa Node.js 20 LTS, necessário para versões atuais de Vite, Vitest e
Playwright no Agent Extra. Os perfis são o base 0.3.3, o Agent Extra 0.4.1 em
preparação e o Agent Android/API 0.5.0; imagens de emulador permanecem fora do
produto mobile.

Antes da preparação do Sandbox, o catálogo continua consultável. As ações de instalação e remoção ficam desabilitadas até que o runtime esteja pronto, evitando qualquer tentativa de executar comandos fora da camada de isolamento.

## Semana 3 — Gerenciamento robusto

A semana 3 adiciona o ciclo de vida completo de componentes:

- resolução recursiva de dependências;
- detecção de dependências cíclicas;
- estado intermediário persistido antes de iniciar uma operação;
- validação antes de considerar um componente instalado;
- falha explícita quando uma dependência não é instalada;
- prevenção de remoção de componentes usados por outros componentes;
- estado `REMOVING` durante a remoção;
- preservação do diagnóstico quando uma remoção falha;
- registro de versão, timestamps, duração, usuário, dependências, erro e resultado da validação.

O estado principal passou a ser salvo em `components.json`, com migração automática do formato TSV legado. As gravações usam arquivo temporário e substituição atômica quando o filesystem oferece suporte; assim, a interrupção do processo durante uma instalação não deixa o arquivo principal truncado. O arquivo TSV anterior continua sendo lido somente para a migração de instalações existentes.

## Segurança e limites

A instalação monta um comando `apt-get` não interativo que é enviado ao executor do runtime. Como o executor protegido da Fase 0 é a única porta de execução, os limites de diretório de trabalho, timeout, comandos bloqueados e saída continuam centralizados na política do Sandbox. O catálogo não executa binários no Android host.

A importação e o workspace adicionados ao pacote mantêm proteção contra Zip Slip por meio de caminho canônico. As operações de Git e demais módulos permanecem atrás da mesma fachada `SandboxPlatform`, sem acesso direto ao processo Android.

## Validação

A suíte JVM cobre persistência JSON, migração do TSV, escape de strings, busca, filtros, instalação, estados de falha e dependências. O build Android produz um APK debug instalável por sideload. A validação de proot com RootFS real ainda depende de um device ou emulador ARM64, pois o sandbox de compilação não expõe um dispositivo Android físico.

## Arquivos principais

| Área | Arquivo |
|---|---|
| Catálogo e gerenciamento | `app/src/main/kotlin/com/sandbox/sandbox/PluginModels.kt` |
| Fachada e persistência | `app/src/main/kotlin/com/sandbox/sandbox/SandboxPlatform.kt` |
| Estado Android | `app/src/main/kotlin/com/sandbox/app/SandboxViewModel.kt` |
| Interface | `app/src/main/kotlin/com/sandbox/app/MainActivity.kt` e `PluginsScreen.kt` |
| Testes | `app/src/test/kotlin/com/sandbox/sandbox/` |
| Runtime Android | `android-module/src/main/kotlin/com/sandbox/` |
