# Edible Backpacks — sibling mod of Dungeon Train

NeoForge 1.21.1 mod (`bh679/ediblebackpacks-mc`, pkg `games.brennan.ediblebackpacks`).
Eat an `edible_backpack` item → +1 persistent backpack slot (cap 108), shown as two
6×9 panels flanking the survival inventory screen, every chest-shaped screen (chests,
trapped chests, ender chests, barrels, shulker boxes) and the villager / wandering-trader
trade screen. The left panel hangs off the GUI's left edge and the right one off its right
edge, so the layout math only needs the GUI's width (`getXSize()`: 176, or 276 for trading;
`BackpackLayout.slotX(index, bookMode, guiWidth)`). Slots unlock **down a column, then
across**, starting on the column closest to the player's inventory and growing outward
(`menu/BackpackLayout`).

Two items, both `EdibleBackpackItem` with a different `slotsGranted`: `edible_backpack` (1)
and `golden_edible_backpack` (9). Both non-stackable; 8 singles ringing a `minecraft:gold_block` in the centre craft into one
golden (shaped, so the block can only sit in the middle; one-way only — an 8-count result is
invalid against a max stack size of 1, which is exactly what the reverse recipe would need).
The recipe reaches the recipe book the vanilla way, through a recipe advancement
(`data/ediblebackpacks/advancement/recipes/misc/golden_edible_backpack.json`, folder singular since
1.21): holding an `edible_backpack` unlocks it. Being 3×3 it shows in a crafting table's book,
not the 2×2 inventory one.
Grants top up to the cap (`BackpackPolicy.effectiveGrant`) so a golden edible backpack near the
cap still does something instead of being stranded.
Food: `PLAIN_FOOD` copies `Foods.APPLE`, `GOLDEN_FOOD` copies `Foods.GOLDEN_APPLE` (effects
included), both with `canAlwaysEat` forced on; vanilla's `super.finishUsingItem` applies them.
At the slot cap `use()` still refuses, so the food is never eaten for nothing.

## Architecture map

- `storage/BackpackData` — per-player serializable attachment (`registry/ModAttachments`,
  `copyOnDeath`): unlocked count + 108-slot `ItemStackHandler` (always full-size; slot
  indices must stay stable for vanilla container sync).
- `mixin/InventoryMenuMixin`, `mixin/ChestMenuMixin`, `mixin/ShulkerBoxMenuMixin` — append 108
  `menu/BackpackSlot`s to the vanilla `InventoryMenu`, `ChestMenu` (chests, trapped chests,
  ender chests, barrels, every `GENERIC_9xN`) and `ShulkerBoxMenu` on BOTH sides, all through
  `menu/BackpackSlotAppender`. Each mixin injects the ONE constructor every other constructor
  and factory on that class delegates to via `this(...)`, so a single TAIL fires exactly once.
  `BackpackSlotAppender` sits in `menu`, not `mixin`: Mixin refuses to class-load an ordinary
  class out of a package it owns (accessor interfaces are exempt, which is why `SlotAccessor`
  can be referenced from `BackpackSlot`). `BackpackSlot.mayPlace/mayPickup` are the
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
- `menu/BackpackQuickMove` — shift-click routing for the survival menu (called from
  `InventoryMenuMixin`'s `quickMoveStack` HEAD inject): the panels act like extra space behind the vanilla
  destinations — inventory/hotbar run vanilla's own hotbar↔inventory shuffle FIRST
  (armour/offhand auto-equip still wins) and only the leftovers overflow into the
  backpack, backpack → inventory, and
  crafting/armour/offhand slots overflow into the backpack when the inventory is full.
  The crafting result tries the offhand between the two, so a bulk craft with a full
  inventory still lands somewhere visible. That extra call is safe where `INV_END` is
  not: the duplication `INV_END` guards against needs the source slot inside the
  destination range, and the result slot is 0.
- `mixin/MerchantMenuMixin` + `menu/MerchantQuickMove` + `menu/MerchantPayment` — the trade
  screen. Unlike the chest mixins vanilla's quick-move is NOT replaced (its result-slot take
  completes the trade, and Dungeon Train HEAD-injects the same method for its extra-cost grid):
  a HEAD inject handles clicks on panel slots only (→ inventory), a RETURN inject moves what
  vanilla left in a payment/inventory slot into the backpack. The result slot's own
  `moveItemStackTo(3, 39, true)` (ordinal 0) is WRAPPED, not split (a second pass would fire the
  trade twice): `MerchantQuickMove#moveResult` tops up inventory stacks, then backpack stacks,
  then vanilla, then empty backpack slots. Picking a trade (`tryMoveItems`
  RETURN) tops the payment slots up from the backpack after vanilla filled them from the
  inventory, only from slots `mayPickup` allows (closed/locked/host-locked backpack never pays).
  Never index past slot 38 by number: Dungeon Train appends its extra-cost slots to the same
  menu, and mixin order decides whose slots come first.
- `mixin/CraftingMenuMixin`, `mixin/AbstractFurnaceMenuMixin` (furnace, blast furnace, smoker),
  `mixin/ItemCombinerMenuMixin` (anvil, smithing table) + `menu/WorkstationQuickMove` — the
  merchant pattern generalised to any menu whose vanilla quick-move uses FIXED ranges: vanilla
  keeps running; a panel click feeds the station's inputs (each mixin's callback, using
  vanilla's own `canSmelt`/`isFuel`/`canMoveIntoInputSlots` tests) then the inventory; RETURN
  overflow into the backpack runs ONLY when vanilla returned EMPTY, so its hotbar shuffle and
  partial moves are untouched; the output's ordinal-0 `moveItemStackTo` is wrapped by
  `moveResult`. `MerchantQuickMove` delegates its shared parts here.
- `mixin/GrindstoneMenuMixin`, `StonecutterMenuMixin` (`@Shadow level` for the recipe test),
  `LoomMenuMixin`, `CartographyTableMenuMixin` — the same workstation pattern, output wrapped.
  `EnchantmentMenuMixin` and `BrewingStandMenuMixin` have no stackable output, so no wrap; they
  pass `restToInventory = false` to `fromBackpack` (the rest of the stack goes to the inventory
  only when the station took nothing). Vanilla's click loop repeats a quick-move while it
  returns the same item: the brewing stand relies on that to fill one bottle slot per call, and
  the enchanting table returns EMPTY to stop it after placing its one item.
  `CrafterMenuMixin` injects every constructor (neither delegates) and uses the workstation
  pattern with the grid as input; its display-only result slot sits after the inventory.
- `mixin/HopperMenuMixin`, `mixin/DispenserMenuMixin` (dispenser + dropper) — handled like the chests with
  `ContainerQuickMove` (the hopper's vanilla quick-move ends at `slots.size()`, which would pour it into the panels).
- `menu/ContainerQuickMove` — the same job for the chest-shaped menus, parameterised by the
  container's size: container → inventory, inventory → container, backpack → container, each
  overflowing into the next place the player can see. Intercepting is mandatory, not cosmetic —
  vanilla's chest quick-move ends its player-side range at `slots.size()`, which now runs into
  the panels, so left alone it would bypass both the inventory and the open/close lock.
- `client/BackpackScreenPanels` — draws panel chrome on `ContainerScreenEvent.Render.Background`;
  vanilla renders the slot items itself. Which screens it acts on is asked of the MENU
  (`BackpackQuickMove.backpackStart(slots) >= 0`), never the screen class, so it cannot disagree
  with what the mixins did — all these GUIs are 176 wide, so the same layout math places them.
  Also owns `client/BackpackToggleButton`, added to the
  screen at `ScreenEvent.Init.Post` (NeoForge's `addListener` puts a widget in both `children`
  and `renderables`) and re-positioned every frame — vanilla only moves its own widgets when
  the recipe book slides the GUI. The button is optional and movable: `config/EBClientConfig`
  (CLIENT toml) carries on/off plus a `ButtonAnchor` of ABOVE_PANEL / CUSTOM x,y, resolved by
  the pure, unit-tested `client/ButtonPlacement`. `ABOVE_PANEL` puts it just above the left
  panel, outside the GUI box, and is the same spot on every screen — nowhere inside the box is
  free everywhere: the survival landmarks the old OFFHAND / RECIPE_BOOK presets named are
  container slots in a chest, and the chest title bar they used to fall back to is where
  sorting mods put their button row. Those coordinates survive as constants the config comment
  offers for `CUSTOM`, which is honoured verbatim. A screen with no room above it
  (`ButtonPlacement.roomAbove(guiTop)`) falls back inside the box. Switching it off is safe
  only because the hotkey exists.
- Open/close toggle — `BackpackData.panelsOpen`, persisted and synced both ways by
  `network/PanelOpenPayload` (one id, `playBidirectional` + `DirectionalPayloadHandler`;
  registering the same payload twice is a hard error). Closed is a real lock, not a hide:
  `BackpackSlot.usableNow()` fails `mayPlace`/`mayPickup`/`isActive`, so quick-move just does
  what vanilla would. That is the whole reason the flag has to reach the server —
  `moveItemStackTo` never asks `isActive`. `BackpackPanelLayout.Mode.CLOSED` is the client
  half, and `client/BackpackKeyBindings` the keyboard one — an unbound-by-default mapping,
  handled both on client tick (in-world) and on `ScreenEvent.KeyPressed.Pre` (vanilla stops
  ticking key mappings while a screen is open).
- Ground pickups — `mixin/ItemEntityMixin` `@WrapOperation`s the single `Inventory.add` call in
  `ItemEntity.playerTouch` and routes it through `storage/BackpackPickup`: top up matching
  backpack stacks (even with inventory room), then vanilla's add, then empty backpack slots.
  Wrapping just that call keeps vanilla's delay/owner checks, NeoForge pickup events, take
  animation, stats and discard; an event can't do it, because `Inventory.add` returns false on
  a full inventory and vanilla then skips the whole pickup. Unlocked slots only, nothing while
  the panels are closed, SERVER config `autoCollect` (default on). `BackpackPickupTest` drives
  a real `playerTouch` on the ephemeral server.
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
- `InventoryMenuQuickMoveTest` / `ChestMenuQuickMoveTest` / `MerchantMenuQuickMoveTest` /
  `WorkstationMenuQuickMoveTest` — **real** `InventoryMenu`,
  `ChestMenu` and `ShulkerBoxMenu` instances with the mixins applied, driven
  on an ephemeral server (`neoForge.unitTest` + NeoForge's `EphemeralTestServerProvider` +
  a `FakePlayer`), so a shift-click there is the call the server really runs. Levels are
  created reflectively: that server skips `loadLevel`, and the spawn search needs a ticking
  chunk system, so the world data is switched to ADVENTURE to skip it.

In-game verification still happens in the Dungeon Train dev client where the mod is
bundled. For anything client-side (panel placement, hit-testing), a scripted `runClient`
with a temporary `ClientTickEvent` driver reproduces a click end to end without a human.
