# Publishing Edible Backpacks standalone (CurseForge + Modrinth)

`release.yml` already contains the mc-publish step. It is **gated on credentials being present**
and currently skips with a warning, because this repo has no secrets or variables set. Fill those
in and every future release publishes to both platforms automatically — no workflow changes needed.

## 1. Create the two projects (manual — needs Brennan's accounts)

Neither platform lets an agent create a project: CurseForge has no project-creation API at all,
and Modrinth's requires an authenticated token this repo doesn't hold.

| | CurseForge | Modrinth |
|---|---|---|
| Create at | https://console.curseforge.com → Projects → Create | https://modrinth.com/dashboard/projects → Create |
| Name | Edible Backpacks | Edible Backpacks |
| Slug | `edible-backpacks` | `edible-backpacks` |
| Game / loader | Minecraft 1.21.1 · NeoForge | Minecraft 1.21.1 · NeoForge |
| Licence | PolyForm Shield 1.0.0 (custom — link the repo `LICENSE`) | same |
| Source | https://github.com/bh679/ediblebackpacks-mc | same |
| Issues | https://github.com/bh679/ediblebackpacks-mc/issues | same |
| Categories | Storage, Food, Utility | storage, food, utility |

CurseForge projects sit in an approval queue after creation; Modrinth requires the project to be
submitted for review once it has an icon, description, and at least one version.

## 2. Listing copy

**Summary (one line):** Eat a backpack, gain an inventory slot.

**Description:**

> Edible Backpacks adds an apple pack you can eat. Doing so grows your personal storage by one
> slot — carried in two panels that sit either side of your inventory screen and fill a column at
> a time as you eat more, up to 108 slots. The panels tuck away while the crafting recipe book is
> open, so they never fight the vanilla UI.
>
> Backpacks don't stack, but they do compress: nine ordinary packs craft into a golden Upgraded
> Backpack worth nine slots in a single bite.
>
> By default your backpack survives death. Servers can flip that with `resetOnDeath` in
> `serverconfig/ediblebackpacks-server.toml`, and host mods can set the default themselves —
> Dungeon Train does exactly that, making the backpack something you lose on death and have to
> rebuild each run.
>
> The mod ships no recipe for the base backpack: where it comes from is left to the pack or host
> mod. Dungeon Train rolls it into train loot.

**Icon:** 512×512 available — generated from the item texture (nearest-neighbour, transparent margin).

## 3. Wire the credentials

Both tokens already exist as secrets on `bh679/dungeon-train-mc`, but GitHub secrets are
write-only, so they cannot be copied across — paste them from the source of truth:

```bash
gh secret set MODRINTH_TOKEN   --repo bh679/ediblebackpacks-mc
gh secret set CURSEFORGE_TOKEN --repo bh679/ediblebackpacks-mc
gh variable set MODRINTH_PROJECT_ID   --repo bh679/ediblebackpacks-mc --body edible-backpacks
gh variable set CURSEFORGE_PROJECT_ID --repo bh679/ediblebackpacks-mc --body '<numeric id from the CF project URL>'
```

## 4. Ship a version

Publishing is triggered by a release, and `release.yml` refuses a tag that already exists — so
v0.1.0–v0.6.0 cannot be republished. Cut a **new** version once the credentials are in place:

```bash
# in ~/Projects/EdibleBackpacks, mod_version must be MINOR-aligned (X.Y.0)
gh workflow run release.yml -f tag=v0.7.0 -f changelog="First standalone release."
```

## 5. Afterwards: un-bundle from Dungeon Train

DT currently jarJars this mod because its listings aren't public. Once **both** listings are
approved, switch DT to the external-required-dep contract used by AIN/AIS/PMOB/ECP — see the
TradeEverything note in DT's `gradle.properties`:

- add `ediblebackpacks_min_version` and point `neoforge.mods.toml` at the floor instead of the pin
- drop the `jarJar(...)` line, keep `implementation`
- add a `modpack/modpack.config.json` entry (`slug`, `required`, `dependency_type: required`,
  `gradle_property`, `modrinth_project`, `modrinth_version`, CF `project_id`/`file_id`)
- add `edible-backpacks(required)` to both dependency lists in DT's `release.yml`
- add a deptest key and run `scripts/deptest/run-all.sh` — that path becomes testable only once
  the mod can actually go missing

That is a separate Gate 1 change, not a follow-on commit.
