package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
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
 * Ground pickups driven through a <em>real</em> {@code ItemEntity.playerTouch} with
 * {@code ItemEntityMixin} applied, on the same ephemeral-server harness as
 * {@link InventoryMenuQuickMoveTest}.
 */
@ExtendWith(EphemeralTestServerProvider.class)
class BackpackPickupTest {

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
        if (server.getWorldData() instanceof net.minecraft.world.level.storage.ServerLevelData data) {
            data.setGameType(net.minecraft.world.level.GameType.ADVENTURE);
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
        return player;
    }

    private static BackpackData data(FakePlayer player) {
        return player.getData(ModAttachments.BACKPACK);
    }

    private static void fillInventory(Inventory inv) {
        for (int i = 0; i < 36; i++) inv.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
    }

    /** Drops {@code stack} at the player's feet and lets them touch it; returns the entity. */
    private static ItemEntity pickUp(ServerLevel level, FakePlayer player, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
        entity.setNoPickUpDelay();
        entity.playerTouch(player);
        return entity;
    }

    private static int inInventory(FakePlayer p, Item item) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(item)) n += inv.getItem(i).getCount();
        return n;
    }

    private static int inBackpack(FakePlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            ItemStack s = data(p).items().getStackInSlot(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    // --- full inventory overflows into the backpack ------------------------

    @Test
    void fullInventoryOverflowsIntoEmptyBackpackSlot(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);
        fillInventory(p.getInventory());

        ItemEntity entity = pickUp(level, p, new ItemStack(Items.DIRT, 10));

        assertEquals(10, data(p).items().getStackInSlot(0).getCount(), "lands in the first backpack slot");
        assertTrue(data(p).items().getStackInSlot(0).is(Items.DIRT));
        assertTrue(entity.isRemoved(), "the whole stack was taken, so the item entity is gone");
    }

    @Test
    void leftoverStaysOnTheGroundWhenBackpackIsFullToo(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 1);
        fillInventory(p.getInventory());
        data(p).items().setStackInSlot(0, new ItemStack(Items.DIRT, 60));

        ItemEntity entity = pickUp(level, p, new ItemStack(Items.DIRT, 10));

        assertEquals(64, inBackpack(p, Items.DIRT));
        assertFalse(entity.isRemoved());
        assertEquals(6, entity.getItem().getCount(), "only what fit was taken");
    }

    // --- matching backpack stacks are topped up first ----------------------

    @Test
    void matchingBackpackStackIsToppedUpEvenWithInventoryRoom(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);
        data(p).items().setStackInSlot(1, new ItemStack(Items.DIRT, 10));

        ItemEntity entity = pickUp(level, p, new ItemStack(Items.DIRT, 20));

        assertEquals(30, data(p).items().getStackInSlot(1).getCount(), "the parked stack keeps collecting");
        assertEquals(0, inInventory(p, Items.DIRT), "nothing went to the inventory");
        assertTrue(data(p).items().getStackInSlot(0).isEmpty(), "no new stack opened ahead of it");
        assertTrue(entity.isRemoved());
    }

    @Test
    void topUpOverflowGoesToInventoryBeforeEmptyBackpackSlots(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);
        data(p).items().setStackInSlot(0, new ItemStack(Items.DIRT, 60));

        pickUp(level, p, new ItemStack(Items.DIRT, 10));

        assertEquals(64, data(p).items().getStackInSlot(0).getCount());
        assertEquals(6, inInventory(p, Items.DIRT), "the rest goes where vanilla puts it");
        assertTrue(data(p).items().getStackInSlot(1).isEmpty());
    }

    @Test
    void stackWithDifferentComponentsIsNotToppedUp(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);
        ItemStack named = new ItemStack(Items.DIRT, 10);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Special"));
        data(p).items().setStackInSlot(0, named);

        pickUp(level, p, new ItemStack(Items.DIRT, 5));

        assertEquals(10, data(p).items().getStackInSlot(0).getCount());
        assertEquals(5, inInventory(p, Items.DIRT));
    }

    @Test
    void plainPickupWithRoomAndNoMatchingStackIsVanilla(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);

        pickUp(level, p, new ItemStack(Items.DIRT, 5));

        assertEquals(5, inInventory(p, Items.DIRT));
        assertEquals(0, inBackpack(p, Items.DIRT));
    }

    // --- locks -------------------------------------------------------------

    @Test
    void closedPanelsCollectNothing(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 3);
        fillInventory(p.getInventory());
        data(p).items().setStackInSlot(0, new ItemStack(Items.DIRT, 10));
        data(p).setPanelsOpen(false);

        ItemEntity entity = pickUp(level, p, new ItemStack(Items.DIRT, 10));

        assertEquals(10, inBackpack(p, Items.DIRT), "closed is a lock, top-up included");
        assertFalse(entity.isRemoved());
        assertEquals(10, entity.getItem().getCount());
    }

    @Test
    void lockedSlotsCollectNothing(MinecraftServer server) throws Exception {
        ServerLevel level = level(server);
        FakePlayer p = player(level, 1);
        fillInventory(p.getInventory());
        data(p).items().setStackInSlot(0, new ItemStack(Items.STONE, 64));

        ItemEntity entity = pickUp(level, p, new ItemStack(Items.DIRT, 10));

        assertEquals(0, inBackpack(p, Items.DIRT), "slot 1 is still locked");
        assertEquals(10, entity.getItem().getCount());
    }
}
