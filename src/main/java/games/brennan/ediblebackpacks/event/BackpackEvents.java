package games.brennan.ediblebackpacks.event;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.EdibleBackpacksApi;
import games.brennan.ediblebackpacks.config.EBConfig;
import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.network.EBNetwork;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import games.brennan.ediblebackpacks.storage.BackpackPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Game-bus events: death policy + client sync points.
 *
 * <p>Death reset (only when the resolved policy says so — standalone default
 * is persist): contents drop at the death site via {@link LivingDropsEvent}
 * and the slot count zeroes. With {@code keepInventory} on, the drops event
 * never fires, so a {@link PlayerEvent.Clone} fallback still zeroes the
 * clone (contents are lost rather than dropped in that corner — acceptable,
 * the policy is "you lose your backpack on death").</p>
 */
@EventBusSubscriber(modid = EdibleBackpacks.MOD_ID)
public final class BackpackEvents {

    private BackpackEvents() {}

    private static boolean shouldReset() {
        return BackpackPolicy.shouldResetOnDeath(
            EBConfig.resetMode(), EdibleBackpacksApi.hostDefaultResetOnDeath());
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!shouldReset()) return;

        BackpackData data = player.getData(ModAttachments.BACKPACK);
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            ItemStack stack = data.items().getStackInSlot(i);
            if (!stack.isEmpty()) {
                event.getDrops().add(new ItemEntity(
                    player.level(), player.getX(), player.getY(), player.getZ(), stack.copy()));
                data.items().setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        data.setUnlocked(0);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer clone)) return;
        if (!shouldReset()) return;

        // keepInventory fallback: LivingDropsEvent didn't fire, but the
        // copyOnDeath attachment carried the backpack over — zero it here.
        BackpackData data = clone.getData(ModAttachments.BACKPACK);
        if (data.unlocked() != 0 || !isEmpty(data)) {
            for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
                data.items().setStackInSlot(i, ItemStack.EMPTY);
            }
            data.setUnlocked(0);
        }
    }

    private static boolean isEmpty(BackpackData data) {
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            if (!data.items().getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    /** Both attachment-backed bits the client needs; neither auto-syncs. */
    private static void sync(ServerPlayer player) {
        EBNetwork.syncSlotCount(player);
        EBNetwork.syncPanelOpen(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }
}
