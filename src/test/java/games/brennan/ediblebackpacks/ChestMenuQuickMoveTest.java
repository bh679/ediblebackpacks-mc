package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.menu.BackpackSlot;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
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
 * The chest-shaped half of {@link InventoryMenuQuickMoveTest}: a <em>real</em> {@code ChestMenu}
 * (and {@code ShulkerBoxMenu}) with the mixins applied, on an ephemeral server, so a shift-click
 * here is the call the server really runs.
 *
 * <p>What it pins is the routing contract: inside a chest a shift-click means "deposit", and the
 * backpack is extra space behind the vanilla destination, never in front of it. Intercepting is
 * not cosmetic — vanilla's own chest quick-move ends its player-side range at
 * {@code slots.size()}, which now runs into the panels, so leaving it alone would push chest
 * contents straight past the player's inventory and into a backpack that might be shut.</p>
 */
@ExtendWith(EphemeralTestServerProvider.class)
class ChestMenuQuickMoveTest {

    private static final int ROWS = 6;
    private static final int CHEST = ROWS * 9;      // 54
    private static final int INV_START = CHEST;     // player rows follow the container
    private static final int PLAYER_END = CHEST + 36;
    private static final int FIRST_BACKPACK = PLAYER_END;

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
            // Same as the inventory suite: the spawn search needs a ticking chunk system, and
            // the overworld ServerLevel is registered before that runs, which is all we need.
        }
        if (server.getWorldData() instanceof net.minecraft.world.level.storage.ServerLevelData data) {
            data.setGameType(net.minecraft.world.level.GameType.ADVENTURE);
        }
        return server.overworld();
    }

    /** The FakePlayer is cached per level, so every field a test can change is reset here. */
    private static FakePlayer player(ServerLevel level, int unlocked) {
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        BackpackData data = player.getData(ModAttachments.BACKPACK);
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) data.items().setStackInSlot(i, ItemStack.EMPTY);
        data.setUnlocked(unlocked);
        data.setPanelsOpen(true);
        return player;
    }

    private static ChestMenu chest(FakePlayer player) {
        return ChestMenu.sixRows(1, player.getInventory(), new SimpleContainer(CHEST));
    }

    private static void fill(net.minecraft.world.Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private static void fillInventory(FakePlayer p) {
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
    }

    private static int count(net.minecraft.world.Container container, Item item) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static int inInventory(FakePlayer p, Item item) {
        return count(p.getInventory(), item);
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

    private static void shiftClick(ChestMenu menu, FakePlayer p, int index) {
        menu.clicked(index, 0, ClickType.QUICK_MOVE, p);
    }

    // --- tests -------------------------------------------------------------

    @Test
    void theMixinAppendsAllBackpackSlotsToAChest(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 0);
        assertEquals(CHEST + 36 + BackpackLayout.MAX_SLOTS, chest(p).slots.size());
    }

    @Test
    void theMixinAppendsAllBackpackSlotsToAShulkerBox(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 0);
        ShulkerBoxMenu menu = new ShulkerBoxMenu(1, p.getInventory(), new SimpleContainer(27));
        assertEquals(27 + 36 + BackpackLayout.MAX_SLOTS, menu.slots.size());
    }

    @Test
    void chestContentsOverflowIntoTheBackpackWhenTheInventoryIsFull(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        fillInventory(p);
        ChestMenu menu = chest(p);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 5));
        shiftClick(menu, p, 0);

        assertEquals(5, inBackpack(p, Items.DIAMOND), "the panels are the overflow, not nowhere");
        assertTrue(menu.getSlot(0).getItem().isEmpty());
    }

    @Test
    void chestContentsStillPreferThePlayerInventory(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        ChestMenu menu = chest(p);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND, 5));
        shiftClick(menu, p, 0);

        // The backpack is extra space behind the vanilla destination, never in front of it.
        assertEquals(5, inInventory(p, Items.DIAMOND));
        assertEquals(0, inBackpack(p, Items.DIAMOND));
    }

    @Test
    void shiftClickingTheInventoryPrefersTheChest(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        ChestMenu menu = chest(p);
        p.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 5));  // menu slot INV_START
        shiftClick(menu, p, INV_START);

        assertEquals(5, count(menu.getContainer(), Items.DIAMOND));
        assertEquals(0, inBackpack(p, Items.DIAMOND));
    }

    @Test
    void shiftClickingTheInventoryOverflowsIntoTheBackpackWhenTheChestIsFull(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        ChestMenu menu = chest(p);
        fill(menu.getContainer());
        p.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 5));
        shiftClick(menu, p, INV_START);

        assertEquals(5, inBackpack(p, Items.DIAMOND));
        assertEquals(0, inInventory(p, Items.DIAMOND));
    }

    @Test
    void shiftClickingTheBackpackDepositsIntoTheChest(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        ChestMenu menu = chest(p);
        p.getData(ModAttachments.BACKPACK).items().setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        shiftClick(menu, p, FIRST_BACKPACK);

        // Inside a chest a shift-click has always meant "put it in the chest" — even with the
        // whole player inventory free, which is the case that would otherwise swallow it.
        assertEquals(5, count(menu.getContainer(), Items.DIAMOND));
        assertEquals(0, inBackpack(p, Items.DIAMOND));
        assertEquals(0, inInventory(p, Items.DIAMOND));
    }

    @Test
    void shiftClickingTheBackpackFallsBackToTheInventoryWhenTheChestIsFull(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        ChestMenu menu = chest(p);
        fill(menu.getContainer());
        p.getData(ModAttachments.BACKPACK).items().setStackInSlot(0, new ItemStack(Items.DIAMOND, 5));
        shiftClick(menu, p, FIRST_BACKPACK);

        assertEquals(5, inInventory(p, Items.DIAMOND));
        assertEquals(0, inBackpack(p, Items.DIAMOND));
    }

    @Test
    void closedPanelsTakeNoChestOverflow(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        p.getData(ModAttachments.BACKPACK).setPanelsOpen(false);
        ChestMenu menu = chest(p);
        fill(menu.getContainer());
        p.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 5));
        shiftClick(menu, p, INV_START);

        // Closed is a real lock in a chest too: nothing lands where the player cannot see it.
        assertEquals(0, inBackpack(p, Items.DIAMOND));
        assertEquals(5, p.getInventory().getItem(9).getCount(), "the stack stays put");
    }

    @Test
    void closedPanelsLockTheChestsCopyOfTheSlots(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        ChestMenu menu = chest(p);
        BackpackSlot slot = (BackpackSlot) menu.getSlot(FIRST_BACKPACK);
        assertTrue(slot.mayPlace(new ItemStack(Items.DIAMOND)));

        p.getData(ModAttachments.BACKPACK).setPanelsOpen(false);
        assertFalse(slot.mayPlace(new ItemStack(Items.DIAMOND)));
        assertFalse(slot.mayPickup(p));
    }

    @Test
    void theChestSeesTheSameBackpackContentsAsTheInventoryMenu(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        p.inventoryMenu.getSlot(46).set(new ItemStack(Items.DIAMOND, 3));

        // Both menus resolve the attachment live rather than capturing a handler, so a chest
        // opened afterwards is looking at the same storage, not a stale copy.
        assertEquals(3, chest(p).getSlot(FIRST_BACKPACK).getItem().getCount());
    }
}
