# Referência IaBrain: Skills, Tools, Commands e Capabilities

Esta pasta contém cópias preservadas, somente para auditoria e comparação arquitetural, do repositório privado `Kyra2214/IaBrain`. Nenhum arquivo desta área é integrado automaticamente ao runtime do BrainCode.

## Origem e snapshot

- Repositório de origem: `Kyra2214/IaBrain`
- Commit de origem auditado: `ac958d8` (`fix: extract proot native libraries for execution`)
- Repositório de destino: `Kyra2214/BrainCode`
- Caminho original preservado em cada grupo abaixo.

## Resultado estrutural

O IaBrain não possui um único arquivo chamado `tools.json` nem um catálogo isolado de Skills com esse nome. A representação encontrada é distribuída entre:

```text
comandos_catalogo.json
  -> ComandoRepository / Entities / DAOs
  -> RoomCommandResolver / TextoLivreCommandResolver
  -> SlashCommandParser
  -> IACapabilityRegistry
  -> ExecutionSecurityPolicy
```

O principal catálogo JSON de comandos encontrado é:

```text
app/src/main/assets/comandos_catalogo.json
```

Ele representa comandos com `id`, `nome` e `comando`, e é validado pelo script `scripts/validate_catalog.py`. O IaBrain também possui catálogos de IAs/APIs, mas não foi encontrado um catálogo JSON dedicado que represente diretamente uma entidade `Skill -> Tool -> Command` completa. A relação capability/policy é implementada principalmente em Kotlin e persistida em Room.

## Inventário copiado

| Destino | Origem no IaBrain | Tipo | Uso e observações |
|---|---|---|---|
| `assets/comandos_catalogo.json` | `app/src/main/assets/comandos_catalogo.json` | Catálogo JSON principal | Catálogo prioritário de comandos; contém comandos e metadados básicos. |
| `assets/ai_api_catalog.json` | `app/src/main/assets/ai_api_catalog.json` | Catálogo JSON de APIs | Catálogo de provedores/APIs de IA; relacionado ao roteamento, não a Tools genéricas. |
| `assets/ia_catalogo.json` | `app/src/main/assets/ia_catalogo.json` | Catálogo JSON | Catálogo geral de IAs. |
| `assets/ia_18_catalogo.json` | `app/src/main/assets/ia_18_catalogo.json` | Catálogo JSON | Catálogo de IAs da categoria 18+. |
| `assets/catalogo_schema.sql` | `app/src/main/assets/catalogo_schema.sql` | Schema SQL | Estrutura de persistência dos catálogos. |
| `implementation/brain/IACapabilityRegistry.kt` | `app/src/main/java/com/aibrain/app/brain/IACapabilityRegistry.kt` | Implementação | Registry persistente de capabilities associadas a IAs/comandos. |
| `implementation/brain/ExecutionSecurityPolicy.kt` | `app/src/main/java/com/aibrain/app/brain/ExecutionSecurityPolicy.kt` | Implementação de policy | Regras de segurança para execução de comandos. |
| `implementation/brain/RoomCommandResolver.kt` | `app/src/main/java/com/aibrain/app/brain/RoomCommandResolver.kt` | Implementação | Resolve comandos persistidos em Room. |
| `implementation/brain/TextoLivreCommandResolver.kt` | `app/src/main/java/com/aibrain/app/brain/TextoLivreCommandResolver.kt` | Implementação | Resolve texto livre contra comandos conhecidos. |
| `implementation/command/SlashCommandParser.kt` | `app/src/main/java/com/aibrain/app/command/SlashCommandParser.kt` | Implementação | Parser de comandos iniciados por slash. |
| `implementation/data/ComandoRepository.kt` | `app/src/main/java/com/aibrain/app/data/local/ComandoRepository.kt` | Implementação de dados | Repositório de comandos persistidos. |
| `implementation/data/Entities.kt` | `app/src/main/java/com/aibrain/app/data/local/Entities.kt` | Modelo de dados | Entidades Room do catálogo/grafo de comandos. |
| `implementation/data/Daos.kt` | `app/src/main/java/com/aibrain/app/data/local/Daos.kt` | Persistência | DAOs Room usados pelos comandos e capabilities. |
| `implementation/data/AppDatabase.kt` | `app/src/main/java/com/aibrain/app/data/local/AppDatabase.kt` | Persistência | Banco Room e versão do modelo de dados. |
| `schemas/5.json` | `app/schemas/com.aibrain.app.data.local.AppDatabase/5.json` | Schema Room | Snapshot histórico associado à introdução do grafo de capabilities. |
| `schemas/6.json` | `app/schemas/com.aibrain.app.data.local.AppDatabase/6.json` | Schema Room | Snapshot associado ao registry persistente de capabilities. |
| `schemas/7.json` | `app/schemas/com.aibrain.app.data.local.AppDatabase/7.json` | Schema Room | Evolução posterior do modelo. |
| `schemas/8.json` | `app/schemas/com.aibrain.app.data.local.AppDatabase/8.json` | Schema Room | Snapshot mais recente disponível no checkout auditado. |
| `scripts/validate_catalog.py` | `scripts/validate_catalog.py` | Validador/schema executável | Valida catálogos de IAs e comandos com regras distintas. |
| `tests/ExecutionSecurityPolicyTest.kt` | `app/src/test/java/com/aibrain/app/brain/ExecutionSecurityPolicyTest.kt` | Teste | Evidência executável das regras de segurança. |
| `tests/TextoLivreCommandResolverTest.kt` | `app/src/test/java/com/aibrain/app/brain/TextoLivreCommandResolverTest.kt` | Teste | Evidência do comportamento de resolução de texto livre. |

## Dependências e compatibilidade

Os arquivos Kotlin foram preservados com os seus packages originais `com.aibrain.app.*`. Eles dependem de classes, entidades Room, configurações Gradle e contratos que existem no IaBrain e, portanto, não devem ser compilados diretamente como parte do BrainCode sem adaptação.

Os JSONs de catálogo não dependem de código para serem lidos, mas possuem formatos próprios. `comandos_catalogo.json` usa `version` numérico e uma lista `commands`; os catálogos de IAs usam `versao` e `ias`. O script de validação trata deliberadamente esses formatos separadamente.

Os schemas Room são históricos e servem para comparação de persistência. Não devem ser aplicados como migration no BrainCode sem mapeamento explícito das entidades e versões.

## Reutilização direta versus adaptação

Podem ser reutilizados diretamente como material de referência ou fixtures, sem execução:

- `assets/comandos_catalogo.json`;
- `assets/ai_api_catalog.json`;
- `assets/ia_catalogo.json`;
- `assets/ia_18_catalogo.json`;
- `assets/catalogo_schema.sql`;
- `scripts/validate_catalog.py`, desde que executado isoladamente sobre cópias ou catálogos compatíveis.

Precisam de adaptação antes de qualquer uso no BrainCode:

- `IACapabilityRegistry.kt`;
- `ExecutionSecurityPolicy.kt`;
- resolvers e parser de comandos;
- entidades, DAOs, repository e `AppDatabase`;
- schemas Room;
- testes Kotlin que dependem dos packages e modelos do IaBrain.

A adaptação deve mapear explicitamente os modelos do IaBrain para `SkillRegistry`, `CapabilityResolver`, `PolicyBroker`, `AgentSandboxSession` e os catálogos já existentes no BrainCode. Nenhum desses mapeamentos foi realizado nesta etapa.

## Duplicidades e versões

O BrainCode já possui catálogos próprios em `app/src/main/assets` e `brain/src/main/resources/catalog`, incluindo `comandos_catalogo.json`, `skills_catalog.json`, `skill_capabilities_catalog.json`, `capability_catalog.json`, `policy_catalog.json` e schemas de manifesto. Por isso, os artefatos do IaBrain foram colocados exclusivamente em `reference/iabrain/skills-tools/` e não sobrescrevem arquivos existentes.

As versões do IaBrain foram mantidas como snapshots de origem, especialmente os schemas Room `5` a `8`. Não foi escolhido arbitrariamente um catálogo para substituir o BrainCode.

## Limites desta etapa

- O IaBrain não foi alterado.
- O runtime do BrainCode não importa esta pasta.
- Nenhuma Skill, Tool, Command ou Capability foi registrada automaticamente.
- Nenhuma policy do IaBrain foi aplicada ao sandbox.
- A consolidação arquitetural deverá ocorrer em etapa posterior, com comparação de contratos, permissões, evidências e lifecycle.

## Hashes

Os hashes SHA-256 dos arquivos copiados estão disponíveis no histórico do commit que adiciona esta referência e podem ser regenerados com:

```bash
find reference/iabrain/skills-tools -type f -print0 | sort -z | xargs -0 sha256sum
```

## Correção funcional publicada junto

No mesmo commit, separadamente desta área de referência, é publicada a correção do transporte de RootFS que permite redirects HTTPS limitados para GitHub Releases. Essa correção está em `android-module/src/main/kotlin/com/sandbox/resource/SandboxResourceManager.kt`, com testes em `android-module/src/test/java/com/sandbox/resource/SandboxResourceTransportTest.kt`.
