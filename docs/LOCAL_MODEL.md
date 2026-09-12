# Mini-LLM local

O aplicativo agora oferece o download opcional da **SmolLM2 135M Instruct**, quantizada em **GGUF Q4_K_M**. O modelo foi escolhido por ser pequeno para uso local, com aproximadamente 101 MiB, e por possuir licença Apache-2.0 declarada no repositório de origem.

## Download no aplicativo

A tela de validação mostra o cartão **Mini-LLM local**. O botão baixa o arquivo para o armazenamento privado do aplicativo, separado do RootFS, usando o mesmo gerenciador de recursos do Sandbox. O download suporta retomada via HTTP Range, grava em arquivo `.part` e só promove o arquivo depois de validar o SHA-256.

O manifesto está em `app/src/main/res/raw/local_model_manifest.json` e contém:

| Campo | Valor |
|---|---|
| Modelo | SmolLM2 135M Instruct |
| Quantização | Q4_K_M |
| Formato | GGUF |
| Tamanho | 105.454.432 bytes |
| Licença | Apache-2.0 |
| SHA-256 | `2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d` |
| Fonte | [Hugging Face](https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF) |

## Limite atual

Esta entrega adiciona **download, armazenamento, retomada e verificação** do modelo. O carregador de inferência ainda precisa ser conectado ao arquivo GGUF — por exemplo, por `llama.cpp`/`llama-server` empacotado ou por um plugin nativo autorizado. O modelo não é executado automaticamente após o download e nenhum processo externo é iniciado pela UI.

A integração futura deverá registrar o modelo no `LocalLLMSecretario`, impor limites de memória/threads, expor timeout e manter fallback para `KeywordSecretario` quando o runtime de inferência não estiver disponível.
