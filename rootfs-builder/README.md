# Rootfs Builder — Sandbox Mobile

Este diretório contém tudo que é necessário para construir o rootfs
completo (**Ubuntu 24.04**) que será baixado e executado dentro do app via
`proot`.

> Trocamos de Alpine (v0.1.0) para Ubuntu (v0.2.0) — motivo registrado em
> `docs/roadmap-sandbox-fase0.md`, item 0.1: musl (Alpine) quebra
> compatibilidade com a maioria dos wheels binários do pip e com binários
> pré-compilados comuns do npm, forçando recompilar tudo do zero. Ubuntu
> usa glibc, igual ao ambiente de referência (runtime mobile da DeepSeek).

## Requisitos para rodar este build

- Docker instalado na máquina onde você for gerar o pacote (não precisa
  Docker dentro do celular — isso é só para *fabricar* o arquivo `.tar.gz`
  uma vez, na sua máquina de desenvolvimento).

## Como usar

```bash
cd rootfs-builder
./build.sh
# ou, para versionar diferente de 0.3.0:
VERSION=0.3.1 ./build.sh
# Em máquina x86 com Docker e binfmt/QEMU configurado, arm64 é o padrão:
PLATFORM=linux/arm64 ./build.sh
```

Isso vai:
1. Construir a imagem Docker definida no `Dockerfile` (Ubuntu 24.04 arm64 +
   bash, coreutils, git, curl, wget, nodejs, npm, python3, python3-pip,
   python3-venv, build-essential, sqlite3, jq, vim/nano, openssh-client,
   entre outros — ver lista completa no `Dockerfile`).
2. Exportar o filesystem resultante como
   `../output/rootfs-ubuntu-<versão>.tar.gz`.
3. Gerar o hash SHA-256 em `../output/rootfs-ubuntu-<versão>.tar.gz.sha256`.
4. Gerar `../output/rootfs_manifest.json` já preenchido (URL, tamanho,
   hash, distro) apontando para uma release em `Kyra2214/SandBox` —
   decisão de hospedagem registrada em `docs/roadmap-sandbox-fase0.md`,
   item 0.2.

## Resultado — release publicada

O `.tar.gz` gerado precisa ficar acessível por HTTP (nunca vai dentro do
APK). Optamos por GitHub Releases do próprio repositório do projeto
(https://github.com/Kyra2214/SandBox), porque o CDN deles já suporta o
header `Range`, que é o que `SandboxResourceManager` usa pra retomar
downloads interrompidos.

O build da versão `0.2.0` foi concluído e a release foi publicada em
`rootfs-v0.2.0`. O comando usado foi:

```bash
gh release create rootfs-v0.2.0 \
  ../output/rootfs-ubuntu-0.2.0.tar.gz \
  --repo Kyra2214/SandBox \
  --title "Rootfs 0.2.0 (Ubuntu 24.04)" \
  --notes "Rootfs completo (Ubuntu 24.04) para o Sandbox Mobile, arch arm64-v8a"
```

Esse comando é o procedimento para uma versão futura. Sem `gh` CLI instalado, o mesmo dá pra fazer pela interface web: Releases
→ "Draft a new release" → tag `rootfs-v0.2.0` → arrastar o `.tar.gz`
como asset → publicar.

O manifesto já foi copiado para `app/src/main/res/raw/rootfs_manifest.json`.
O hash SHA-256 é o mesmo padrão de validação de integridade usado no app
(baixa → confere hash → só então considera pronto pra uso).

O histórico `../output/rootfs-build-info.txt` já registra o SHA-256 e o
tamanho reais da release publicada.

## Ferramentas para agentes

A imagem 0.3.0 inclui lint e formatação Python (`ruff`, `black`, `mypy`), ESLint e Prettier globais, ShellCheck, ctags, GitHub CLI, Git LFS, tmux, fzf, entr, utilitários de rede (`net-tools`, `dnsutils`, `netcat-openbsd`, `socat`), `yq`, Pandoc, ImageMagick e Graphviz.

A imagem 0.3.1 acrescenta `pytest` e `bandit`, `yarn`, `pnpm` e `pipx`, `httpie`, `bat`, suporte a `.7z`/`.xz`/`.bz2`, além dos clientes `psql`, MySQL e Redis.

O RootFS base 0.3.3 remove o Ollama para manter o download enxuto; o plugin
`ollama` instala-o sob demanda. O arquivo `Dockerfile.agent-extra` e o script
`build-agent-extra.sh` geram o RootFS opcional 0.4.1 para agentes, com dados,
segurança, multimídia, depuração, testes avançados e comandos `sandbox-*`.
O builder base foi atualizado para Node.js 20 LTS via NodeSource, mantendo
compatibilidade com Vite, Vitest e Playwright.

O catálogo do app oferece ainda os plugins opcionais Android NDK, Trivy, SOPS,
grpcurl e websocat. Eles não são embutidos no RootFS: são baixados sob demanda,
validados e removíveis pelo usuário.

Para um agente via API, os comandos `sandbox-run`, `sandbox-test`,
`sandbox-build`, `sandbox-clean`, `sandbox-info`, `sandbox-diagnose` e
`sandbox-health` fornecem uma interface estável para execução, testes,
artefatos, logs e diagnóstico sem depender de detalhes do Ubuntu.

## Customizando o conteúdo do rootfs

Edite o `Dockerfile` e adicione/remova pacotes com `apt-get install`. Como
a base já é "completa" (Ubuntu), o cuidado agora é o oposto do Alpine: não
precisa economizar pacote por pacote, mas evite inflar demais o tamanho do
download (imagens de IA/ML pesadas, toolchains gigantes) sem necessidade
comprovada — isso é decisão de fase futura (Fase 1+), não da fundação.

## Próximo passo (Fase 0.2 e 0.3 do roadmap)

Este builder só resolve o item 0.1 do roadmap (`docs/roadmap-sandbox-fase0.md`).
O reteste manual em device real com a nova base continua sendo a próxima
verificação operacional, descrita em `output/rootfs-build-info.txt`.

## RootFS Agent Android/API 0.5.0

O terceiro perfil é construído sobre o Agent Extra 0.4.1 usando
`build-agent-android.sh`. Ele adiciona Android SDK, adb, aapt2, apksigner,
zipalign, gitleaks, unrar e integração de jobs e artefatos. Também não inclui
Ollama. NDK e imagens de emulador ficam fora para evitar um pacote
excessivamente pesado em telefones.

Os comandos adicionais são `sandbox-artifact`, `sandbox-job` e
`sandbox-health-android`, destinados a agentes que trabalham via API.
