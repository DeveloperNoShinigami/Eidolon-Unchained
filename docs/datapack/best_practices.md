# Datapack Best Practices

## Common Pitfalls
- **Missing `mods.toml`** – Forge warns when required metadata is absent; ensure every JAR contains a `mods.toml`.
- **Unreadable refmaps** – If a mixin reference map cannot be read, related mixins may silently fail.
- **Invalid world symlinks** – Broken symlinks can crash world selection screens; verify symlinks before packaging.

## Logging Tips
- Review `latest.log` for warnings such as missing `mods.toml` entries or unreadable reference maps.
- Call `ResearchDataManager.logLoadedData()` to list loaded datapack entries during development.
- Keep logs clean and address warnings before release.

## Version Control
- Use Git for iterative development: commit early and reference issues fixed in the changelog.
- Tag releases and maintain a changelog so players can track compatibility.
- Develop new features on dedicated branches and merge only after review.
