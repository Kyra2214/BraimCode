# Release readiness — Fase 7

## Objetivo

Este documento define o preflight local e os gates externos necessários para considerar o Sandbox pronto para validação de produção. O preflight não substitui a execução em Android ARM64, a assinatura de produção ou a validação da infraestrutura host; ele impede que um artefato inconsistente avance para esses gates.

## Preflight automatizado

O script `scripts/validate-release-readiness.sh` verifica os três perfis RootFS publicados no BrainCode. Para cada manifesto, ele confirma que a URL usa HTTPS e aponta para um release do `Kyra2214/BrainCode`, compara o `Content-Length` publicado com `sizeBytes` e compara o SHA-256 do sidecar publicado com o valor do manifesto.

Execute a partir da raiz do repositório:

```bash
bash scripts/validate-release-readiness.sh
```

O script não baixa os tarballs completos nem reconstrói RootFS. Ele é um gate de distribuição e integridade, não um teste funcional do `proot`.

## Status consolidado

Os três releases RootFS já foram homologados e hardened no Sandbox de origem,
migrados byte-a-byte e aprovados pelo preflight de distribuição. Não existe
pendência de reconstrução ou re-homologação desses artefatos. Os bloqueios
restantes abaixo pertencem exclusivamente à implantação do app e do host:

- **Android/device:** SDK e JDK 17 já validados no host de build; `adb` e emulador ou device ARM64 continuam pendentes;
- **Assinatura:** autoridade de chaves e verificação do APK de produção;
- **Host:** cgroups, Bubblewrap, firewall/namespaces e serviços distribuídos.

Esses gates não são simulados como concluídos por validações locais do
repositório.

## Gates de implantação

| Gate | Evidência exigida | Estado neste ambiente |
|---|---|---|
| Build Android | `:brain:test`, `:android-module:test`, `:app:test` e `:app:assembleDebug` com JDK 17 e Android SDK configurados | **Aprovado no clone limpo; BUILD SUCCESSFUL** |
| Device/emulador ARM64 | `scripts/e2e-smoke.sh` executado via `adb` | Pendente: nenhum device/emulador conectado |
| RootFS/proot real | Preparar sandbox, extrair RootFS, executar `bash`, health check, reset e repetir para os perfis aplicáveis | **Homologado no Sandbox de origem**; os três artefatos foram migrados byte-a-byte e não serão reconstruídos. Revalidação no app/dispositivo local permanece um teste de implantação, não uma nova homologação do release |
| Ciclo de vida | Evidência de prepare, running, cancelamento, diagnóstico, reset e recuperação após interrupção | Pendente: depende do gate Android |
| Assinatura de release | APK assinado pela autoridade de chaves de produção e verificação com `apksigner` | Pendente: credenciais de assinatura não estão no repositório |
| Infraestrutura OS-level | cgroups graváveis, Bubblewrap/firewall/namespaces e serviços distribuídos validados no host | Pendente: dependência externa de implantação |

## Critério de fechamento

A Fase 7 só pode ser marcada como concluída quando o preflight local passar e cada gate de implantação tiver evidência reproduzível. A ausência de SDK, dispositivo, autoridade de assinatura ou infraestrutura externa deve permanecer registrada como bloqueio; não deve ser simulada como sucesso.

Os RootFS `0.3.3`, `0.4.1` e `0.5.0` permanecem artefatos imutáveis. Uma falha de validação deve investigar o runtime ou o ambiente de implantação, sem reconstruir silenciosamente os tarballs homologados.
