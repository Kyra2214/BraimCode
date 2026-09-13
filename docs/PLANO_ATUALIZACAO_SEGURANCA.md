# Plano de Atualização e Remediação de Segurança — BrainCode

**Status:** Planejamento aprovado
**Base:** auditoria completa do estado atual do BrainCode
**Objetivo:** fechar os bloqueadores de segurança antes de adicionar novas superfícies de execução.

## Princípios

Cada item só pode ser considerado concluído quando houver:

1. correção de código;
2. teste que falha no comportamento antigo e passa no novo;
3. validação do chamador real;
4. evidência observável da execução;
5. documentação coerente com o código.

Não fechar itens apenas com mocks quando o risco depende do comportamento real do Android/ARM64.

---

## Fase 0 — Infraestrutura de validação

### 0.1 Harness ARM64

Disponibilizar validação em dispositivo/emulador ARM64 no CI ou, enquanto isso, manter procedimento manual documentado no dispositivo real.

### 0.2 Attack probes versionados

Criar `tests/attack-probes/` com probes controlados para:

- fork bomb controlada;
- `dd`/`mount` através de `bash -c`;
- `exec 3<>/dev/tcp/...`;
- DNS rebinding;
- truncamento do EventStore.

### 0.3 Gate arquitetural

Criar verificação de CI que impeça novas chamadas diretas a `.execute(`, `.launch(` e `ProcessBuilder(` fora dos chamadores explicitamente aprovados.

---

# Fase 1 — Bloqueadores P0

## 1.1 Corrigir inversão de `networkAllowed`

Arquivo principal: `android-module/src/main/kotlin/com/sandbox/runtime/ProotProcessLauncher.kt`

Comportamento obrigatório:

- `networkAllowed=true`: não adicionar `unshare -n`; executar proot normalmente com rede disponível.
- `networkAllowed=false`: localizar `unshare` e executar com `unshare -n --` antes do proot/comando.
- se `unshare` não existir quando a rede estiver proibida: falhar fechado com exceção explícita.
- nunca executar silenciosamente sem isolamento quando a política exige isolamento.

Testes obrigatórios:

- `networkAllowed=true` não contém `unshare` nem `-n`;
- `networkAllowed=false` contém `unshare -n --` na ordem correta;
- ausência de `unshare` com rede proibida resulta em fail-closed;
- quando possível, validar conectividade real em dispositivo ARM64.

## 1.2 Garantir encerramento da árvore de processos

Implementar fallback real para encerramento da árvore quando `setsid` não estiver disponível.

**Nota:** qualquer uso de `ProcessHandle.descendants()` precisa ser previamente verificado quanto à disponibilidade/compatibilidade real no Android. Não implementar baseado apenas na documentação existente.

Adicionar teste com o launcher real, não apenas fake launcher.

## 1.3 Limitar criação de processos

Ativar `maxProcesses` com desenho compatível com Android. Se `RLIMIT_NPROC` não for confiável devido ao UID compartilhado do Android, implementar watchdog/estratégia equivalente.

Adicionar probe controlado de fork bomb e comprovar encerramento seguro.

## 1.4 Eliminar bypass do `SecureCommandExecutor`

Remover a segurança baseada apenas em `command.first()`/denylist.

O caso crítico é evitar que comandos proibidos sejam escondidos dentro de `bash -c`.

A solução preferencial é encaminhar execução para capacidades autorizadas em vez de confiar em shell livre.

## 1.5 Unificar superfícies de execução

Encaminhar git, toolchains, TestLab, diagnostics e plugins pelo caminho de autorização baseado em `CapabilityResolver`/`PolicyBroker`.

Ordem sugerida:

1. git;
2. toolchains;
3. TestLab/diagnostics;
4. plugins.

Nenhuma nova superfície direta deve ser adicionada antes desse fechamento.

## 1.6 Decisão sobre terminal

Escolher uma das opções:

- remover terminal livre da release;
- manter terminal somente em build/debug;
- ou reestruturar como capacidade de alto risco, por exemplo `terminal.raw`, passando por `PolicyBroker`, aprovação, orçamento e expiração.

### Critério de saída da Fase 1

Os probes de rede, fork bomb e `bash -c` devem passar em dispositivo/emulador ARM64 real.

---

# Fase 2 — P1

## 2.1 Assinatura Ed25519 do RootFS

Ativar a cadeia de confiança criptográfica do RootFS:

- provisionar chave pública fora do pipeline de build do aplicativo;
- assinar os manifests;
- exigir assinatura válida;
- passar o verificador para todas as instâncias de `SandboxResourceManager`.

SHA-256 continua sendo usado para integridade do artefato, mas não substitui autenticidade criptográfica.

## 2.2 Assinaturas reais para SkillManifest

Substituir o modelo de confiança baseado em campos autodeclarados por assinatura criptográfica verificável.

## 2.3 Persistência de revogação de skills

A revogação não deve desaparecer após reinicialização do aplicativo.

## 2.4 Corrigir validação de rede/SSRF

Fortalecer `NetworkPolicy` para validar o destino real da conexão, cobrindo:

- IPv4 privado;
- loopback;
- link-local;
- IPv6;
- IPv4-mapped IPv6;
- DNS rebinding.

## 2.5 Aplicar a mesma proteção ao RemotePluginCatalog

Não permitir bypass das regras de destino através do catálogo remoto.

## 2.6 Auditoria documental

Após a Fase 1, revisar a documentação de isolamento e segurança para que nenhuma capacidade seja descrita como mais forte do que a implementação real.

Em especial, proot não deve ser apresentado como equivalente a um jail/kernel sandbox forte quando namespaces/mount isolation reais não estiverem disponíveis.

---

# Fase 3 — P2

## 3.1 Rotação do EventStore

Projetar rotação/compactação preservando continuidade da cadeia de hashes.

## 3.2 Checkpoint externo contra truncamento

Adicionar mecanismo externo de checkpoint para detectar truncamento do final do EventStore.

## 3.3 Lease/fencing de workflows

Portar para Kotlin/Android antes de habilitar execução distribuída, workers remotos ou aprovação remota.

## 3.4 Armazenamento criptografado de credenciais

Substituir `SharedPreferences` em texto claro por armazenamento seguro apropriado.

## 3.5 Quota agregada de disco

Adicionar limite agregado do workspace; `RLIMIT_FSIZE` por arquivo não é equivalente a quota total.

---

# Dependências

```text
Fase 0
  └── Fase 1
       ├── 1.1 → 1.4 → 1.5
       └── 1.2 → 1.3
              ↓
           Fase 2
           ├── 2.1 → 2.2
           ├── 2.3 (paralelo)
           └── 2.4 → 2.5
              ↓
           Fase 3
           ├── 3.1 → 3.2
           ├── 3.3 (paralelo)
           ├── 3.4 (paralelo)
           └── 3.5 (paralelo)
```

# Definition of Done

Um item somente pode ser marcado como concluído quando houver:

- código alterado;
- teste demonstrando falha no comportamento antigo e sucesso no novo;
- validação em dispositivo real quando o risco depender de Android/ARM64/isolation;
- evidência observável do resultado;
- documentação atualizada e coerente.

# Regra operacional

A execução do plano será incremental: **um teste/tarefa por vez**. Nenhuma etapa posterior deve ser tratada como concluída enquanto a etapa atual não tiver evidência suficiente.

## Primeiro item autorizado

**1.1 — Corrigir a inversão de `networkAllowed` em `ProotProcessLauncher.kt`.**

Antes de alterar código, registrar os argumentos atuais para `true` e `false`, os argumentos esperados após a correção e como o teste capturará esses argumentos sem mascarar a regra de negócio.
