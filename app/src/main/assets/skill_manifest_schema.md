# SkillManifest

Cada skill registrada deve declarar:

- `id`
- `name`
- `version`
- `description`
- `category`
- `capabilities`
- `triggers`
- `exclusions`
- `required_permissions`
- `risk_class`
- `sandbox_required`
- `tools`
- `preferred_agents`
- `preferred_providers`
- `input_schema`
- `output_schema`
- `validation`
- `trust_level`
- `enabled`

## Regras

1. `triggers` orientam descoberta; não são autorização.
2. `required_permissions` são requisitos que o PolicyBroker deve avaliar.
3. `risk_class` determina o nível mínimo de política.
4. `sandbox_required=true` impede execução direta fora do ambiente controlado.
5. `trust_level` representa confiança da origem, não autorização.
6. Alterações de versão devem ser rastreáveis.
7. Skills de terceiros precisam de validação de manifesto, hash, licença e segurança.
