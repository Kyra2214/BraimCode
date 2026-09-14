# BrainCode 2.0 — Consolidated Architecture Intelligence Plan

**Status:** Research / post-consolidation roadmap — DO NOT IMPLEMENT YET
**Date:** 2026-09-14
**Target repository:** `Kyra2214/BrainCode`
**Purpose:** Preserve the useful architectural findings from selected open-source projects so they can be incorporated only after the current BrainCode application is consolidated and its current behavior is revalidated.

---

## 0. Executive decision

BrainCode 2.0 is **not a rewrite** and is not a request to copy the selected projects.

The current BrainCode already has a stronger core boundary than most of the reviewed projects:

```text
Brain
  ↓
Planner / Router
  ↓
Policy
  ↓
Capability
  ↓
Sandbox
  ↓
Evidence / Events / Delivery
```

The current HEAD also has dynamic provider discovery, free-only routing, automatic waterfall/fallback, an Android `BrainApiGateway`, persistent knowledge memory, provenance and a conservative Critic. The current repository explicitly states that the Android chat → gateway caller still needs real proof before being considered integrated.

**BrainCode 2.0 therefore means consolidation + selective adoption of patterns, not feature accumulation.**

No feature from this document should be implemented until:

1. the current app is consolidated;
2. current Android/Brain/Sandbox wiring is revalidated;
3. existing tests/build/readiness are rerun on the resulting HEAD;
4. each proposed 2.0 change has an explicit contract, caller, evidence and regression test.

---

# 1. Audit scope

The review was narrowed to the projects that can materially improve the BrainCode runtime rather than the many vertical AI SaaS templates in the Awesome collection.

## Primary sources reviewed

| Project | Primary value for BrainCode | Decision |
|---|---|---|
| `SamurAIGPT/llm-wiki-agent` | persistent structured knowledge, cross-references, contradiction detection, graph, retrieval-oriented maintenance | **ADOPT CONCEPTS** |
| `Anil-matcha/open-grok-bot` | governed computer actions, action gateway, approvals, connectors, audit lifecycle, isolated computer runtime | **ADAPT CONCEPTS** |
| `SamurAIGPT/Generative-Media-Skills` | schema-driven skills, core/library split, reusable recipes, agent-native structured outputs | **ADOPT CONCEPTS** |
| `SamurAIGPT/Vibe-Workflow` | node/DAG workflow editor and reusable pipeline composition | **ADAPT CONCEPTS** |
| `Anil-matcha/open-ai-agents-hub` | reusable agent definitions, agent catalog, builder, per-agent history/configuration | **ADAPT CONCEPTS** |
| `Anil-matcha/open-claude-tag` | curated memory, automatic skill creation, scoped context, tool scoping, proactive routines | **ADOPT SELECTIVELY** |
| `SamurAIGPT/pixelrelay` | persistent async jobs, webhook completion, provider abstraction and failover/cooldown | **STUDY / ADOPT PATTERNS** |

The source collection itself describes these as complete applications, but many are designed around MuAPI, Stripe, OAuth and hosted SaaS deployment. Those product-level dependencies are deliberately excluded from BrainCode 2.0.

---

# 2. Current BrainCode baseline

The current BrainCode architecture already provides:

- Python reference runtime;
- Kotlin/JVM Brain;
- Android Compose client;
- PolicyBroker and approval;
- capability resolution;
- Sandbox/proot execution;
- workflows;
- skills;
- dynamic provider/model discovery;
- free-only policy;
- automatic provider waterfall/fallback;
- Android `BrainApiGateway`;
- persistent knowledge memory;
- provenance;
- Conservative Knowledge Critic;
- Security Test Lab;
- release/readiness documentation;
- three homologated RootFS versions (`0.3.3`, `0.4.1`, `0.5.0`).

Important current gaps documented by the project:

- Android chat is not yet proven to call `BrainApiGateway`;
- retrieval hints are stored but there is no automatic retrieval executor;
- Critic is structural/evidential, not a complete semantic validator;
- knowledge needs deduplication, version history and stronger indexing;
- Sandbox still lacks OS-level isolation guarantees;
- several wiring/security hardening items remain before production-level claims.

These existing gaps take precedence over any 2.0 feature.

---

# 3. Finding A — Persistent Knowledge Compiler

## Source

`SamurAIGPT/llm-wiki-agent`

The project turns source material into persistent, interlinked Markdown knowledge. It creates source pages, entity pages, concept pages, living synthesis, contradiction flags, a graph and lint reports. It also uses deterministic links plus inferred relationships and maintains the result through subsequent ingestion.

## What matters

The valuable idea is **knowledge compilation**, not Markdown or Obsidian itself.

BrainCode currently does:

```text
question
 ↓
validated memory
 ↓
router
 ↓
external answer
 ↓
candidate
 ↓
critic
 ↓
validated knowledge
```

2.0 should extend this to:

```text
question
 ↓
knowledge index
 ├─ exact match
 ├─ semantic/structural match
 ├─ source/repository retrieval
 └─ related knowledge
 ↓
if insufficient → provider/router
 ↓
answer + evidence
 ↓
critic
 ↓
knowledge compiler
 ↓
validated versioned knowledge
```

## Proposed BrainCode component

`KnowledgeCompiler`

Responsibilities:

- normalize a knowledge candidate;
- fingerprint/deduplicate equivalent knowledge;
- maintain source references;
- maintain entities/concepts/relationships;
- detect contradictory claims;
- preserve correction history;
- generate retrieval hints;
- update indexes;
- expose only validated knowledge to automatic recall.

## Do NOT copy

- Obsidian integration;
- visual HTML graph as a mandatory dependency;
- Claude-specific slash commands;
- repository layout;
- MarkItDown as a mandatory BrainCode dependency.

## BrainCode-specific improvement

Knowledge should be stored through a BrainCode contract, not tied to Markdown:

```text
KnowledgeRecord
├── id
├── scope
├── problem_signature
├── claim / answer
├── evidence[]
├── provenance[]
├── repository/path/commit
├── retrieval_hints[]
├── relations[]
├── fingerprint
├── confidence
├── validation_state
├── version
├── supersedes
└── timestamps
```

This directly addresses the current missing retrieval executor.

---

# 4. Finding B — Retrieval Executor

This is one of the most important 2.0 changes because the current BrainCode already records `retrievalHints` but does not yet automatically use them.

## Proposed flow

```text
Knowledge hit
 ↓
retrieval hints
 ├─ GitHub repository
 ├─ path
 ├─ commit
 ├─ source URI
 ├─ provider/model
 └─ semantic tags
 ↓
RetrievalExecutor
 ↓
fetch source
 ↓
verify source identity/version
 ↓
validate relevant evidence
 ↓
return evidence bundle
```

The Brain should learn **where to look again**, not only remember what it previously answered.

## Safety rule

A stored URL is not automatically trustworthy evidence. Source identity, fetched content and validation state must be tracked separately.

---

# 5. Finding C — Governed Action Gateway

## Source

`Anil-matcha/open-grok-bot`

The project contains a deny-by-default action registry, explicit workspace commands, approval gating, structured action request/result contracts, audit lifecycle records and a computer-provider abstraction. Its Docker computer runtime separates bot workspaces and applies resource limits.

## What matters

BrainCode already has Policy/Capability/Sandbox, so the goal is not to introduce a competing action system.

The useful pattern is to make **every external action a first-class, typed lifecycle**:

```text
REQUESTED
   ↓
CLASSIFIED
   ↓
POLICY_CHECK
   ↓
APPROVAL_REQUIRED? ── yes → PENDING
   ↓ no                    ↓
AUTHORIZED             APPROVED
   ↓                       ↓
EXECUTING ←───────────────┘
   ↓
SUCCEEDED / FAILED / DENIED / EXPIRED
```

Every transition should produce auditable evidence.

## Adopt

- deny-by-default action registry;
- normalized action request/result;
- explicit risk class;
- approval state machine;
- lifecycle audit events;
- connector action contracts;
- provider abstraction for computer execution;
- per-runtime workspace boundary;
- resource limits where the host platform can enforce them.

## Do not adopt blindly

The Docker/Playwright runtime is not a replacement for BrainCode's Sandbox. Proot is not OS-level isolation, and the source project itself warns that its runtime is not a hardened hostile-web sandbox.

---

# 6. Finding D — Skill System 2.0

## Sources

`SamurAIGPT/Generative-Media-Skills` and `Anil-matcha/open-claude-tag`

Both projects demonstrate a useful pattern: skills are reusable, human-readable playbooks that teach an agent how to perform a repeatable task.

## Proposed contract

```text
Skill
├── id
├── name
├── description
├── trigger conditions
├── required capabilities
├── required tools
├── inputs
├── outputs
├── procedure
├── safety constraints
├── validation procedure
├── provenance
├── version
├── usage_count
├── last_used
├── status
└── supersedes
```

## Skill lifecycle

```text
DISCOVERED
 ↓
CANDIDATE
 ↓
VALIDATED
 ↓
ACTIVE
 ↓
STALE
 ↓
ARCHIVED
```

A skill should never gain capabilities merely by being created. Its allowed capabilities remain controlled by Policy/Capability resolution.

## Auto-created skills

The Open Claude Tag pattern of creating a playbook after complex work is valuable, but BrainCode should make it stricter:

```text
complex task
 ↓
execution evidence
 ↓
candidate procedure
 ↓
Critic
 ↓
Sandbox replay/test
 ↓
Skill candidate
 ↓
approval or deterministic acceptance policy
 ↓
Active skill
```

The agent must not be allowed to silently teach itself an unsafe capability.

---

# 7. Finding E — Agent Registry / Reusable Agents

## Source

`Anil-matcha/open-ai-agents-hub`

The project treats an agent as a reusable definition containing persona/system behavior, capability type, skills, profile and history. It provides an agent library and builder.

## BrainCode adaptation

Add an **Agent Definition** abstraction without creating a second runtime:

```text
AgentDefinition
├── id
├── role
├── objective
├── planner profile
├── allowed skills
├── allowed capabilities
├── memory scope
├── provider policy
├── output contract
└── validation policy
```

Then:

```text
AgentDefinition
 ↓
Planner
 ↓
ExecutionPlan
 ↓
Policy
 ↓
Capability
 ↓
Sandbox
```

The agent registry is configuration and identity. The Brain remains the execution authority.

---

# 8. Finding F — Workflow/DAG Engine

## Source

`SamurAIGPT/Vibe-Workflow`

The useful part is the node-based pipeline model: reusable nodes, connected outputs, templates and repeatable workflows.

## BrainCode adaptation

BrainCode workflows should be represented as a typed DAG/plan rather than arbitrary visual automation:

```text
Workflow
 ↓
Node A → Node B → Node C
           ↘ Node D
 ↓
ExecutionPlan
 ↓
Policy per action
 ↓
Sandbox / Provider / Memory
```

### Node categories

- `LLM`
- `API`
- `SKILL`
- `CAPABILITY`
- `SANDBOX`
- `MEMORY_READ`
- `MEMORY_WRITE`
- `CRITIC`
- `APPROVAL`
- `GIT`
- `FILE`
- `CONDITION`
- `TRANSFORM`
- `OUTPUT`

### Critical invariant

A visual workflow must compile into the same `ExecutionPlan` contract used by the normal Brain planner. There must not be a second privileged execution path.

This addresses the existing BrainCode concern about proving the canonical `Planner → ExecutionPlan → Policy → AuthorizedPlan → Agent → Sandbox` chain.

---

# 9. Finding G — Async Jobs and Provider Failover

## Source

`SamurAIGPT/pixelrelay`

The strongest idea is persistent asynchronous job state combined with webhook completion and provider cooldown/failover. It explicitly avoids tying job state to one Python process.

## BrainCode adaptation

This should be considered for long-running operations:

```text
Task submitted
 ↓
JobStore
 ↓
Provider attempt #1
 ↓
callback / completion
 ↓
Evidence + result
```

If provider #1 fails:

```text
failure classification
 ↓
cooldown
 ↓
provider #2
 ↓
retry/failover
```

## Why this matters

The current BrainCode waterfall handles provider selection/failure, but 2.0 should distinguish:

- short synchronous request;
- long-running task;
- provider job;
- callback;
- orphaned job after process restart;
- retry/failover;
- final evidence.

## Proposed `JobRecord`

```text
JobRecord
├── job_id
├── task_id
├── provider
├── model
├── attempt
├── status
├── submitted_at
├── deadline
├── callback_state
├── retry_count
├── cooldown_reason
├── result_ref
├── evidence_ref
└── audit_ref
```

Do not make webhooks mandatory for providers that cannot provide them; use an internal worker adapter where necessary.

---

# 10. Cross-project synthesis

The seven projects converge on five ideas that are worth bringing into BrainCode:

## A. Everything important becomes a typed contract

```text
Agent
Skill
Workflow
Action
Job
Knowledge
Evidence
```

## B. Memory becomes structured, not just historical

```text
raw history
   ↓
curation
   ↓
knowledge
   ↓
relations
   ↓
retrieval
   ↓
validation
```

## C. Tools become governed actions

```text
Tool
 ↓
Action
 ↓
Policy
 ↓
Approval
 ↓
Execution
 ↓
Evidence
```

## D. Repeated work becomes reusable

```text
successful task
 ↓
validated procedure
 ↓
Skill / Workflow
 ↓
future reuse
```

## E. Long-running work becomes durable

```text
request
 ↓
JobStore
 ↓
worker/provider
 ↓
callback
 ↓
result/evidence
```

---

# 11. Proposed BrainCode 2.0 architecture

```text
                         ┌─────────────────────┐
                         │      Android UI     │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   Conversation      │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │       BRAIN         │
                         │ intent / planner    │
                         │ router / critic     │
                         └──────────┬──────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
              ▼                     ▼                     ▼
       Knowledge Engine       Agent Registry       Workflow Engine
              │                     │                     │
              └─────────────────────┼─────────────────────┘
                                    ▼
                           ExecutionPlan
                                    │
                                    ▼
                              Policy Layer
                                    │
                              ┌─────┴─────┐
                              │           │
                              ▼           ▼
                         Capability   Approval
                              │           │
                              └─────┬─────┘
                                    ▼
                              Action Gateway
                                    │
                 ┌──────────────────┼──────────────────┐
                 ▼                  ▼                  ▼
              Sandbox            Provider            Jobs
                 │                  │                  │
                 └──────────────────┼──────────────────┘
                                    ▼
                             Evidence/Event
                                    │
                         ┌──────────┴──────────┐
                         ▼                     ▼
                       Critic              Memory
                         │                     │
                         └──────────┬──────────┘
                                    ▼
                            Knowledge Compiler
```

---

# 12. What NOT to add

BrainCode 2.0 should explicitly reject feature creep from the audited projects.

Do not import merely because a source project has it:

- Stripe billing;
- SaaS account management;
- Vercel deployment assumptions;
- MuAPI-specific architecture;
- vendor-specific model names as core contracts;
- Slack-specific channel infrastructure;
- Obsidian as a required database/UI;
- Next.js dashboards when Android/local runtime is the actual target;
- duplicated provider gateways;
- a second agent runtime;
- a second policy system;
- a second sandbox system;
- a second memory system.

The BrainCode rule is **one authority per concern**.

---

# 13. Priority order after consolidation

## P0 — Preserve current architecture

1. Consolidate current application.
2. Re-run current tests/build/readiness on the consolidated HEAD.
3. Prove Android chat → `BrainApiGateway`.
4. Prove canonical planner/execution/policy/sandbox wiring.
5. Preserve RootFS artifacts and existing contracts.

## P1 — Knowledge 2.0

1. `KnowledgeCompiler`.
2. `RetrievalExecutor`.
3. fingerprints/deduplication.
4. versioned corrections.
5. structured evidence/citations.
6. source verification.
7. retrieval tests.

## P1 — Action Gateway hardening

1. unify all actions under one typed lifecycle.
2. deny-by-default registry.
3. approval state machine.
4. evidence for every action.
5. connector scope enforcement.
6. resource/risk classification.

## P1 — Skill 2.0

1. formal Skill contract.
2. capability requirements.
3. validation procedure.
4. lifecycle/staleness.
5. safe automatic skill candidates.

## P2 — Workflow Engine

1. typed DAG contract.
2. compiler → `ExecutionPlan`.
3. node validation.
4. replay/deterministic execution.
5. optional visual editor later.

## P2 — Agent Registry

1. reusable AgentDefinition.
2. memory scope.
3. skill scope.
4. provider policy.
5. capability policy.

## P2 — Durable Jobs

1. persistent job store.
2. long-running provider adapters.
3. restart recovery.
4. cooldown/failover.
5. callback/webhook normalization.

---

# 14. Security requirements for 2.0

Nothing from the source projects weakens these BrainCode invariants.

### Policy remains authoritative

No Skill, Agent, Workflow, connector or provider can bypass Policy.

### Sandbox remains authoritative for execution

A workflow node or skill cannot directly execute privileged host operations.

### Memory is not authority

Stored knowledge is information, never permission.

### Retrieval is untrusted until validated

A retrieved source can inform a plan but cannot grant capability.

### Auto-generated skills are untrusted until validated

A skill generated from previous execution must pass the BrainCode validation process before becoming active.

### Provider responses are untrusted input

Provider output must not directly execute commands or change policy.

### Provenance must survive transformation

When knowledge is synthesized, the resulting record should preserve the evidence chain to the underlying sources.

---

# 15. Licensing / reuse rule

The reviewed projects advertise permissive open-source licenses, including MIT for most of the selected projects and Apache-2.0 for Pixelrelay.

For BrainCode 2.0, the default approach is:

**architecture/pattern study first; independent BrainCode implementation second.**

If actual source code is ever copied/adapted, preserve the applicable license/notice requirements and record the exact source, commit and files in a provenance document before merging.

Do not copy vendor branding, proprietary hosted-service assumptions or source-specific credentials/configuration.

---

# 16. Acceptance criteria for every BrainCode 2.0 feature

A 2.0 feature is not complete because a class exists or a unit test passes.

It requires:

1. implementation;
2. contract/invariant;
3. automated test;
4. real caller from UI/runtime where applicable;
5. observable evidence;
6. failure-path test;
7. security/policy test;
8. documentation;
9. provenance where external source code/pattern was materially reused;
10. regression validation against the consolidated BrainCode.

This deliberately preserves the project's existing completion criterion.

---

# 17. Recommended implementation sequence after consolidation

```text
CURRENT BRAINCODE
      │
      ▼
CONSOLIDATION
      │
      ▼
FULL REVALIDATION
      │
      ├───────────────┐
      ▼               ▼
KNOWLEDGE 2.0      ACTION GATEWAY
      │               │
      └───────┬───────┘
              ▼
          SKILLS 2.0
              │
              ▼
         WORKFLOW DAG
              │
              ▼
         AGENT REGISTRY
              │
              ▼
          DURABLE JOBS
              │
              ▼
       FULL 2.0 VALIDATION
```

Do not start with the visual workflow UI. The contracts and execution semantics must exist first.

---

# 18. Final assessment

The audit confirms that the most valuable material in the selected open-source collection is **not the individual consumer applications**. It is the set of reusable patterns around:

- persistent structured knowledge;
- retrieval and provenance;
- governed tools/actions;
- reusable skills;
- typed workflows;
- reusable agent definitions;
- durable asynchronous jobs;
- provider failover.

BrainCode already owns the most important security/orchestration boundary. The 2.0 opportunity is therefore to make that boundary more capable without fragmenting it.

The guiding principle is:

> **BrainCode 2.0 should remember better, retrieve better, learn procedures safely, compose workflows, govern every action, and survive long-running work — while keeping Brain → Policy → Capability → Sandbox as the single execution authority.**

**Implementation status: intentionally deferred until current BrainCode consolidation is complete.**

---

## Source references

- https://github.com/SamurAIGPT/llm-wiki-agent
- https://github.com/Anil-matcha/open-grok-bot
- https://github.com/SamurAIGPT/Generative-Media-Skills
- https://github.com/SamurAIGPT/Vibe-Workflow
- https://github.com/Anil-matcha/open-ai-agents-hub
- https://github.com/Anil-matcha/open-claude-tag
- https://github.com/SamurAIGPT/pixelrelay
- https://github.com/Anil-matcha/awesome-generative-ai-apps
