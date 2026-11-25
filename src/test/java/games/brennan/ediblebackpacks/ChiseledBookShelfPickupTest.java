package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.config.AutoCollectMode;
import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Taking a book off a chiseled bookshelf, driven through vanilla's <em>real</em>
 * {@code ChiseledBookShelfBlock.removeBook} with {@code ChiseledBookShelfBlockMixin} applied,
 * on the same ephemeral-server harness as {@link BackpackPickupTest}.
 */
@ExtendWith(EphemeralTestServerProvider.class)
class ChiseledBookShelfPickupTest {

    private static final BlockPos POS = BlockPos.ZERO;

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
        return server.overworld();
    }

    /** Fresh state: empty inventory, empty backpack, panels open, {@code unlocked} slots granted. */
    private static FakePlayer player(ServerLevel level, int unlocked) {
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        BackpackData data = data(player);
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) data.items().setStackInSlot(i, ItemStack.EMPTY);
        data.setUnlocked(unlocked);
        data.setPanelsOpen(true);
        data.setAutoCollect(AutoCollectMode.WHILE_OPEN);
        return player;
    }

    private static BackpackData data(Player player) {
        return player.getData(ModAttachments.BACKPACK);
    }

    private static void fillInventory(Inventory inv) {
        for (int i = 0; i < 36; i++) inv.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
    }

    /**
     * A shelf holding one book in slot 0. {@code removeItem} skips vanilla's {@code updateState},
     * which rewrites the placed block's state; there is no placed block here.
     */
    private static ChiseledBookShelfBlockEntity shelfWith(ItemStack book) {
        return new ChiseledBookShelfBlockEntity(POS, Blocks.CHISELED_BOOKSHELF.defaultBlockState()) {
            private ItemStack held = book;

            @Override
            public ItemStack getItem(int slot) {
                return slot == 0 ? held : ItemStack.EMPTY;
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                ItemStack taken = getItem(slot);
                if (slot == 0) held = ItemStack.EMPTY;
                return taken;
            }
        };
    }

    /** Vanilla's private {@code removeBook(level, pos, player, shelf, slot)}. */
    private static void takeBook(ServerLevel level, Player player, ChiseledBookShelfBlockEntity shelf) throws Exception {
        Method m = ChiseledBookShelfBlock.class.getDeclaredMethod("removeBook",
            Level.class, BlockPos.class, Player.class, ChiseledBookShelfBlockEntity.class, int.class);
        m.setAccessible(true);
        m.invoke(null, level, POS, player, shelf, 0);
    }

    private static int inInventory(Player p, Item item) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) n += inv.getItem(i).getCount();
        return n;
    }

    private static int inBackpack(Player p, Item item) {
        int n = 0;
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            ItemStack s = data(p).items().getStackInSlot(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    // --- tests -------------------------------------------------------------

    @Test
    void fullInventoryPutsTheBookInAnOpenBackpackSlot(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);
        fillInventory(p.getInventory());
        ChiseledBookShelfBlockEntity shelf = shelfWith(new ItemStack(Items.BOOK));

        takeBook(level, p, shelf);

        assertTrue(data(p).items().getStackInSlot(0).is(Items.BOOK), "lands in the first backpack slot");
        assertTrue(shelf.getItem(0).isEmpty(), "the shelf gave it up");
    }

    @Test
    void enchantedBookToo(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 1);
        fillInventory(p.getInventory());

        takeBook(level, p, shelfWith(new ItemStack(Items.ENCHANTED_BOOK)));

        assertEquals(1, inBackpack(p, Items.ENCHANTED_BOOK));
    }

    @Test
    void inventoryWithRoomStillTakesTheBook(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);

        takeBook(level, p, shelfWith(new ItemStack(Items.BOOK)));

        assertEquals(1, inInventory(p, Items.BOOK), "vanilla's own add, unchanged");
        assertEquals(0, inBackpack(p, Items.BOOK));
    }

    @Test
    void closedPanelsDropTheBook(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);
        fillInventory(p.getInventory());
        data(p).setPanelsOpen(false);

        takeBook(level, p, shelfWith(new ItemStack(Items.BOOK)));

        assertEquals(0, inBackpack(p, Items.BOOK), "closed is a lock under WHILE_OPEN");
        assertEquals(0, inInventory(p, Items.BOOK));
    }

    @Test
    void fullBackpackDropsTheBook(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 1);
        fillInventory(p.getInventory());
        data(p).items().setStackInSlot(0, new ItemStack(Items.DIRT, 64));

        takeBook(level, p, shelfWith(new ItemStack(Items.BOOK)));

        assertEquals(0, inBackpack(p, Items.BOOK));
        assertEquals(0, inInventory(p, Items.BOOK));
    }

    @Test
    void lockedSlotsAreNotUsed(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 0);
        fillInventory(p.getInventory());

        takeBook(level, p, shelfWith(new ItemStack(Items.BOOK)));

        assertEquals(0, inBackpack(p, Items.BOOK));
    }

    @Test
    void alwaysModeCollectsWithPanelsClosed(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);
        fillInventory(p.getInventory());
        data(p).setPanelsOpen(false);
        data(p).setAutoCollect(AutoCollectMode.ALWAYS);

        takeBook(level, p, shelfWith(new ItemStack(Items.BOOK)));

        assertEquals(1, inBackpack(p, Items.BOOK), "same rule as a ground pickup");
    }
}
