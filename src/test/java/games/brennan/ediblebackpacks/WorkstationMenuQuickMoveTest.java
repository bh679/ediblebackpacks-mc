package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.SmithingMenu;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The workstation menus with the mixins applied, on an ephemeral server: crafting table,
 * furnace, anvil, smithing table and hopper all carry the panels, outputs can join a backpack
 * stack, panel shift-clicks feed the station, and vanilla's own routing still comes first.
 */
@ExtendWith(EphemeralTestServerProvider.class)
class WorkstationMenuQuickMoveTest {

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

    private static int inInventory(FakePlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
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

    private static void backpack(FakePlayer p, int slot, ItemStack stack) {
        p.getData(ModAttachments.BACKPACK).items().setStackInSlot(slot, stack);
    }

    private static void shiftClick(AbstractContainerMenu menu, FakePlayer p, int index) {
        menu.clicked(index, 0, ClickType.QUICK_MOVE, p);
    }

    /** Every inventory and hotbar slot full of stone, so vanilla has nowhere to put anything. */
    private static void fillInventory(FakePlayer p) {
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
    }

    private static CraftingMenu crafting(FakePlayer p) {
        return new CraftingMenu(1, p.getInventory(), ContainerLevelAccess.NULL);
    }

    private static final int CRAFT_BACKPACK = 46;
    private static final int FURNACE_BACKPACK = 39;
    private static final int ANVIL_BACKPACK = 39;
    private static final int HOPPER_BACKPACK = 41;

    // --- tests -------------------------------------------------------------

    @Test
    void everyWorkstationCarriesThePanels(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        assertEquals(CRAFT_BACKPACK + BackpackLayout.MAX_SLOTS, crafting(p).slots.size());
        assertEquals(FURNACE_BACKPACK + BackpackLayout.MAX_SLOTS, new FurnaceMenu(1, p.getInventory()).slots.size());
        assertEquals(ANVIL_BACKPACK + BackpackLayout.MAX_SLOTS, new AnvilMenu(1, p.getInventory()).slots.size());
        assertEquals(40 + BackpackLayout.MAX_SLOTS, new SmithingMenu(1, p.getInventory()).slots.size());
        assertEquals(HOPPER_BACKPACK + BackpackLayout.MAX_SLOTS, new HopperMenu(1, p.getInventory()).slots.size());
    }

    @Test
    void craftResultJoinsTheBackpackStack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 3, new ItemStack(Items.OAK_PLANKS, 10));
        CraftingMenu menu = crafting(p);
        menu.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 4));
        shiftClick(menu, p, 0);
        assertEquals(14, p.getData(ModAttachments.BACKPACK).items().getStackInSlot(3).getCount());
        assertEquals(0, inInventory(p, Items.OAK_PLANKS), "no new inventory stack");
    }

    @Test
    void craftResultGoesToTheBackpackWhenTheInventoryIsFull(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        fillInventory(p);
        CraftingMenu menu = crafting(p);
        menu.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 4));
        shiftClick(menu, p, 0);
        assertEquals(4, inBackpack(p, Items.OAK_PLANKS));
        assertTrue(menu.getSlot(0).getItem().isEmpty());
    }

    @Test
    void closedBackpackTakesNoCraftResult(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        p.getData(ModAttachments.BACKPACK).setPanelsOpen(false);
        fillInventory(p);
        CraftingMenu menu = crafting(p);
        menu.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 4));
        shiftClick(menu, p, 0);
        assertEquals(0, inBackpack(p, Items.OAK_PLANKS));
    }

    @Test
    void lockedSlotTakesNoCraftResult(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 0);
        fillInventory(p);
        CraftingMenu menu = crafting(p);
        menu.getSlot(0).set(new ItemStack(Items.OAK_PLANKS, 4));
        shiftClick(menu, p, 0);
        assertEquals(0, inBackpack(p, Items.OAK_PLANKS));
    }

    @Test
    void backpackFeedsTheCraftingGrid(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.OAK_LOG, 5));
        CraftingMenu menu = crafting(p);
        shiftClick(menu, p, CRAFT_BACKPACK);
        assertTrue(menu.getSlot(1).getItem().is(Items.OAK_LOG), "into the grid, like from the inventory");
        assertEquals(5, menu.getSlot(1).getItem().getCount());
        assertEquals(0, inBackpack(p, Items.OAK_LOG));
    }

    @Test
    void gridItemOverflowsIntoTheBackpackOnlyWhenTheInventoryIsFull(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        CraftingMenu menu = crafting(p);
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG, 5));
        shiftClick(menu, p, 1);
        assertEquals(5, inInventory(p, Items.OAK_LOG), "room in the inventory: vanilla's move stands");
        assertEquals(0, inBackpack(p, Items.OAK_LOG));

        fillInventory(p);
        menu.getSlot(1).set(new ItemStack(Items.OAK_LOG, 5));
        shiftClick(menu, p, 1);
        assertEquals(5, inBackpack(p, Items.OAK_LOG));
        assertTrue(menu.getSlot(1).getItem().isEmpty());
    }

    @Test
    void inventoryItemKeepsVanillasHotbarShuffle(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        CraftingMenu menu = crafting(p);
        for (int i = 1; i < 10; i++) menu.getSlot(i).set(new ItemStack(Items.STONE, 64));
        p.getInventory().setItem(9, new ItemStack(Items.DIRT, 10)); // first main row → menu slot 10
        shiftClick(menu, p, 10);
        assertEquals(10, inInventory(p, Items.DIRT), "grid full: vanilla moved it to the hotbar");
        assertEquals(0, inBackpack(p, Items.DIRT));
    }

    @Test
    void furnaceOutputJoinsTheBackpackStack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 2, new ItemStack(Items.IRON_INGOT, 5));
        FurnaceMenu menu = new FurnaceMenu(1, p.getInventory());
        menu.getSlot(2).set(new ItemStack(Items.IRON_INGOT, 3));
        shiftClick(menu, p, 2);
        assertEquals(8, p.getData(ModAttachments.BACKPACK).items().getStackInSlot(2).getCount());
        assertEquals(0, inInventory(p, Items.IRON_INGOT));
    }

    @Test
    void backpackFuelAndOreGoToTheirFurnaceSlots(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.COAL, 8));
        backpack(p, 1, new ItemStack(Items.RAW_IRON, 4));
        FurnaceMenu menu = new FurnaceMenu(1, p.getInventory());
        shiftClick(menu, p, FURNACE_BACKPACK);
        shiftClick(menu, p, FURNACE_BACKPACK + 1);
        assertTrue(menu.getSlot(1).getItem().is(Items.COAL), "fuel slot");
        assertTrue(menu.getSlot(0).getItem().is(Items.RAW_IRON), "input slot");
    }

    @Test
    void backpackFeedsTheAnvil(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.IRON_SWORD));
        AnvilMenu menu = new AnvilMenu(1, p.getInventory());
        shiftClick(menu, p, ANVIL_BACKPACK);
        assertTrue(menu.getSlot(0).getItem().is(Items.IRON_SWORD));
        assertEquals(0, inBackpack(p, Items.IRON_SWORD));
    }

    @Test
    void hopperEmptiesIntoTheInventoryNotTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        HopperMenu menu = new HopperMenu(1, p.getInventory());
        menu.getSlot(0).set(new ItemStack(Items.DIRT, 12));
        shiftClick(menu, p, 0);
        assertEquals(12, inInventory(p, Items.DIRT));
        assertEquals(0, inBackpack(p, Items.DIRT));
    }

    @Test
    void backpackFeedsTheHopper(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.DIRT, 12));
        HopperMenu menu = new HopperMenu(1, p.getInventory());
        shiftClick(menu, p, HOPPER_BACKPACK);
        assertTrue(menu.getSlot(0).getItem().is(Items.DIRT));
        assertEquals(0, inBackpack(p, Items.DIRT));
    }
}
