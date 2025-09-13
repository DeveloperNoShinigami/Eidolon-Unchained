# Chant JSON Reference

Location

- `data/<namespace>/chants/*.json`

Schema (common fields)

- `id` (string): Namespaced ID
- `name` (string): Display name
- `signs` (array of resource IDs): Ordered list of sign IDs
- `spell` (string): Linked spell ID (e.g., custom `DatapackChantSpell`)

Example

```json
{
  "id": "eidolonunchained:divine_communion",
  "name": "Divine Communion",
  "signs": ["eidolon:sacred", "eidolon:harmony", "eidolon:warding"],
  "spell": "eidolonunchained:divine_communion"
}
```

Debugging

- Check server logs for recognized sign sequences and matched spells.
- Ensure your signs exist in the target environment (`elucent.eidolon.registries.Signs`).
