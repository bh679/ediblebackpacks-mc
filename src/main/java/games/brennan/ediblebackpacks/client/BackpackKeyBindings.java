package games.brennan.ediblebackpacks.client;

import com.mojang.blaze3d.platform.InputConstants;
import games.brennan.ediblebackpacks.EdibleBackpacks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * The open/close hotkey — the keyboard twin of {@link BackpackToggleButton}. Ships
 * <b>unbound</b> ({@link InputConstants#UNKNOWN}); the player picks a key in Options →
 * Controls, under this mod's own category.
 *
 * <p>Handled twice over because the two places a player would press it read keys through
 * different paths: vanilla only ticks {@link KeyMapping}s while no screen is open
 * ({@link #onClientTick}), so the inventory screen — where the panels actually are — needs
 * {@link #onScreenKeyPressed}. Both land on {@link ClientPanelState#toggle()}, so the button
 * and the key can never disagree.</p>
 */
@EventBusSubscriber(modid = EdibleBackpacks.MOD_ID, value = Dist.CLIENT)
public final class BackpackKeyBindings {

    public static final String CATEGORY = "key.categories." + EdibleBackpacks.MOD_ID;
    public static final String TOGGLE_PANELS = "key." + EdibleBackpacks.MOD_ID + ".toggle_panels";

    /** Unbound out of the box: UNKNOWN is vanilla's "not assigned" key. */
    public static final KeyMapping TOGGLE = new KeyMapping(
        TOGGLE_PANELS, KeyConflictContext.UNIVERSAL, InputConstants.UNKNOWN, CATEGORY);

    private BackpackKeyBindings() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    /** In-world presses. {@code consumeClick} drains the queued presses vanilla recorded. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        boolean toggled = false;
        while (TOGGLE.consumeClick()) toggled = true;
        if (toggled && Minecraft.getInstance().player != null) ClientPanelState.toggle();
    }

    /**
     * Presses made with the inventory open, where vanilla never ticks key mappings. Matched
     * against the bound key directly, and left un-cancelled so anything else watching the
     * key still sees it.
     */
    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;
        // isActiveAndMatches has no unbound check of its own, and an unbound mapping holds
        // the UNKNOWN key — never let it swallow a keystroke while it has no key assigned.
        if (TOGGLE.isUnbound()) return;
        if (!TOGGLE.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) return;
        ClientPanelState.toggle();
        event.setCanceled(true);
    }
}
