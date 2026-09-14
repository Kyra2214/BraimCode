# Isolamento real e limites de segurança

O BrainCode usa `proot` para compatibilidade de filesystem e execução de um rootfs sem privilégios. **proot não é equivalente a um jail de kernel forte**: ele não cria namespace de PID, namespace de mount, namespace de usuário, cgroup dedicado ou firewall por processo.

As garantias implementadas são mais estreitas: validação de capacidades, política de rede antes da execução, `unshare -n` quando disponível e exigido, limites `setrlimit` verificados, watchdog de processos por árvore, encerramento da árvore via grupo ou `/proc`, allowlists de comandos/capacidades e auditoria de eventos.

Quando `unshare` não existe, uma execução com rede proibida falha fechada; o aplicativo não continua silenciosamente com rede disponível. Quando o ambiente não fornece namespaces reais, a documentação e a UI não devem prometer isolamento de kernel, contenção de UID ou invisibilidade de processos.

A validação em dispositivo/emulador ARM64 continua sendo um gate separado. Testes JVM, inspeções estáticas e probes controlados demonstram contratos locais, mas não substituem a execução real no Android.
