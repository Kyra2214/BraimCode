# Item 3 — Bloqueio de rede no proot

## Resultado

`proot` não cria namespace de rede e não intercepta sockets. Portanto, não é correto afirmar que uma flag do proot bloqueia a rede dentro do guest.

O caminho Android agora propaga `ExecutionAuthorization.networkAllowed` até `ManagedSandboxRuntime` e `SandboxProcessLauncher`. O `ProotProcessLauncher` aplica a seguinte política:

- `networkAllowed = true`: inicia o proot normalmente;
- `networkAllowed = false`: envolve o processo com `unshare -n --`, quando o binário está disponível;
- sem `unshare` executável: falha fechado com erro explícito, em vez de executar com rede aberta fingindo isolamento.

A política de rede é aplicada por capacidade, a partir da autorização, e não por texto de comando.

## Limitação Android

`unshare(CLONE_NEWNET)` pode ser bloqueado por kernels Android de fabricante ou pelo sandbox do próprio app. Nessa situação, o runtime não oferece uma solução equivalente sem suporte do kernel/root; ele recusa a execução offline. Isso é uma mitigação segura, não uma promessa de isolamento universal.

A próxima evolução possível é um helper nativo assinado, instalado pelo próprio APK, que faça a tentativa de namespace e reporte claramente a falha. Mesmo esse helper não pode superar um kernel que não permita a operação.

## Estado

Implementação concluída no launcher, runtime e sessão do Agent. A validação Gradle deve ser executada em CI com JDK 17 e Android SDK; o ambiente atual não possui esses componentes.
