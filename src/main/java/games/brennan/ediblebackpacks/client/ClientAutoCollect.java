package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.config.AutoCollectMode;
import games.brennan.ediblebackpacks.config.EBClientConfig;
import games.brennan.ediblebackpacks.network.AutoCollectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Keeps the server's copy of the player's auto-collect choice in step with the CLIENT
 * config: sent on every login, and again whenever {@link #set} changes it (DT's Options
 * screen is the UI; standalone, it is {@code ediblebackpacks-client.toml}).
 *
 * <p>Every send checks the channel first, so a server with an older Edible Backpacks —
 * which has no such payload — is simply left on its default rather than disconnecting.</p>
 */
@EventBusSubscriber(modid = EdibleBackpacks.MOD_ID, value = Dist.CLIENT)
public final class ClientAutoCollect {

    private ClientAutoCollect() {}

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        send(event.getPlayer().connection, EBClientConfig.autoCollect());
    }

    /** Stores {@code mode} in the client config and tells the server, if connected. */
    public static void set(AutoCollectMode mode) {
        if (!EBClientConfig.SPEC.isLoaded()) return;
        EBClientConfig.AUTO_COLLECT.set(mode);
        EBClientConfig.SPEC.save();
        send(Minecraft.getInstance().getConnection(), mode);
    }

    private static void send(ClientPacketListener connection, AutoCollectMode mode) {
        if (connection == null || !connection.hasChannel(AutoCollectPayload.TYPE)) return;
        connection.send(new AutoCollectPayload(mode));
    }
}
