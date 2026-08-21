package games.brennan.ediblebackpacks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Client-only screen-state probe. Loaded lazily (guarded by
 * {@code level().isClientSide} at every call site) so it never classloads on
 * a dedicated server.
 */
public final class ClientPanelState {

    private ClientPanelState() {}

    /** True while the survival inventory's crafting recipe book is open. */
    public static boolean recipeBookOpen() {
        return Minecraft.getInstance().screen instanceof InventoryScreen screen
            && screen.getRecipeBookComponent().isVisible();
    }
}
