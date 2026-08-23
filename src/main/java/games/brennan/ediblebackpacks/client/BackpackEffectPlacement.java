package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.menu.EffectPlacement;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Keeps the vanilla mob-effect stack clear of the right backpack panel.
 *
 * <p>The horizontal half of the move goes through NeoForge's
 * {@link ScreenEvent.RenderInventoryMobEffects}; the vertical half has no event,
 * so {@code EffectRenderingInventoryScreenMixin} redirects the screen's
 * {@code topPos} reads here. Both sides call {@link #resolve} rather than
 * sharing state, so they cannot disagree about where the stack ended up.</p>
 */
@EventBusSubscriber(modid = EdibleBackpacks.MOD_ID, value = Dist.CLIENT)
public final class BackpackEffectPlacement {

    private BackpackEffectPlacement() {}

    @SubscribeEvent
    public static void onRenderInventoryMobEffects(ScreenEvent.RenderInventoryMobEffects event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        EffectPlacement.Placement placement = resolve(screen);
        if (placement == null) return;
        event.setHorizontalOffset(placement.left());
        event.setCompact(placement.compact());
    }

    /**
     * Where this screen's effect stack belongs, or {@code null} when the panels
     * are not in play and vanilla's own placement should stand.
     */
    public static EffectPlacement.Placement resolve(AbstractContainerScreen<?> screen) {
        if (!(screen instanceof InventoryScreen)) return null;
        // The recipe book hides the panels (and shoves the GUI sideways), so
        // while it is open there is nothing for the stack to dodge.
        if (ClientPanelState.recipeBookOpen()) return null;

        Player player = Minecraft.getInstance().player;
        if (player == null) return null;
        int unlocked = player.getData(ModAttachments.BACKPACK).unlocked();
        if (!EffectPlacement.rightPanelPresent(unlocked)) return null;

        int count = 0;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (ClientHooks.shouldRenderEffect(effect)) count++;
        }
        if (count <= 0) return null;

        int vanillaLeft = screen.getGuiLeft() + screen.getXSize() + 2;
        boolean vanillaCompact = screen.width - vanillaLeft < EffectPlacement.WIDE;
        return EffectPlacement.resolve(screen.getGuiLeft(), screen.getGuiTop(), screen.width,
                                       vanillaLeft, vanillaCompact, unlocked, count);
    }
}
