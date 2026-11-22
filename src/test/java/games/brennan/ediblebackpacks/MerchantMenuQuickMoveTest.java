package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The villager trade menu with the mixins applied, on an ephemeral server: the panels are there,
 * shift-clicks reach and leave them, and picking a trade pays from the backpack once the
 * inventory runs out. Vanilla's own trade routing must still come first.
 */
@ExtendWith(EphemeralTestServerProvider.class)
class MerchantMenuQuickMoveTest {

    private static final int INV_START = 3;
    private static final int HOTBAR_START = 30;
    private static final int FIRST_BACKPACK = 39;

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

    /** 5 emeralds (+ 2 sticks when {@code withB}) for a diamond. */
    private static MerchantMenu menu(FakePlayer p, boolean withB) {
        ClientSideMerchant merchant = new ClientSideMerchant(p);
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 5),
            withB ? Optional.of(new ItemCost(Items.STICK, 2)) : Optional.empty(),
            new ItemStack(Items.DIAMOND), 12, 1, 0.05f));
        merchant.overrideOffers(offers);
        return new MerchantMenu(1, p.getInventory(), merchant);
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

    private static void shiftClick(MerchantMenu menu, FakePlayer p, int index) {
        menu.clicked(index, 0, ClickType.QUICK_MOVE, p);
    }

    // --- tests -------------------------------------------------------------

    @Test
    void theMixinAppendsAllBackpackSlotsAfterThePlayerRows(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 0);
        assertEquals(FIRST_BACKPACK + BackpackLayout.MAX_SLOTS, menu(p, false).slots.size());
    }

    @Test
    void pickingATradePaysFromTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        backpack(p, 0, new ItemStack(Items.EMERALD, 20));
        backpack(p, 1, new ItemStack(Items.STICK, 4));
        MerchantMenu menu = menu(p, true);
        menu.tryMoveItems(0);

        assertEquals(20, menu.getSlot(0).getItem().getCount());
        assertTrue(menu.getSlot(0).getItem().is(Items.EMERALD));
        assertEquals(4, menu.getSlot(1).getItem().getCount());
        assertEquals(0, inBackpack(p, Items.EMERALD));
        assertTrue(menu.getSlot(2).getItem().is(Items.DIAMOND), "the trade is ready to take");
    }

    @Test
    void theInventoryPaysBeforeTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        p.getInventory().setItem(9, new ItemStack(Items.EMERALD, 50));
        backpack(p, 0, new ItemStack(Items.EMERALD, 30));
        MerchantMenu menu = menu(p, false);
        menu.tryMoveItems(0);

        assertEquals(64, menu.getSlot(0).getItem().getCount(), "a full stack, like vanilla");
        assertEquals(0, inInventory(p, Items.EMERALD));
        assertEquals(16, inBackpack(p, Items.EMERALD));
    }

    @Test
    void aClosedBackpackNeverPays(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        p.getData(ModAttachments.BACKPACK).setPanelsOpen(false);
        backpack(p, 0, new ItemStack(Items.EMERALD, 20));
        MerchantMenu menu = menu(p, false);
        menu.tryMoveItems(0);

        assertTrue(menu.getSlot(0).getItem().isEmpty());
        assertEquals(20, inBackpack(p, Items.EMERALD));
    }

    @Test
    void lockedSlotsNeverPay(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), 1);
        backpack(p, 1, new ItemStack(Items.EMERALD, 20));
        MerchantMenu menu = menu(p, false);
        menu.tryMoveItems(0);

        assertTrue(menu.getSlot(0).getItem().isEmpty());
    }

    @Test
    void shiftClickingTheBackpackMovesIntoTheInventory(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        backpack(p, 0, new ItemStack(Items.EMERALD, 7));
        MerchantMenu menu = menu(p, false);
        shiftClick(menu, p, FIRST_BACKPACK);

        assertEquals(7, inInventory(p, Items.EMERALD));
        assertEquals(0, inBackpack(p, Items.EMERALD));
    }

    @Test
    void shiftClickingTheInventoryKeepsVanillasShuffleFirst(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        p.getInventory().setItem(9, new ItemStack(Items.EMERALD, 7));   // menu slot INV_START
        MerchantMenu menu = menu(p, false);
        shiftClick(menu, p, INV_START);

        assertEquals(7, inInventory(p, Items.EMERALD), "main rows to hotbar, as vanilla");
        assertEquals(0, inBackpack(p, Items.EMERALD));
        assertTrue(menu.getSlot(HOTBAR_START).getItem().is(Items.EMERALD));
    }

    @Test
    void shiftClickingTheInventoryOverflowsIntoTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        for (int i = 0; i < 9; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        p.getInventory().setItem(9, new ItemStack(Items.EMERALD, 7));
        MerchantMenu menu = menu(p, false);
        shiftClick(menu, p, INV_START);

        assertEquals(7, inBackpack(p, Items.EMERALD), "hotbar full: the backpack is the overflow");
        assertTrue(menu.getSlot(INV_START).getItem().isEmpty());
    }

    @Test
    void shiftClickingThePaymentOverflowsIntoTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        MerchantMenu menu = menu(p, false);
        menu.getSlot(0).set(new ItemStack(Items.EMERALD, 9));
        shiftClick(menu, p, 0);

        assertEquals(9, inBackpack(p, Items.EMERALD));
        assertTrue(menu.getSlot(0).getItem().isEmpty());
    }

    @Test
    void aClosedBackpackTakesNoOverflow(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        p.getData(ModAttachments.BACKPACK).setPanelsOpen(false);
        for (int i = 0; i < 9; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        p.getInventory().setItem(9, new ItemStack(Items.EMERALD, 7));
        MerchantMenu menu = menu(p, false);
        shiftClick(menu, p, INV_START);

        assertEquals(0, inBackpack(p, Items.EMERALD));
        assertEquals(7, inInventory(p, Items.EMERALD));
    }

    @Test
    void closingWithAFullInventoryReturnsPaymentToTheBackpack(MinecraftServer server) throws Exception {
        FakePlayer p = player(level(server), BackpackLayout.MAX_SLOTS);
        backpack(p, 0, new ItemStack(Items.EMERALD, 20));
        MerchantMenu menu = menu(p, false);
        menu.tryMoveItems(0);
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        menu.removed(p);

        assertEquals(20, inBackpack(p, Items.EMERALD), "nothing dropped on the floor");
        assertTrue(menu.getSlot(0).getItem().isEmpty());
    }
}
