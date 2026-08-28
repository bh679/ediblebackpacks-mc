package games.brennan.ediblebackpacks.network;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Network registrar + send helpers. */
@EventBusSubscriber(modid = EdibleBackpacks.MOD_ID)
public final class EBNetwork {

    private EBNetwork() {}

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(EdibleBackpacks.MOD_ID).versioned("1");
        registrar.playToClient(SlotCountPayload.TYPE, SlotCountPayload.STREAM_CODEC, SlotCountPayload::handle);
        // One id, both directions: a payload may only be registered once, so the pair of
        // handlers rides a DirectionalPayloadHandler rather than two registrations.
        registrar.playBidirectional(PanelOpenPayload.TYPE, PanelOpenPayload.STREAM_CODEC,
            new DirectionalPayloadHandler<>(PanelOpenPayload::handleClient, PanelOpenPayload::handleServer));
    }

    /** Push the player's current unlocked-slot count to their client. */
    public static void syncSlotCount(ServerPlayer player) {
        int unlocked = player.getData(ModAttachments.BACKPACK).unlocked();
        PacketDistributor.sendToPlayer(player, new SlotCountPayload(unlocked));
    }

    /** Push the player's open/closed panel state to their client. */
    public static void syncPanelOpen(ServerPlayer player) {
        boolean open = player.getData(ModAttachments.BACKPACK).panelsOpen();
        PacketDistributor.sendToPlayer(player, new PanelOpenPayload(open));
    }
}
