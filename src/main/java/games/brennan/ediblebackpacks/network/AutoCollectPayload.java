package games.brennan.ediblebackpacks.network;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.config.AutoCollectMode;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: the sending player's auto-collect choice. The setting lives in the
 * player's CLIENT config, so it is sent on login and whenever it changes
 * ({@code client/ClientAutoCollect}); the server keeps it on the attachment, where the
 * pickup mixin reads it.
 */
public record AutoCollectPayload(AutoCollectMode mode) implements CustomPacketPayload {

    public static final Type<AutoCollectPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(EdibleBackpacks.MOD_ID, "auto_collect"));

    public static final StreamCodec<FriendlyByteBuf, AutoCollectPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, packet) -> buf.writeUtf(packet.mode.name()),
            buf -> new AutoCollectPayload(AutoCollectMode.byName(buf.readUtf(32)))
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleServer(AutoCollectPayload packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ctx.player().getData(ModAttachments.BACKPACK).setAutoCollect(packet.mode()));
    }
}
