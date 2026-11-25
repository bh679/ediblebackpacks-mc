package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.StonecutterMenu;
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

    // --- 0.28: the remaining stations ---------------------------------------

    private static int count(AbstractContainerMenu menu, int slot, Item item) {
        ItemStack s = menu.getSlot(slot).getItem();
        return s.is(item) ? s.getCount() : 0;
    }

    @Test
    void everyRemainingStationCarriesThePanelsOnce(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        assertEquals(38 + BackpackLayout.MAX_SLOTS, new EnchantmentMenu(1, p.getInventory()).slots.size());
        assertEquals(39 + BackpackLayout.MAX_SLOTS, new GrindstoneMenu(1, p.getInventory()).slots.size());
        assertEquals(38 + BackpackLayout.MAX_SLOTS, new StonecutterMenu(1, p.getInventory()).slots.size());
        assertEquals(40 + BackpackLayout.MAX_SLOTS, new LoomMenu(1, p.getInventory()).slots.size());
        assertEquals(39 + BackpackLayout.MAX_SLOTS, new CartographyTableMenu(1, p.getInventory()).slots.size());
        assertEquals(41 + BackpackLayout.MAX_SLOTS, new BrewingStandMenu(1, p.getInventory()).slots.size());
        assertEquals(46 + BackpackLayout.MAX_SLOTS, new CrafterMenu(1, p.getInventory()).slots.size(), "crafter: one set, after its result slot");
        assertEquals(45 + BackpackLayout.MAX_SLOTS, new DispenserMenu(1, p.getInventory()).slots.size());
    }

    @Test
    void enchantingTableTakesLapisAndOneItem(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.LAPIS_LAZULI, 10));
        backpack(p, 1, new ItemStack(Items.BOOK, 5));
        EnchantmentMenu menu = new EnchantmentMenu(1, p.getInventory());
        shiftClick(menu, p, 38);
        shiftClick(menu, p, 39);
        assertEquals(10, count(menu, 1, Items.LAPIS_LAZULI));
        assertEquals(1, count(menu, 0, Items.BOOK), "one book, as from the inventory");
        assertEquals(4, inBackpack(p, Items.BOOK), "the rest stays in the backpack");
        assertEquals(0, inInventory(p, Items.BOOK));
    }

    @Test
    void grindstoneFeedsAndResultFallsIntoTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.IRON_SWORD));
        GrindstoneMenu menu = new GrindstoneMenu(1, p.getInventory());
        shiftClick(menu, p, 39);
        assertTrue(menu.getSlot(0).getItem().is(Items.IRON_SWORD));

        GrindstoneMenu full = new GrindstoneMenu(1, p.getInventory());
        fillInventory(p);
        full.getSlot(2).set(new ItemStack(Items.GOLDEN_SWORD));
        shiftClick(full, p, 2);
        assertEquals(1, inBackpack(p, Items.GOLDEN_SWORD), "full inventory: the result lands in the backpack");
    }

    @Test
    void closedBackpackTakesNoGrindstoneResult(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        p.getData(ModAttachments.BACKPACK).setPanelsOpen(false);
        fillInventory(p);
        GrindstoneMenu menu = new GrindstoneMenu(1, p.getInventory());
        menu.getSlot(2).set(new ItemStack(Items.GOLDEN_SWORD));
        shiftClick(menu, p, 2);
        assertEquals(0, inBackpack(p, Items.GOLDEN_SWORD));
    }

    @Test
    void stonecutterTakesOnlyCuttableItemsAndResultJoinsTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.STONE, 5));
        backpack(p, 1, new ItemStack(Items.DIRT, 5));
        backpack(p, 2, new ItemStack(Items.STONE_BRICKS, 10));
        StonecutterMenu menu = new StonecutterMenu(1, p.getInventory());
        shiftClick(menu, p, 38);
        shiftClick(menu, p, 39);
        assertEquals(5, count(menu, 0, Items.STONE));
        assertEquals(5, inInventory(p, Items.DIRT), "no recipe: past the input, into the inventory");

        menu.getSlot(1).set(new ItemStack(Items.STONE_BRICKS, 1));
        shiftClick(menu, p, 1);
        assertEquals(11, p.getData(ModAttachments.BACKPACK).items().getStackInSlot(2).getCount());
    }

    @Test
    void loomSortsBannerAndDyeAndResultJoinsTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.RED_DYE, 3));
        backpack(p, 1, new ItemStack(Items.WHITE_BANNER, 1));
        backpack(p, 2, new ItemStack(Items.BLUE_BANNER, 2));
        LoomMenu menu = new LoomMenu(1, p.getInventory());
        shiftClick(menu, p, 40);
        shiftClick(menu, p, 41);
        assertEquals(1, count(menu, 0, Items.WHITE_BANNER));
        assertEquals(3, count(menu, 1, Items.RED_DYE));

        LoomMenu empty = new LoomMenu(1, p.getInventory());
        empty.getSlot(3).set(new ItemStack(Items.BLUE_BANNER, 1));
        shiftClick(empty, p, 3);
        assertEquals(3, p.getData(ModAttachments.BACKPACK).items().getStackInSlot(2).getCount());
    }

    @Test
    void cartographyTableSortsAndResultJoinsTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.PAPER, 4));
        backpack(p, 1, new ItemStack(Items.MAP, 3));
        CartographyTableMenu menu = new CartographyTableMenu(1, p.getInventory());
        shiftClick(menu, p, 39);
        assertEquals(4, count(menu, 1, Items.PAPER), "paper slot; slot0=" + menu.getSlot(0).getItem() + " slot1=" + menu.getSlot(1).getItem() + " inv=" + inInventory(p, Items.PAPER));

        // The table clears its result whenever an input is empty, so give it both inputs first.
        CartographyTableMenu table = new CartographyTableMenu(1, p.getInventory());
        table.getSlot(0).set(new ItemStack(Items.FILLED_MAP));
        table.getSlot(1).set(new ItemStack(Items.PAPER));
        table.getSlot(2).set(new ItemStack(Items.MAP, 1));
        shiftClick(table, p, 2);
        assertEquals(4, p.getData(ModAttachments.BACKPACK).items().getStackInSlot(1).getCount(), "result joins the backpack stack");
        assertEquals(0, inInventory(p, Items.MAP));
    }

    @Test
    void brewingStandTakesFuelThenBottles(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.BLAZE_POWDER, 5));
        backpack(p, 1, new ItemStack(Items.GLASS_BOTTLE, 4));
        BrewingStandMenu menu = new BrewingStandMenu(1, p.getInventory());
        shiftClick(menu, p, 41);
        shiftClick(menu, p, 42);
        assertEquals(5, count(menu, 4, Items.BLAZE_POWDER), "fuel slot first");
        assertEquals(1, count(menu, 0, Items.GLASS_BOTTLE));
        assertEquals(1, count(menu, 2, Items.GLASS_BOTTLE));
    }

    @Test
    void crafterAndDispenserTradeWithTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 9);
        backpack(p, 0, new ItemStack(Items.DIRT, 12));
        CrafterMenu crafter = new CrafterMenu(1, p.getInventory());
        shiftClick(crafter, p, 46);
        assertEquals(12, count(crafter, 0, Items.DIRT));

        backpack(p, 1, new ItemStack(Items.ARROW, 20));
        DispenserMenu dispenser = new DispenserMenu(1, p.getInventory());
        shiftClick(dispenser, p, 46);
        assertEquals(20, count(dispenser, 0, Items.ARROW));
        shiftClick(dispenser, p, 0);
        assertEquals(20, inInventory(p, Items.ARROW), "out of the dispenser: inventory, not the backpack");
        assertEquals(0, inBackpack(p, Items.ARROW));
    }
}
