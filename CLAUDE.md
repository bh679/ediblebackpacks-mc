# Edible Backpacks — sibling mod of Dungeon Train

NeoForge 1.21.1 mod (`bh679/ediblebackpacks-mc`, pkg `games.brennan.ediblebackpacks`).
Eat an `edible_backpack` item → +1 persistent backpack slot (cap 108), shown as two
6×9 panels flanking the survival inventory screen. Slots unlock **down a column, then
across**, starting on the column closest to the player's inventory and growing outward
(`menu/BackpackLayout`).

Two items, both `EdibleBackpackItem` with a different `slotsGranted`: `edible_backpack` (1)
and `upgraded_backpack` (9). Both non-stackable; 3×3 singles craft into one upgraded (one-way only — a 9-count result is
invalid against a max stack size of 1, which is exactly what the reverse recipe would need).
Grants top up to the cap (`BackpackPolicy.effectiveGrant`) so an upgraded backpack near the
cap still does something instead of being stranded.

## Architecture map

- `storage/BackpackData` — per-player serializable attachment (`registry/ModAttachments`,
  `copyOnDeath`): unlocked count + 108-slot `ItemStackHandler` (always full-size; slot
  indices must stay stable for vanilla container sync).
- `mixin/InventoryMenuMixin` — appends 108 `menu/BackpackSlot`s to the vanilla
  `InventoryMenu` on BOTH sides. `BackpackSlot.mayPlace/mayPickup` are the
  server-authoritative lock; `isActive` is display-only (also hides while the recipe
  book is open via `client/ClientPanelState`).
- `menu/BackpackQuickMove` — shift-click routing (called from `InventoryMenuMixin`'s
  `quickMoveStack` HEAD inject): the panels act like an always-open container —
  inventory/hotbar → backpack (armour/offhand auto-equip still wins; falls back to
  vanilla's hotbar↔inventory shuffle when nothing fits), backpack → inventory, and
  crafting/armour/offhand slots overflow into the backpack when the inventory is full.
- `client/BackpackScreenPanels` — draws panel chrome on `ContainerScreenEvent.Render.Background`;
  vanilla renders the slot items itself.
- `network/SlotCountPayload` — server→client unlocked-count sync (attachments don't
  auto-sync). Sent on login/respawn/dimension-change/eat (`event/BackpackEvents`).
- Death policy — `storage/BackpackPolicy.shouldResetOnDeath(configMode, hostDefault)`.
  Standalone default = persist. Host mods call
  `EdibleBackpacksApi.setHostDefaultResetOnDeath(true)` to make `DEFAULT` mean reset
  (Dungeon Train does). Explicit server config `ON`/`OFF` always wins.

## Release contract (Dungeon Train jarJar)

Release asset MUST be `ediblebackpacks-neoforge-<v>.jar` from tag `v<version>` —
DT's Ivy patternLayout depends on it. `release.yml` is dispatch-only and creates the
tag; `version-bump.yml` MINOR-bumps on main pushes when PATCH ≠ 0, so releases are
MINOR-aligned (X.Y.0). Never `git tag` manually.

## Testing

`./gradlew test` (pure-logic: policy + layout math). In-game verification happens in
the Dungeon Train dev client where the mod is bundled.
