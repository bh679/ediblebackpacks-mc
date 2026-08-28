package games.brennan.ediblebackpacks.network;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Whether the receiving/sending player's backpack panels are open. Travels BOTH ways:
 * client → server when the toggle button beside the recipe book is pressed, server →
 * client on the usual sync points so a fresh login starts in the state the player left.
 *
 * <p>The flag has to reach the server because closing the panels shuts the backpack for
 * real — {@code menu/BackpackSlot} refuses items while it is closed, and quick-move runs
 * server-side.</p>
 */
public record PanelOpenPayload(boolean open) implements CustomPacketPayload {

    public static final Type<PanelOpenPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(EdibleBackpacks.MOD_ID, "panel_open"));

    public static final StreamCodec<FriendlyByteBuf, PanelOpenPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, packet) -> buf.writeBoolean(packet.open),
            buf -> new PanelOpenPayload(buf.readBoolean())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** playToServer: the sending player toggled their own panels. */
    public static void handleServer(PanelOpenPayload packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            player.getData(ModAttachments.BACKPACK).setPanelsOpen(packet.open());
        });
    }

    /** playToClient: adopt the server's state on login/respawn/dimension change. */
    public static void handleClient(PanelOpenPayload packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPayloadHandler.applyPanelOpen(packet.open()));
    }
}
