# Edible Backpacks — sibling mod of Dungeon Train

NeoForge 1.21.1 mod (`bh679/ediblebackpacks-mc`, pkg `games.brennan.ediblebackpacks`).
Eat an `edible_backpack` item → +1 persistent backpack slot (cap 108), shown as two
6×9 panels flanking the survival inventory screen. Slots unlock **down a column, then
across**, starting on the column closest to the player's inventory and growing outward
(`menu/BackpackLayout`).

Two items, both `EdibleBackpackItem` with a different `slotsGranted`: `edible_backpack` (1)
and `upgraded_backpack` (9). Both non-stackable; 8 singles ringing a `minecraft:gold_block` in the centre craft into one
upgraded (shaped, so the block can only sit in the middle; one-way only — an 8-count result is
invalid against a max stack size of 1, which is exactly what the reverse recipe would need).
Grants top up to the cap (`BackpackPolicy.effectiveGrant`) so an upgraded backpack near the
cap still does something instead of being stranded.

## Architecture map

- `storage/BackpackData` — per-player serializable attachment (`registry/ModAttachments`,
  `copyOnDeath`): unlocked count + 108-slot `ItemStackHandler` (always full-size; slot
  indices must stay stable for vanilla container sync).
- `mixin/InventoryMenuMixin` — appends 108 `menu/BackpackSlot`s to the vanilla
  `InventoryMenu` on BOTH sides. `BackpackSlot.mayPlace/mayPickup` are the
  server-authoritative lock; `isActive` is display-only.
  **Never leave a panel slot inactive while it can still accept items.** `moveItemStackTo`
  consults `mayPlace`, never `isActive`, so a hidden-but-live slot silently swallows
  quick-moved stacks — a shift-clicked crafting result leaves the grid and lands nowhere
  the player can see. That is why the recipe book makes the panels MOVE
  (`client/BackpackPanelLayout` + `BackpackLayout.slotX(index, bookMode)`: both panels go
  right of the GUI, clear of the book) instead of switching off. Only a screen too narrow
  for the moved layout still hides them.
  **Never capture the attachment or its handler in the menu/slots.** The menu is
  built in the `Player` constructor, and NeoForge replaces the whole `BackpackData`
  object afterwards — on login (`deserializeAttachments` → `map.put(type, read(...))`)
  and on respawn (`copyOnDeath` → `copyAttachments` → `map.put(type, copy)`). A
  captured handler is orphaned from the first relog on: slots still unlock correctly
  (the count is read live) but read/write a discarded `ItemStackHandler`, so anything
  stored there is voided at the next save. `BackpackSlot.getItemHandler()` re-resolves
  on every access; `mayPlace`/`getMaxStackSize` are re-implemented because
  `SlotItemHandler` reads its captured field directly in exactly those three.
  Each menu also hands its panels their own empty `SimpleContainer` marker via
  `mixin/SlotAccessor`: NeoForge gives every `SlotItemHandler` one shared
  `emptyInventory`, and vanilla's `transferState` (on every container close)
  matches slots by `(container, containerSlot)`, so without the marker another
  mod's item-handler slots overwrite this panel's sync bookkeeping.
- `menu/BackpackQuickMove` — shift-click routing (called from `InventoryMenuMixin`'s
  `quickMoveStack` HEAD inject): the panels act like extra space behind the vanilla
  destinations — inventory/hotbar run vanilla's own hotbar↔inventory shuffle FIRST
  (armour/offhand auto-equip still wins) and only the leftovers overflow into the
  backpack, backpack → inventory, and
  crafting/armour/offhand slots overflow into the backpack when the inventory is full.
  The crafting result tries the offhand between the two, so a bulk craft with a full
  inventory still lands somewhere visible. That extra call is safe where `INV_END` is
  not: the duplication `INV_END` guards against needs the source slot inside the
  destination range, and the result slot is 0.
- `client/BackpackScreenPanels` — draws panel chrome on `ContainerScreenEvent.Render.Background`;
  vanilla renders the slot items itself. Also owns `client/BackpackToggleButton`, added to the
  screen at `ScreenEvent.Init.Post` (NeoForge's `addListener` puts a widget in both `children`
  and `renderables`) and re-positioned every frame above the offhand slot — vanilla only moves
  its own widgets when the recipe book slides the GUI.
- Open/close toggle — `BackpackData.panelsOpen`, persisted and synced both ways by
  `network/PanelOpenPayload` (one id, `playBidirectional` + `DirectionalPayloadHandler`;
  registering the same payload twice is a hard error). Closed is a real lock, not a hide:
  `BackpackSlot.usableNow()` fails `mayPlace`/`mayPickup`/`isActive`, so quick-move just does
  what vanilla would. That is the whole reason the flag has to reach the server —
  `moveItemStackTo` never asks `isActive`. `BackpackPanelLayout.Mode.CLOSED` is the client
  half, and `client/BackpackKeyBindings` the keyboard one — an unbound-by-default mapping,
  handled both on client tick (in-world) and on `ScreenEvent.KeyPressed.Pre` (vanilla stops
  ticking key mappings while a screen is open).
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

`./gradlew test` covers two layers:
- pure logic (policy + layout math), no Minecraft on the classpath;
- `InventoryMenuQuickMoveTest` — a **real** `InventoryMenu` with the mixin applied, driven
  on an ephemeral server (`neoForge.unitTest` + NeoForge's `EphemeralTestServerProvider` +
  a `FakePlayer`), so a shift-click there is the call the server really runs. Levels are
  created reflectively: that server skips `loadLevel`, and the spawn search needs a ticking
  chunk system, so the world data is switched to ADVENTURE to skip it.

In-game verification still happens in the Dungeon Train dev client where the mod is
bundled. For anything client-side (panel placement, hit-testing), a scripted `runClient`
with a temporary `ClientTickEvent` driver reproduces a click end to end without a human.
