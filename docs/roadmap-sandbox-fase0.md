# Roadmap Oficial — Sandbox Mobile

> Roadmap do Sandbox. A Fase 0 está concluída e congelada. Tudo que for desenvolvido daqui em diante pertence às fases seguintes.

---

## 🔒 FASE 0 — FUNDAÇÃO

**STATUS: FECHADA / CONGELADA**

A Fase 0 não receberá novas funcionalidades.

Inclui:

- RootFS Ubuntu 24.04
- Download do RootFS
- Extração e preparação
- Proot
- Runtime
- Execução de comandos
- Timeout
- Cancelamento
- Lifecycle do runtime
- Persistência de execução
- Logs e observabilidade
- Reset
- Tela de validação
- Empacotamento Android
- Testes da fundação

**Regra:** não adicionar funcionalidades nem alterar o comportamento funcional já validado da Fase 0.

---

# 🚀 FASE 1 — PLUGINS E FERRAMENTAS

**Objetivo:** criar o sistema de instalação e gerenciamento de componentes dentro do Sandbox.

### Tela Sandbox

Adicionar dois menus principais:

- **Plugins**
- **Ferramentas**

Cada item deverá apresentar:

- Nome
- Descrição
- Status
- Versão, quando disponível
- Botão **Instalar**
- Botão **Remover**

### Plugins iniciais

- Python Dev
- Node.js Dev
- Java/Gradle
- Android Build
- C/C++ Dev
- Rust
- Go

### Ferramentas iniciais

- ripgrep
- fd
- tree
- less
- file
- rsync
- patch
- diff
- sed
- awk
- procps
- psmisc
- htop
- lsof
- iproute2
- ferramentas de banco de dados
- outras ferramentas úteis

### Regra de instalação

Tudo que for instalado deve existir **dentro do Sandbox/RootFS**.

Nada de plugin executável no Android host.

O sistema deve detectar ferramentas que já fazem parte do RootFS e exibir **✓ Já instalado**, evitando instalações duplicadas.

---

# 🧩 FASE 2 — SISTEMA COMPLETO DE PLUGINS

**Objetivo:** transformar o gerenciamento de Plugins em um sistema robusto e extensível.

### Catálogo

Cada plugin deverá possuir:

- ID
- Nome
- Descrição
- Versão
- Dependências
- Instalação
- Remoção
- Validação
- Atualização

### Gerenciamento

- Instalar
- Remover
- Atualizar
- Verificar
- Resolver dependências
- Detectar conflitos
- Registrar instalações

### Segurança da instalação

- Validação pós-instalação
- Tratamento de erros
- Rollback quando possível
- Nunca marcar instalação incompleta como concluída

### Persistência

Registrar:

- Plugin instalado
- Versão
- Data da instalação
- Dependências
- Estado

---

# 🛠️ FASE 3 — AMBIENTE DE DESENVOLVIMENTO

**Objetivo:** transformar o Sandbox em um ambiente capaz de trabalhar com projetos reais.

### Projetos

- Criar projeto
- Importar projeto
- Abrir projeto
- Excluir projeto
- Listar projetos

### Workspace

Estrutura organizada para:

- Projetos
- Ferramentas
- Ambientes
- Arquivos temporários
- Configurações

### Terminal

- Execução de comandos
- Histórico
- stdout
- stderr
- Cancelamento
- Diretório de trabalho
- Processos em execução

### Git

- clone
- pull
- push
- branch
- checkout
- commit
- diff
- status

Tudo executado dentro do Sandbox.

---

# 🔐 FASE 4 — SEGURANÇA E ISOLAMENTO

**Objetivo:** tornar o Sandbox mais seguro e resistente a processos problemáticos.

### Controle

- Filesystem
- Processos
- Rede
- Comandos
- Diretórios

### Limites

- CPU
- Memória
- Armazenamento
- Tempo de execução
- Quantidade de processos

### Diagnóstico

- Processos ativos
- Processos órfãos
- Processos travados
- Falhas de runtime
- Eventos de segurança

### Recuperação

- Encerramento seguro
- Limpeza
- Recuperação de estado
- Tratamento de falhas

---

# ⚙️ FASE 5 — TOOLCHAINS E AMBIENTES AVANÇADOS

**Objetivo:** ampliar as capacidades de desenvolvimento sem inflar desnecessariamente o RootFS base.

### Android

- Android SDK
- platform-tools
- build-tools
- plataformas Android

### Java

- JDK
- Gradle
- Maven

### Python

- Ambientes virtuais
- Ferramentas de teste
- Lint
- Formatadores
- Build

### Web / Node.js

- TypeScript
- ESLint
- Prettier
- Ferramentas de testes
- Gerenciadores de pacotes

### C/C++

- CMake
- Ninja
- Toolchains

### Rust

- Rust
- Cargo

### Go

- Go toolchain

Novos ambientes poderão ser adicionados posteriormente pelo sistema de Plugins.

---

# 🌐 FASE 6 — REDE E SERVIÇOS

**Objetivo:** permitir que o Sandbox execute serviços completos.

Suporte para:

- FastAPI
- Flask
- Node.js
- Vite
- Servidores HTTP
- SQLite
- PostgreSQL
- Redis
- Outros serviços compatíveis

### Gerenciamento

- Iniciar
- Parar
- Reiniciar
- Verificar status
- Identificar portas
- Visualizar logs
- Detectar processos encerrados

---

# 🧪 FASE 7 — TEST LAB

**Objetivo:** criar um ambiente completo para testar projetos dentro do Sandbox.

### Entrada

- Projeto local
- ZIP
- Repositório Git

### Validação

- Instalação de dependências
- Build
- Testes unitários
- Testes de integração
- Lint
- Análise
- Execução de scripts

### Relatório

Mostrar:

- Testes executados
- Testes aprovados
- Testes falhados
- Erros
- Warnings
- Tempo de execução
- Resultado final

---

# 🏆 FASE 8 — SANDBOX 100% COMPLETO

**MARCO FINAL DO ROADMAP ATUAL**

O objetivo é entregar um Sandbox estável, completo e utilizável para desenvolvimento e execução de projetos reais.

### Critérios de conclusão

- RootFS estável
- Runtime estável
- Plugins funcionando
- Ferramentas funcionando
- Instalação e remoção confiáveis
- Gerenciamento de dependências
- Projetos
- Workspace
- Terminal
- Git
- Toolchains
- Serviços
- Rede
- Segurança
- Test Lab
- Logs
- Diagnóstico
- Recuperação
- Persistência
- Experiência de uso consistente

### Resultado esperado

```text
ANDROID
   │
   ▼
SANDBOX MOBILE
   │
   ├── Plugins
   ├── Ferramentas
   ├── Projetos
   ├── Workspace
   ├── Terminal
   ├── Git
   ├── Toolchains
   ├── Serviços
   ├── Rede
   ├── Segurança
   └── Test Lab
          │
          ▼
     ROOTFS UBUNTU
          │
          ▼
         PROOT
```

---

## 🎯 OBJETIVO DO PROJETO

Construir primeiro um **Sandbox completo, estável, seguro e utilizável sozinho**.

A Fase 0 permanece congelada.

O roadmap atual termina na **Fase 8 — Sandbox 100% Completo**.

Qualquer projeto futuro que utilize o Sandbox será definido separadamente, depois que esta etapa estiver concluída.
