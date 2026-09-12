# 13 — Assemou007/OFFPack

## Objetivo

Estudar cache offline de dependências npm para uma melhoria futura do Sandbox, não para o núcleo do Brain.

## O que o OFFPack faz

É um gerenciador npm offline em Go, sem dependências externas. Ele baixa packages e dependências transitivas para um cache local e depois instala sem rede.

Comandos principais:

- `cache`;
- `install`;
- `fetch-popular`;
- `get`;
- `update`;
- `self-install`;
- `help`.

## Arquitetura interessante

```text
~/.offpack/
  package/
    version/
      package.tgz
      manifest.json
```

O cache separa pacote e versão, armazenando também metadados/dependências.

## O que absorver no ecossistema Braim + Sandbox

### Cache de dependências

Antes de construir um projeto, o Brain pode pedir ao Sandbox:

> “Verifique se as dependências necessárias já estão no cache.”

Se estiverem, não precisa baixar novamente.

### Manifest

Cada pacote pode ter hash, versão, origem e dependências. Isso aumenta reprodutibilidade.

### Popular cache

A ideia de pré-carregar ferramentas populares combina com o RootFS do Sandbox. Em vez de instalar tudo repetidamente, o ambiente mantém caches preparados.

### Offline-first

O Sandbox já é pensado para trabalho local. OFFPack complementa isso com cache de dependências de projetos Node.

## O que não absorver

- implementação inteira agora;
- lista fixa de 700 packages como verdade permanente;
- lógica de npm dentro do Brain.

A função pertence ao Sandbox/Toolchain Manager.

## Licença

MIT.

## Prioridade

**BAIXA agora / ALTA depois da união Brain + Sandbox**, quando a camada de produtividade offline for refinada.

## Fonte

https://github.com/Assemou007/OFFPack

## Conclusão

OFFPack não ensina muito sobre o cérebro, mas pode economizar tempo e banda no ambiente de execução. Deve ser estudado para a futura camada de **Dependency Cache / Offline Toolchain** do Sandbox.
