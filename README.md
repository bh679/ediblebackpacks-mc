# Edible Backpacks

A NeoForge 1.21.1 mod: **eat a backpack to permanently grow your personal backpack
storage by one slot.**

- Backpack storage renders as two 6×9 panels flanking the vanilla survival inventory
  screen (up to **108 slots**). Slots unlock down a column at a time, starting with the
  column closest to your inventory and growing outward. Panels hide while the crafting
  recipe book is open.
- Contents and unlocked slots persist through death **by default**. Servers can set
  `resetOnDeath = ON` in `serverconfig/ediblebackpacks-server.toml`, and host mods that
  bundle this mod (e.g. Dungeon Train) can steer the `DEFAULT` policy via
  `EdibleBackpacksApi.setHostDefaultResetOnDeath(boolean)` — an explicit server-config
  `ON`/`OFF` always wins.
- No crafting recipe of its own: obtainability is left to the host mod / datapacks
  (Dungeon Train rolls it into train container loot).

## Config (`ediblebackpacks-server.toml`, per-world SERVER config)

| Key | Default | Meaning |
|---|---|---|
| `resetOnDeath` | `DEFAULT` | `DEFAULT` = follow host mod (standalone: persist) · `ON` = slots reset + contents drop on death · `OFF` = always persist |
| `maxSlots` | `108` | Cap on slots unlockable by eating backpacks (render cap 108) |

## Development

```bash
./gradlew build       # jar at build/libs/ediblebackpacks-neoforge-<v>.jar
./gradlew test        # pure-logic unit tests
./gradlew runClient   # dev client
```

Releases are dispatch-only (`.github/workflows/release.yml`): the workflow creates the
tag from `main`, asserts it matches `mod_version`, and uploads
`ediblebackpacks-neoforge-<v>.jar` — the exact asset name Dungeon Train's Ivy
repository resolves for jarJar bundling. `version-bump.yml` MINOR-bumps `mod_version`
on pushes to `main` when PATCH ≠ 0, so release versions must be MINOR-aligned (X.Y.0).

## Licence

PolyForm Shield 1.0.0.
