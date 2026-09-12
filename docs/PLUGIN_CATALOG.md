# Catálogo remoto de plugins

## Objetivo

O `RemotePluginCatalog` é a fronteira de confiança entre um catálogo externo e o `PluginManager` do Sandbox Mobile. Ele recebe um snapshot já coletado pelo chamador, valida a origem e a integridade do artefato e devolve somente componentes aceitos. A classe não faz chamadas de rede, não instala pacotes e não executa comandos.

## Regras de aceitação

Um snapshot só pode ser importado quando sua fonte está na allowlist configurada, a URL do catálogo usa HTTPS sem credenciais, fragmentos ou endereços privados, o manifesto declara uma fonte oficial e o SHA-256 do conteúdo recebido corresponde ao hash declarado. Identificadores duplicados, fontes não confiáveis, hashes divergentes e URLs inseguras são rejeitados.

A API separa `RemoteCatalogFinding` por componente do resultado aceito. Isso permite que a UI ou uma camada de sincronização apresente o motivo de cada rejeição sem transformar dados externos em autoridade de instalação. O resultado aceito ainda precisa ser passado explicitamente ao `PluginManager`; importar um snapshot não instala nada.

## Fluxo esperado

1. Uma camada de transporte obtém um documento remoto usando as políticas de rede do ambiente.
2. Essa camada converte o documento em `RemoteCatalogSnapshot` e `RemoteComponentManifest`.
3. `RemotePluginCatalog.importSnapshot` valida fonte, HTTPS, duplicidade e SHA-256.
4. O chamador combina `components()` com uma política local e, somente após autorização, cria ou atualiza o catálogo do `PluginManager`.
5. A instalação continua sujeita às dependências, validação pós-instalação e persistência existentes.

## Estado da entrega

A camada e seus testes foram implementados. A validação automática do módulo `:app` permanece pendente neste ambiente porque o Android SDK não está instalado nem definido por `ANDROID_HOME` ou `local.properties`. A validação do módulo `:brain` continua independente e já passa com JDK 17. A etapa não deve ser considerada concluída até o teste `:app:test` ser executado em um ambiente com SDK Android configurado.
