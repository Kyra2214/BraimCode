# Fase 2 — Recurso de Estudo: LEANN

## Referência

- Repositório: `StarTrail-org/LEANN`
- Projeto: LEANN — Low-Storage Vector Index / RAG Everything
- Licença: MIT
- Foco: busca semântica local, RAG de baixo armazenamento, memória/knowledge retrieval e integração com agentes.

## Por que estudar no SandBox / IaBrain

LEANN é especialmente relevante para a camada de conhecimento do ecossistema. A proposta é reduzir drasticamente o armazenamento necessário para índices vetoriais por meio de busca em grafo, pruning e recomputação seletiva de embeddings sob demanda, mantendo os dados locais.

O projeto demonstra uma direção interessante para:

- memória semântica local;
- RAG de documentos e código;
- busca sobre grandes volumes com baixo custo de armazenamento;
- indexação incremental/watch de fontes;
- recuperação contextual para agentes;
- execução privada sem depender de enviar a base de conhecimento para a nuvem;
- MCP como interface de busca para agentes.

## Capacidades relevantes

O README e a documentação do projeto apresentam suporte para documentos, código, histórico de navegador, e-mails, chats, memória de agentes e outras fontes. Há também chunking orientado por AST para código, permitindo preservar melhor a estrutura semântica de arquivos de programação.

A documentação atual também apresenta HNSW como backend padrão e DiskANN para conjuntos maiores, além de permitir trocar armazenamento por latência por meio da configuração de recomputação.

## Relação com a arquitetura

A ideia não é transformar LEANN em dependência obrigatória nem colocar LEANN como cérebro.

Fluxo conceitual:

`IaBrain → Knowledge Layer → busca semântica → Skill/experiência/docs/código → Agente → SandBox → resultado → memória/índice → aprendizado`

Dentro dessa arquitetura:

- **IaBrain** decide o que precisa ser recuperado e como usar o resultado.
- **LLM secretário/analista** pode ajudar a formular consultas, resumir resultados ou analisar contexto.
- **LEANN-like Semantic Index** seria uma capacidade de recuperação, não o orquestrador.
- **Skills** representam capacidades reutilizáveis que podem ser encontradas pela busca.
- **Prompt DB** pode ser indexado semanticamente.
- **Knowledge/Discovery DB** pode usar busca semântica para recuperar fontes e experiências anteriores.
- **Connector Catalog** pode ser consultado semanticamente para encontrar conectores relacionados a uma necessidade.
- **SandBox** continua sendo o ambiente de execução.

## LEANN + Anthropic Agent Skills

A combinação é particularmente interessante para estudo:

`Problema → IaBrain → busca semântica → Skill/experiência/documentação relevante → agente → SandBox → resultado → indexação/memória`

A busca semântica pode ajudar o IaBrain a descobrir Skills, experiências anteriores, documentação, código e estratégias sem carregar todo esse conhecimento no contexto do LLM.

## Código e memória

O suporte a AST-aware chunking merece atenção especial para o SandBox, porque permite pesquisar código por significado estrutural em vez de tratar todo arquivo apenas como texto. A documentação cita Python, Java, C#, TypeScript e JavaScript/JSX via parser TypeScript.

Isso pode futuramente alimentar:

- recuperação de funções/classes relevantes;
- análise de arquitetura;
- localização de código relacionado a uma tarefa;
- debugging contextual;
- seleção de arquivos antes de um agente executar uma mudança;
- memória técnica de projetos.

## Metadados

Outro ponto importante para estudo é o filtro por metadados. O projeto permite associar metadados aos chunks e restringir a busca por campos e operadores.

Para IaBrain isso pode evoluir para filtros como:

- projeto;
- agente;
- Skill;
- versão;
- linguagem;
- tipo de documento;
- data;
- confiabilidade;
- origem;
- custo;
- resultado anterior;
- sucesso/falha;
- permissões.

Isso é potencialmente útil para combinar busca semântica com memória estruturada.

## Aprendizado

LEANN não precisa ser o mecanismo de aprendizado do IaBrain. Ele pode ser uma infraestrutura de recuperação para o ciclo de aprendizado:

`Problema → Fonte → Estratégia → API → Skill → Prompt → Agente → Resultado`

Os resultados e experiências podem ser indexados para recuperação futura, enquanto o IaBrain mantém scores, confiança, histórico, custo, tempo, erros e decisões em memória estruturada.

## Pontos para testar posteriormente

1. Armazenamento real em dispositivo ARM64/Android/Linux.
2. Consumo de RAM durante construção e consulta.
3. Tempo de indexação com modelos pequenos locais.
4. Tempo de busca com recomputação habilitada/desabilitada.
5. HNSW versus DiskANN.
6. Qualidade de recuperação em documentação de projetos grandes.
7. AST chunking para repositórios Android/Java/TypeScript/Python.
8. Filtros de metadados combinados com busca semântica.
9. `watch` para atualização incremental.
10. Integração MCP e possível uso como Connector/Skill de conhecimento.
11. Persistência e recuperação após reinicialização do SandBox.
12. Custo de armazenamento versus custo computacional.

## Cautelas

Este é um recurso de estudo. Não assumir que os números de benchmark publicados serão reproduzidos no hardware do SandBox.

Também não adotar automaticamente o projeto inteiro, seu runtime, modelos ou integrações. A abordagem correta é:

`estudar → testar → medir → comparar → decidir → implementar nativamente quando fizer sentido`

## Princípio

**LEANN não é o cérebro. É uma referência para construir uma memória/busca semântica local extremamente eficiente, que permita ao IaBrain encontrar conhecimento sem carregar tudo no contexto do LLM.**
