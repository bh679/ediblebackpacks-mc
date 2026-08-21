package games.brennan.ediblebackpacks.network;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: the receiving player's unlocked backpack-slot count.
 * Attachments don't auto-sync; the client needs the count for slot
 * activation + panel rendering. Contents ride the vanilla container slot
 * sync of {@code InventoryMenu} — only the count travels here.
 */
public record SlotCountPayload(int unlocked) implements CustomPacketPayload {

    public static final Type<SlotCountPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(EdibleBackpacks.MOD_ID, "slot_count"));

    public static final StreamCodec<FriendlyByteBuf, SlotCountPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, packet) -> buf.writeVarInt(packet.unlocked),
            buf -> new SlotCountPayload(buf.readVarInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SlotCountPayload packet, IPayloadContext ctx) {
        // playToClient — runs on the client only; sets the client player's
        // attachment copy so slot activation matches the server.
        ctx.enqueueWork(() -> ClientPayloadHandler.applySlotCount(packet.unlocked()));
    }
}
