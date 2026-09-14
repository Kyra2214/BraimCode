# Brain Local — especificação de produto e release 0.6

**Status:** REGISTRADO — planejamento / não implementar até a consolidação e os testes atuais passarem
**Data:** 2026-09-14
**Repositório:** `Kyra2214/BrainCode`

## 1. Decisão de produto

O modelo de linguagem local faz parte do **Brain**. Para o usuário, ele não deve aparecer como uma LLM independente nem como uma ferramenta técnica separada.

A identidade apresentada ao usuário será:

- **Brain** — identidade principal da inteligência do aplicativo;
- **Brain Local** — identidade da capacidade de inferência local quando o processamento ocorrer no aparelho.

O usuário conversa exclusivamente com o Brain. O usuário não escolhe nem chama diretamente o motor de inferência, o modelo, o GGUF, o `llama-cli` ou o Sandbox.

## 2. Fase atual: teste

Durante a consolidação e homologação, o runtime/modelo local pode continuar sendo baixado separadamente. Isso existe apenas para facilitar os testes de:

```text
Chat
 ↓
Brain
 ↓
Router / decisão
 ↓
Brain Local
 ↓
Sandbox
 ↓
motor de inferência + modelo
 ↓
resposta
 ↓
Critic / Memory
```

O download separado nesta fase é uma decisão de engenharia temporária, não uma decisão de produto final.

## 3. Regra arquitetural obrigatória

A UI **não pode** chamar diretamente o runtime local.

Correto:

```text
Usuário → Brain → decide API ou Brain Local → execução → resposta
```

Incorreto:

```text
Usuário → llama.cpp
Usuário → modelo GGUF
Usuário → Sandbox
Usuário → API de provedor
```

A mesma regra vale para testes funcionais: um teste de LLM deve validar o caminho através do Brain, não criar um segundo caminho privilegiado na UI.

## 4. Fallback local

Quando não houver uma API gratuita utilizável, o Brain deve poder selecionar o Brain Local:

```text
Brain
 ↓
Memória
 ↓
API gratuita disponível?
 ├─ SIM → Provider → resposta → Critic
 │
 └─ NÃO → Brain Local → Sandbox → resposta → Critic
```

A ausência de chaves de APIs gratuitas não deve, por si só, impedir o Brain de responder quando o Brain Local estiver instalado e apto.

## 5. Release 0.6

Após os testes e a homologação do runtime local, o Brain Local será tratado como componente do release **0.6**.

A intenção é que a instalação normal do BrainCode possa entregar os componentes necessários sem exigir que o usuário saiba que está instalando uma LLM específica.

Conceitualmente:

```text
BrainCode 0.6
├── Brain Core
├── Router / Policy
├── Memory / Knowledge
├── Critic
├── Sandbox
├── APIs gratuitas
└── Brain Local
    └── runtime + modelo padrão interno
```

## 6. Abstração sobre o modelo

O nome e a implementação do modelo são detalhes internos.

O usuário **não precisa saber**:

- qual modelo foi escolhido;
- qual arquivo GGUF foi baixado;
- qual versão do motor de inferência é usada;
- qual URL fornece o artefato;
- qual biblioteca executa a inferência;
- onde o modelo fica armazenado no Sandbox.

Essas informações continuam registradas internamente para diagnóstico, segurança, atualização, integridade e auditoria técnica.

## 7. Independência do modelo

O Brain Local não deve ser conceitualmente acoplado a um modelo específico.

Se no futuro o modelo padrão for substituído por outro modelo mais eficiente ou capaz, a identidade do produto permanece **Brain Local**.

```text
Brain Local
   │
   └── modelo interno do release
          ↓
       pode mudar
          ↓
   identidade continua Brain Local
```

Isso evita que uma implementação interna se torne parte do contrato de produto.

## 8. Regras de nomenclatura da UI

Evitar como identidade visível:

- `mini-LLM`;
- `llama.cpp`;
- `llama-cli`;
- `GGUF`;
- nome do modelo;
- `Local LLM` como produto independente.

Preferir:

- `Brain`;
- `Brain Local` quando for necessário indicar o modo de execução;
- mensagens de diagnóstico que descrevam a capacidade, sem expor detalhes de implementação ao usuário comum.

## 9. Critério para incorporar ao release 0.6

O Brain Local só passa de componente experimental para componente de release depois de:

1. instalação/download validado;
2. integridade do artefato validada;
3. runtime iniciado pelo Sandbox;
4. modelo carregado corretamente;
5. conversa iniciada pela UI através do Brain;
6. fallback API → local comprovado;
7. resposta local passando pelo fluxo de Critic/Memory definido;
8. testes automatizados cobrindo o caminho;
9. Android debug build aprovado no CI;
10. nenhuma chamada direta da UI ao executor local;
11. documentação e nomenclatura da UI alinhadas com a identidade Brain/Brain Local.

## 10. Regra de ouro

> **O modelo é infraestrutura. O Brain é o produto.**

O usuário conversa com o Brain. O Brain decide quem executa o trabalho.
