# Release `rootfs-agent-android-v0.5.0` — Agent Android/API

## Identidade

A release `rootfs-agent-android-v0.5.0` é o perfil **Agent Android/API** do Sandbox Mobile. Ela é construída sobre o Agent Extra 0.4.1 e adiciona a toolchain Android, ferramentas de segurança e comandos para integração de jobs e artefatos via API.

| Campo | Valor |
|---|---|
| Release | `rootfs-agent-android-v0.5.0` |
| Artefato | `rootfs-agent-android-0.5.0.tar.gz` |
| Base direta | Agent Extra `0.4.1` |
| Distribuição | Ubuntu 24.04 |
| Arquitetura | `arm64-v8a` |
| Tamanho | 1.764.603.872 bytes |
| SHA-256 | `374e795ce3caa8eaf4cbb58f39d591230918f5226ec525d0140b5f94ff472c09` |
| Ollama | `false`; permanece opcional |
| Repositório de distribuição atual | [Kyra2214/BrainCode](https://github.com/Kyra2214/BrainCode) |

## Conteúdo Android e segurança

A release instala Android SDK, platform-tools, build-tools, a plataforma Android 23 e utilitários relacionados. Os binários `adb`, `aapt2`, `apksigner`, `zipalign`, `sdkmanager` e `avdmanager` são expostos com nomes previsíveis em `/usr/local/bin` quando presentes no SDK empacotado. `ANDROID_HOME` e `ANDROID_SDK_ROOT` apontam para `/opt/android-sdk`.

Também são adicionados gitleaks, unrar-free, qpdf, rsync e socat. O diretório `/home/sandbox/android-projects` é criado e atribuído ao usuário `sandbox` para projetos Android. O perfil não inclui Android NDK nem imagens de emulador, uma decisão deliberada para controlar o tamanho do download e manter esses componentes como opção separada.

## Integração de jobs e artefatos

A camada copia os scripts `sandbox-artifact`, `sandbox-job` e `sandbox-health-android` para `/usr/local/bin`. Eles complementam os comandos gerais do perfil base e Agent Extra, permitindo que agentes via API trabalhem com execução delimitada, health checks e movimentação de artefatos dentro das convenções do Sandbox.

O RootFS Android/API não inclui Ollama. O catálogo do aplicativo pode instalar esse componente sob demanda, sem torná-lo uma dependência obrigatória do perfil Android.

## Cadeia de dependência e uso

A cadeia homologada é:

```text
rootfs-v0.3.3
    └── rootfs-agent-v0.4.1
            └── rootfs-agent-android-v0.5.0
```

O manifesto do perfil registra `baseRootfs: 0.4.1`, `arch: arm64-v8a`, `distro: ubuntu-24.04` e `ollamaIncluded: false`. Alterar qualquer camada sem gerar e validar uma nova versão quebraria a rastreabilidade da cadeia; a release atual deve ser tratada como artefato imutável homologado.

## Fonte e migração

Esta documentação foi consolidada a partir de `rootfs-builder/Dockerfile.agent-android`, `rootfs-builder/build-agent-android.sh`, `rootfs-builder/README.md`, `rootfs-builder/agent_android_manifest.json` e dos scripts de integração no repositório [Kyra2214/SandBox](https://github.com/Kyra2214/SandBox). O artefato foi migrado para o BrainCode sem rebuild, alteração interna ou recompressão.

- [Release no BrainCode](https://github.com/Kyra2214/BrainCode/releases/tag/rootfs-agent-android-v0.5.0)
- [Artefato no BrainCode](https://github.com/Kyra2214/BrainCode/releases/download/rootfs-agent-android-v0.5.0/rootfs-agent-android-0.5.0.tar.gz)
- [Sidecar SHA-256](https://github.com/Kyra2214/BrainCode/releases/download/rootfs-agent-android-v0.5.0/rootfs-agent-android-0.5.0.tar.gz.sha256)
- [Manifesto do perfil](../rootfs-builder/agent_android_manifest.json)
