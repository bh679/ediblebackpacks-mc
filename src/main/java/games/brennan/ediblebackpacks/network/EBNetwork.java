package games.brennan.ediblebackpacks.network;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
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
    }

    /** Push the player's current unlocked-slot count to their client. */
    public static void syncSlotCount(ServerPlayer player) {
        int unlocked = player.getData(ModAttachments.BACKPACK).unlocked();
        PacketDistributor.sendToPlayer(player, new SlotCountPayload(unlocked));
    }
}
