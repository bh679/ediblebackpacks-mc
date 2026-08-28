package games.brennan.ediblebackpacks.network;

import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Client-side payload application, isolated in its own class so the client
 * classes it touches are only resolved when a payload actually arrives on the
 * client (the standard NeoForge split-handler pattern).
 */
final class ClientPayloadHandler {

    private ClientPayloadHandler() {}

    static void applySlotCount(int unlocked) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getData(ModAttachments.BACKPACK).setUnlocked(unlocked);
        }
    }

    static void applyPanelOpen(boolean open) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getData(ModAttachments.BACKPACK).setPanelsOpen(open);
        }
    }
}
