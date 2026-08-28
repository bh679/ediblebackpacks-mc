package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.menu.BackpackSlot;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quick-move behaviour driven through a <em>real</em> {@code InventoryMenu} on a real
 * server, with the mixin applied — the layer {@link BackpackQuickMoveTest} can't reach.
 *
 * <p>An ephemeral JUnit server (NeoForge's test framework) plus a {@code FakePlayer} gives
 * a genuine menu, genuine recipes and genuine {@code moveItemStackTo} routing, so a
 * shift-click here is the same call the server runs for a real click. Levels are created
 * reflectively: the ephemeral server deliberately skips {@code loadLevel}, and the spawn
 * search it would otherwise run needs a ticking chunk system this harness has no use for.</p>
 *
 * <p>The grid is loaded with the vertical two-plank stick recipe, so one shift-click on the
 * result bulk-crafts 64 times for 256 sticks.</p>
 */
@ExtendWith(EphemeralTestServerProvider.class)
class InventoryMenuQuickMoveTest {

    private static final int CRAFTS = 64;
    private static final int STICKS = CRAFTS * 4;

    // --- harness -----------------------------------------------------------

    private static ServerLevel level(MinecraftServer server) throws Exception {
        if (server.overworld() != null) return server.overworld();
        Method m = MinecraftServer.class.getDeclaredMethod("createLevels", ChunkProgressListener.class);
        m.setAccessible(true);
        try {
            m.invoke(server, new ChunkProgressListener() {
                public void updateSpawnPos(net.minecraft.world.level.ChunkPos center) {}
                public void onStatusChange(net.minecraft.world.level.ChunkPos p, net.minecraft.world.level.chunk.status.ChunkStatus s) {}
                public void start() {}
                public void stop() {}
            });
        } catch (Exception expected) {
            // The spawn-point search needs a ticking chunk system; the overworld
            // ServerLevel is registered before that runs, which is all we need.
        }
        // ServerPlayer's constructor hunts for a spawn position — which loads chunks, and
        // this server never ticks its chunk system. Adventure mode is the one branch that
        // skips the search, so the FakePlayer below can be built at all.
        if (server.getWorldData() instanceof net.minecraft.world.level.storage.ServerLevelData data) {
            data.setGameType(net.minecraft.world.level.GameType.ADVENTURE);
        }
        return server.overworld();
    }

    /**
     * Fresh state: empty inventory, empty backpack, panels open, {@code unlocked} slots
     * granted. The FakePlayer is cached per level, so every field a test can change has to
     * be reset here or it leaks into the next one.
     */
    private static FakePlayer player(ServerLevel level, int unlocked) {
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        BackpackData data = player.getData(ModAttachments.BACKPACK);
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) data.items().setStackInSlot(i, ItemStack.EMPTY);
        data.setUnlocked(unlocked);
        data.setPanelsOpen(true);
        for (int i = 0; i <= BackpackQuickMoveHelper.OFFHAND; i++) player.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
        return player;
    }

    private static void loadGrid(InventoryMenu menu) {
        menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS, CRAFTS));
        menu.getSlot(3).set(new ItemStack(Items.OAK_PLANKS, CRAFTS));
        menu.slotsChanged(null);
    }

    private static void fillInventory(Inventory inv) {
        for (int i = 0; i < 36; i++) inv.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
    }

    /** Hotbar only (inventory indices 0..8) — the destination a main-inventory click prefers. */
    private static void fillHotbar(Inventory inv) {
        for (int i = 0; i < 9; i++) inv.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
    }

    private static int inHotbar(FakePlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    /** Main inventory rows only (inventory indices 9..35). */
    private static int inMainInventory(FakePlayer p, Item item) {
        int n = 0;
        for (int i = 9; i < 36; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static void shiftClickResult(FakePlayer player) {
        player.inventoryMenu.clicked(0, 0, ClickType.QUICK_MOVE, player);
    }

    private static int inInventory(FakePlayer p, Item item) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) n += inv.getItem(i).getCount();
        return n;
    }

    private static int inBackpack(FakePlayer p, Item item) {
        BackpackData data = p.getData(ModAttachments.BACKPACK);
        int n = 0;
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            ItemStack s = data.items().getStackInSlot(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    /** Keeps the slot-index constants readable without re-exporting them from the menu. */
    private static final class BackpackQuickMoveHelper {
        static final int OFFHAND = 45;
        static final int FIRST_BACKPACK = 46;
    }

    // --- tests -------------------------------------------------------------

    @Test
    void mixinAppendsAllBackpackSlots(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 0);
        assertEquals(46 + BackpackLayout.MAX_SLOTS, p.inventoryMenu.slots.size());
    }

    @Test
    void shiftClickingTheResultBulkCrafts(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 0);
        loadGrid(p.inventoryMenu);
        shiftClickResult(p);

        assertEquals(STICKS, inInventory(p, Items.STICK), "one shift-click should craft the whole grid");
        assertTrue(p.inventoryMenu.getSlot(1).getItem().isEmpty(), "grid should be consumed");
    }

    @Test
    void partialSpaceCraftsOnlyWhatFits(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 0);
        fillInventory(p.getInventory());
        p.getInventory().setItem(20, ItemStack.EMPTY);
        // Offhand occupied, so this isolates the inventory pass from the offhand fallback.
        p.inventoryMenu.getSlot(BackpackQuickMoveHelper.OFFHAND).set(new ItemStack(Items.SHIELD));
        loadGrid(p.inventoryMenu);
        shiftClickResult(p);

        assertEquals(64, inInventory(p, Items.STICK), "exactly one stack fits");
        assertEquals(48, p.inventoryMenu.getSlot(1).getItem().getCount(), "the rest of the grid is untouched");
    }

    @Test
    void aFullInventoryFallsBackToTheOffhand(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 0);
        fillInventory(p.getInventory());
        loadGrid(p.inventoryMenu);
        shiftClickResult(p);

        // The offhand is the last destination the player can always see, so it comes before
        // the panels — and before giving up, which is what made a full-inventory shift-click
        // look like the click had done nothing at all.
        ItemStack offhand = p.inventoryMenu.getSlot(BackpackQuickMoveHelper.OFFHAND).getItem();
        assertTrue(offhand.is(Items.STICK), "result should fall back to the offhand, got " + offhand);
        assertEquals(64, offhand.getCount());
        assertEquals(CRAFTS - 16, p.inventoryMenu.getSlot(1).getItem().getCount(),
            "16 crafts fill one offhand stack, the rest of the grid stays");
    }

    @Test
    void nowhereLeftToPutItCraftsNothing(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 0);
        fillInventory(p.getInventory());
        p.inventoryMenu.getSlot(BackpackQuickMoveHelper.OFFHAND).set(new ItemStack(Items.SHIELD));
        loadGrid(p.inventoryMenu);
        shiftClickResult(p);

        assertEquals(0, inInventory(p, Items.STICK));
        assertEquals(CRAFTS, p.inventoryMenu.getSlot(1).getItem().getCount(), "grid must not be consumed");
    }

    @Test
    void aFullInventoryOverflowsIntoTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        fillInventory(p.getInventory());
        p.inventoryMenu.getSlot(BackpackQuickMoveHelper.OFFHAND).set(new ItemStack(Items.SHIELD));
        loadGrid(p.inventoryMenu);
        shiftClickResult(p);

        assertEquals(STICKS, inBackpack(p, Items.STICK), "overflow goes to the panels, not nowhere");
        assertEquals(0, inInventory(p, Items.STICK));
    }

    @Test
    void shiftClickingTheInventoryPrefersTheHotbar(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        p.inventoryMenu.getSlot(9).set(new ItemStack(Items.DIAMOND, 5));
        p.inventoryMenu.clicked(9, 0, ClickType.QUICK_MOVE, p);

        // The hotbar is where a shift-click has always put things; the backpack is extra
        // space, not a queue-jumper.
        assertEquals(5, inHotbar(p, Items.DIAMOND));
        assertEquals(0, inBackpack(p, Items.DIAMOND));
    }

    @Test
    void shiftClickingTheInventoryFillsTheBackpackOnceTheHotbarIsFull(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        fillHotbar(p.getInventory());
        p.inventoryMenu.getSlot(9).set(new ItemStack(Items.DIAMOND, 5));
        p.inventoryMenu.clicked(9, 0, ClickType.QUICK_MOVE, p);

        assertEquals(5, inBackpack(p, Items.DIAMOND));
        assertEquals(0, inInventory(p, Items.DIAMOND));
    }

    @Test
    void shiftClickingTheHotbarGoesToTheInventoryFirst(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        // Menu slot 36 is hotbar index 0.
        p.inventoryMenu.getSlot(36).set(new ItemStack(Items.DIAMOND, 5));
        p.inventoryMenu.clicked(36, 0, ClickType.QUICK_MOVE, p);

        assertEquals(5, inMainInventory(p, Items.DIAMOND));
        assertEquals(0, inBackpack(p, Items.DIAMOND));
    }

    @Test
    void closedPanelsRefuseQuickMoves(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        p.getData(ModAttachments.BACKPACK).setPanelsOpen(false);
        fillHotbar(p.getInventory());
        p.inventoryMenu.getSlot(9).set(new ItemStack(Items.DIAMOND, 5));
        p.inventoryMenu.clicked(9, 0, ClickType.QUICK_MOVE, p);

        // Closed is a real lock, not a hide: nothing may land where the player cannot see it.
        assertEquals(0, inBackpack(p, Items.DIAMOND));
        assertEquals(5, p.inventoryMenu.getSlot(9).getItem().getCount(), "the stack stays put");
    }

    @Test
    void closedPanelsTakeNoCraftingOverflow(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        p.getData(ModAttachments.BACKPACK).setPanelsOpen(false);
        fillInventory(p.getInventory());
        p.inventoryMenu.getSlot(BackpackQuickMoveHelper.OFFHAND).set(new ItemStack(Items.SHIELD));
        loadGrid(p.inventoryMenu);
        shiftClickResult(p);

        assertEquals(0, inBackpack(p, Items.STICK));
        assertEquals(CRAFTS, p.inventoryMenu.getSlot(1).getItem().getCount(), "grid must not be consumed");
    }

    @Test
    void closedPanelsLockTheSlots(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        BackpackSlot slot = (BackpackSlot) p.inventoryMenu.getSlot(BackpackQuickMoveHelper.FIRST_BACKPACK);
        assertTrue(slot.mayPlace(new ItemStack(Items.DIAMOND)));

        p.getData(ModAttachments.BACKPACK).setPanelsOpen(false);
        assertFalse(slot.mayPlace(new ItemStack(Items.DIAMOND)), "a closed backpack takes nothing");
        assertFalse(slot.mayPickup(p), "and gives nothing back until it is reopened");
    }

    @Test
    void lockedSlotsRefuseItems(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        BackpackSlot slot = (BackpackSlot) p.inventoryMenu.getSlot(BackpackQuickMoveHelper.FIRST_BACKPACK);
        assertTrue(slot.mayPlace(new ItemStack(Items.DIAMOND)));

        p.getData(ModAttachments.BACKPACK).setUnlocked(0);
        assertFalse(slot.mayPlace(new ItemStack(Items.DIAMOND)));
        assertFalse(slot.isActive());
    }
}
