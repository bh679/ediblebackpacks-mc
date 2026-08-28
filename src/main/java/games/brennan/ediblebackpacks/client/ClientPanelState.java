package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.network.PanelOpenPayload;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only screen-state probe. Loaded lazily (guarded by
 * {@code level().isClientSide} at every call site) so it never classloads on
 * a dedicated server.
 */
public final class ClientPanelState {

    private ClientPanelState() {}

    /** True while the player has the panels open (the toggle button's state). */
    public static boolean panelsOpen() {
        Player player = Minecraft.getInstance().player;
        return player == null || player.getData(ModAttachments.BACKPACK).panelsOpen();
    }

    /**
     * Flips the panels open/closed: the local attachment first, so the next frame draws the
     * new state without waiting on a round trip, then the server, which owns the lock the
     * slots actually enforce.
     */
    public static void toggle() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean open = !player.getData(ModAttachments.BACKPACK).panelsOpen();
        player.getData(ModAttachments.BACKPACK).setPanelsOpen(open);
        PacketDistributor.sendToServer(new PanelOpenPayload(open));
    }

    /** True while the survival inventory's crafting recipe book is open. */
    public static boolean recipeBookOpen() {
        return Minecraft.getInstance().screen instanceof InventoryScreen screen
            && screen.getRecipeBookComponent().isVisible();
    }
}
