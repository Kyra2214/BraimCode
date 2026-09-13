# Migração dos releases validados do SandBox

## Objetivo

O `SandBox` foi a primeira fase do projeto e seus RootFS já foram construídos,
validados e publicados. Como o Sandbox agora faz parte do subsistema de
execução do BrainCode, os artefatos validados passam a ser distribuídos pelo
repositório `Kyra2214/BrainCode`.

**Esta migração é copy-only. Nenhum RootFS deve ser reconstruído ou alterado.**

## Artefatos de origem

| Release SandBox | Arquivo | Tamanho | SHA-256 |
|---|---|---:|---|
| `rootfs-v0.3.3` | `rootfs-ubuntu-0.3.3.tar.gz` | 1,075,455,791 | `a43f0915d5cd6e2e0c8640b8b30855d54f7b8873ca3be95acca769100b1487f4` |
| `rootfs-agent-v0.4.1` | `rootfs-agent-extra-0.4.1.tar.gz` | 1,727,218,090 | `ffe23b4bb326bfd9ef7548f6378d9ca5e6c75fce342ee7c7923203492cd4850b` |
| `rootfs-agent-android-v0.5.0` | `rootfs-agent-android-0.5.0.tar.gz` | 1,764,603,872 | `374e795ce3caa8eaf4cbb58f39d591230918f5226ec525d0140b5f94ff472c09` |

Esses valores são os metadados dos releases publicados no SandBox e devem
ser tratados como referência de integridade da migração.

## Regra de preservação

A migração não cria uma nova build. Para cada artefato:

1. baixar o arquivo do release original do SandBox;
2. calcular o SHA-256 local;
3. comparar tamanho e SHA-256 com os valores acima;
4. publicar o mesmo arquivo no release correspondente do BrainCode;
5. verificar novamente o asset publicado;
6. somente depois apontar os manifests para o BrainCode.

Se tamanho ou SHA-256 divergirem, a migração deve parar. Não se deve fazer
rebuild para "corrigir" a divergência: deve-se investigar a cópia/origem.

## Mapeamento

A versão do artefato é preservada. A mudança é de repositório de distribuição:

```text
Kyra2214/SandBox
    rootfs-v0.3.3
        ↓ cópia byte-a-byte
Kyra2214/BrainCode
    rootfs-v0.3.3

Kyra2214/SandBox
    rootfs-agent-v0.4.1
        ↓ cópia byte-a-byte
Kyra2214/BrainCode
    rootfs-agent-v0.4.1

Kyra2214/SandBox
    rootfs-agent-android-v0.5.0
        ↓ cópia byte-a-byte
Kyra2214/BrainCode
    rootfs-agent-android-v0.5.0
```

Os releases antigos do SandBox permanecem como histórico/origem. Não são
apagados nem modificados durante esta migração.

## Automação

`rootfs-builder/migrate-sandbox-releases.sh` executa a migração usando o `gh`
CLI. O script:

- baixa somente os três assets conhecidos do SandBox;
- verifica tamanho e SHA-256 antes de publicar;
- cria ou atualiza os releases correspondentes no BrainCode;
- publica também os arquivos `.sha256`;
- atualiza os manifests do BrainCode para as URLs novas somente depois da
  publicação;
- nunca executa Docker e nunca reconstrói RootFS.

Execute a partir de um clone do BrainCode:

```bash
cd rootfs-builder
bash migrate-sandbox-releases.sh
```

É necessário estar autenticado no GitHub CLI e ter permissão de escrita no
repositório BrainCode.

## Pós-migração

Depois da execução:

```bash
git diff --check
git diff -- app/src/main/res/raw/rootfs_manifest.json rootfs-builder/agent_extra_manifest.json rootfs-builder/agent_android_manifest.json
```

Verifique que as URLs passaram de `Kyra2214/SandBox/releases/download/...` para
`Kyra2214/BrainCode/releases/download/...`, mantendo exatamente os mesmos
`tamanho` e `sha256`.

Também deve ser confirmado que cada release do BrainCode contém o `.tar.gz` e
o `.tar.gz.sha256` correspondentes.

## Decisão arquitetural

O Sandbox não é tratado como produto separado nesta arquitetura. Ele é o
ambiente de execução controlada do BrainCode:

```text
BrainCode
├── IaBrain
│   ├── Planner
│   ├── PolicyBroker
│   ├── Router
│   ├── Agents
│   ├── Skills
│   └── Memory
│
├── Execution
│   ├── ExecutionAuthorization
│   ├── SandboxDispatcher
│   └── Sandbox
│       ├── CapabilityResolver
│       ├── RootFS
│       ├── ProcessIsolation
│       ├── ResourceLimits
│       └── NetworkIsolation
│
└── Delivery
```

Portanto, a distribuição dos RootFS validados acompanha o BrainCode sem
reconstruir uma fase que já foi homologada.
