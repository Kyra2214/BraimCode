# Release `rootfs-agent-v0.4.1` — Agent Extra

## Identidade

A release `rootfs-agent-v0.4.1` é o perfil **Agent Extra** do Sandbox Mobile. Ela é uma camada derivada do RootFS base 0.3.3 e adiciona ferramentas para depuração, segurança, dados, multimídia, testes avançados e operação de jobs de agentes.

| Campo | Valor |
|---|---|
| Release | `rootfs-agent-v0.4.1` |
| Artefato | `rootfs-agent-extra-0.4.1.tar.gz` |
| Base | `0.3.3` |
| Distribuição | Ubuntu 24.04 |
| Arquitetura | `arm64-v8a` |
| Tamanho | 1.727.218.090 bytes |
| SHA-256 | `ffe23b4bb326bfd9ef7548f6378d9ca5e6c75fce342ee7c7923203492cd4850b` |
| Ollama | Não incluído |
| Repositório de distribuição atual | [Kyra2214/BrainCode](https://github.com/Kyra2214/BrainCode) |

## Ferramentas adicionadas

O perfil instala utilitários de diagnóstico e segurança como Bats, ShellCheck, Valgrind, strace, ltrace, GDB, LLDB, binutils, OpenSSL e age. Também adiciona ferramentas de rede e observabilidade, incluindo ping, tcpdump, traceroute, mtr, nmap, whois, rclone, socat, parallel, btop, ncdu, watch e libarchive-tools. Para dados e documentos, inclui XML utilities, FFmpeg, ExifTool, Poppler, Ghostscript e qpdf.

A camada Python acrescenta `uv`, pip-tools, Poetry, coverage, tox, pre-commit, isort, flake8, pyright, maturin, Cython, Hypothesis, jsonschema e yamllint. Para dados e documentos, disponibiliza build, DuckDB, pandas, polars, NumPy, openpyxl, PyArrow, python-docx, pypdf e ReportLab. Também inclui semgrep, grpcio-tools e websockets.

No ecossistema JavaScript, fornece TypeScript, tsx, Vite, Vitest e Playwright globalmente. As versões locais dos projetos continuam podendo ser fixadas no `package.json`.

## Estrutura de trabalho

O perfil cria a estrutura padrão em `/home/sandbox`: `workspace`, `projects`, `jobs`, `artifacts`, `logs`, `cache`, `tools`, `scripts` e `tmp`. Os comandos de agente instalados no perfil base oferecem uma interface estável para execução, testes, builds, limpeza, informações, diagnóstico e health checks sem acoplar o agente aos detalhes internos do Ubuntu.

As ferramentas `grpcurl` e `websocat` não são embutidas nesta release; o projeto ou o catálogo de plugins deve instalá-las quando necessário. Android NDK, Trivy, SOPS e imagens de emulador permanecem fora deste perfil para evitar duplicação e crescimento excessivo do pacote.

## Cadeia de dependência

O Dockerfile do perfil usa `sandbox-rootfs-builder:0.3.3` como base. O perfil Android/API 0.5.0 é construído sobre `sandbox-rootfs-agent-extra:0.4.1`. Assim, o Agent Extra funciona como a camada intermediária entre o ambiente geral de desenvolvimento e a integração Android/API.

## Fonte e migração

Esta documentação foi consolidada a partir de `rootfs-builder/Dockerfile.agent-extra`, `rootfs-builder/build-agent-extra.sh`, `rootfs-builder/README.md`, `rootfs-builder/agent_extra_manifest.json` e dos scripts de agente no repositório [Kyra2214/SandBox](https://github.com/Kyra2214/SandBox). O artefato foi migrado para o BrainCode preservando bytes, tamanho e SHA-256.

- [Release no BrainCode](https://github.com/Kyra2214/BrainCode/releases/tag/rootfs-agent-v0.4.1)
- [Artefato no BrainCode](https://github.com/Kyra2214/BrainCode/releases/download/rootfs-agent-v0.4.1/rootfs-agent-extra-0.4.1.tar.gz)
- [Sidecar SHA-256](https://github.com/Kyra2214/BrainCode/releases/download/rootfs-agent-v0.4.1/rootfs-agent-extra-0.4.1.tar.gz.sha256)
- [Manifesto do perfil](../rootfs-builder/agent_extra_manifest.json)
