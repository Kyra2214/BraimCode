# Viabilidade de um launcher equivalente ao bwrap no Android

**Item investigado:** Item 1 — launcher bwrap equivalente no Android.

**Conclusão:** não é seguro tratar um launcher baseado em `unshare(CLONE_NEWUSER)` como disponível ou como requisito obrigatório para o produto Android atual. A viabilidade é **condicional ao kernel, à política SELinux e ao fabricante**. A implementação deve permanecer fora do caminho padrão até existir uma matriz de validação em dispositivos ou emuladores Android representativos.

## Decisão técnica

O projeto não deve copiar o modelo desktop do `bwrap` diretamente para o Android. O Android já fornece um sandbox por aplicativo baseado em UID Linux próprio, processo separado e SELinux em modo enforcing. Esse sandbox é a base de segurança do aplicativo, mas não fornece automaticamente um novo mount namespace, PID namespace, user namespace ou rede isolada para cada comando executado pelo aplicativo [1] [2].

O `bubblewrap` depende de user namespaces para permitir operações de container sem privilégios. O projeto upstream informa que o modo setuid histórico foi removido. Portanto, quando o kernel ou a política do dispositivo não permitem user namespaces não privilegiados, não existe um fallback genérico equivalente dentro do próprio `bwrap` [3].

A criação de um user namespace exige suporte do kernel (`CONFIG_USER_NS`) e pode ser recusada por restrições do sistema. Mesmo quando a chamada é permitida, as capacidades obtidas dentro do namespace só governam recursos associados a esse namespace; elas não transformam o aplicativo em root fora dele [4].

## Evidência observada no repositório

O Android module já possui um `ProotProcessLauncher` acionado pela `AndroidSandboxFactory`. O runtime executa comandos através de `proot` e aplica limites de processo por `ulimit`. A interface `SandboxProcessLauncher` permite introduzir outro launcher sem alterar o `ManagedSandboxRuntime`, mas não existe atualmente um launcher Android baseado em namespaces.

O projeto já documenta que o isolamento por processo e UID do Android não substitui controles OS-level equivalentes ao hardening Python. Também documenta que testes de `proot`, lifecycle e limites ainda não equivalem à validação em dispositivo ou emulador Android.

O host desta investigação possui `/usr/bin/unshare` e um valor positivo em `/proc/sys/user/max_user_namespaces`, mas não possui `bwrap`, `adb` ou uma imagem Android disponível. Esse resultado só prova que o host Linux local tem alguns pré-requisitos; não prova compatibilidade com um kernel Android de fabricante.

## Matriz de dependências

| Camada | Necessidade | Situação | Consequência |
|---|---|---|---|
| Kernel | `CONFIG_USER_NS` e suporte a `CLONE_NEWUSER` | Não verificável no host local para o alvo Android | `unshare` pode falhar com `ENOSYS`, `EINVAL` ou `EPERM` |
| Política do dispositivo | Permissão efetiva para o app criar namespaces e montar a topologia desejada | Dependente de build, vendor e SELinux | O binário pode existir e ainda assim ser bloqueado |
| Mount namespace | Bind mounts, `/proc`, rootfs e layout privado | Requer combinação de namespace, capacidades e política | Um launcher parcial não deve ser anunciado como sandbox forte |
| PID namespace | Esconder processos externos e prover um PID 1 | Dependente do suporte e da sequência de criação | Pode não funcionar mesmo quando `CLONE_NEWUSER` funciona |
| Network namespace | Egress isolado ou loopback-only | Especialmente dependente do kernel/política | Não deve ser presumido pelo launcher |
| Execução do binário | `bwrap` compilado para ABI e arquitetura do dispositivo | Não existe artefato Android no projeto | Exige build, empacotamento, licenciamento e testes por ABI |
| Segurança | Política explícita para mounts, sockets, `/dev`, `/proc` e capacidades | Ainda não definida para Android | Um launcher permissivo pode aumentar a superfície de ataque |

## Alternativas avaliadas

| Alternativa | Viabilidade geral | Nível de isolamento | Decisão |
|---|---:|---:|---|
| `bwrap` com user namespaces não privilegiados | Condicional | Alto quando todos os namespaces e mounts funcionam | Não adotar como padrão; investigar por matriz de devices |
| `bwrap` setuid | Inadequado para o aplicativo | Alto, mas exige autoridade privilegiada | Não implementar; o modo setuid foi removido do upstream e seria incompatível com o modelo normal de distribuição Android |
| `proot` atual + sandbox UID/SELinux do app | Alta | Compatibilidade de filesystem, não isolamento OS-level completo | Manter como caminho padrão atual, com diagnóstico explícito de limites |
| Processo Android separado + limites + seccomp/política nativa | Condicional | Pode ser forte, mas exige implementação nativa e política de device | Avaliar somente como projeto separado após a matriz Android |
| Serviço privilegiado, root ou módulo do sistema | Fora do escopo | Potencialmente alto | Não usar em APK comum; requer autoridade, assinatura e coordenação externa |

## Recomendação de implementação futura

A próxima implementação, se aprovada como item separado, deve ser um **preflight de capabilities**, não um launcher bwrap completo. Esse preflight deve executar apenas chamadas não destrutivas e produzir um resultado estruturado para:

1. detectar ABI, versão do kernel e presença do binário;
2. testar a disponibilidade de `CLONE_NEWUSER` em um processo descartável;
3. testar separadamente mount, PID e network namespaces;
4. registrar `errno`, contexto SELinux e diagnóstico do dispositivo;
5. recusar fail-closed qualquer modo de isolamento que não confirme todas as propriedades exigidas;
6. manter `proot` como fallback de compatibilidade, sem classificá-lo como equivalente ao bwrap;
7. impedir que o fallback seja usado quando a policy exigir isolamento OS-level.

Não se deve adicionar um `BwrapProcessLauncher` antes desse preflight e de testes em pelo menos um emulador e um dispositivo Android ARM64. O launcher também exigiria uma política explícita de mounts, `/proc`, `/dev`, sockets, capabilities, sinais, arquivos temporários e limites de recursos.

## Resultado do item

O item foi **investigado e não implementado**, conforme o requisito de investigar a viabilidade antes de codificar. A decisão registrada é:

> **Não viável como capacidade garantida em APK Android comum; viável apenas como caminho opcional e detectado em runtime, condicionado ao kernel, SELinux, ABI e fabricante.**

O backlog seguinte deve ser um preflight Android de namespaces e uma matriz de compatibilidade. A implementação do launcher só deve começar se essa matriz demonstrar que o conjunto mínimo de propriedades é reproduzível sem root e sem permissões privilegiadas.

## Referências

[1]: https://developer.android.com/guide/components/fundamentals "Application fundamentals — Android Developers"
[2]: https://source.android.com/docs/security/features/selinux "Security-Enhanced Linux in Android — Android Open Source Project"
[3]: https://github.com/containers/bubblewrap "bubblewrap — containers/bubblewrap"
[4]: https://man7.org/linux/man-pages/man7/user_namespaces.7.html "user_namespaces(7) — Linux manual page"
