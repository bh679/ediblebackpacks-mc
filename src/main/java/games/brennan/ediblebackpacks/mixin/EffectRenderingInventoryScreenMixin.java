package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.client.BackpackEffectPlacement;
import games.brennan.ediblebackpacks.menu.EffectPlacement;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lifts the mob-effect stack above the right backpack panel.
 *
 * <p>Every y in the effect renderer is derived from {@code this.topPos}, and
 * NeoForge's {@link net.neoforged.neoforge.client.event.ScreenEvent.RenderInventoryMobEffects}
 * only exposes the horizontal offset — so the vertical move has to come from
 * redirecting those four reads. The tooltip hit-test in {@code renderEffects}
 * is one of them, which is what keeps hovering aligned with the icons.</p>
 */
@Mixin(EffectRenderingInventoryScreen.class)
public class EffectRenderingInventoryScreenMixin {

    @Redirect(
        method = {"renderEffects", "renderBackgrounds", "renderIcons", "renderLabels"},
        at = @At(value = "FIELD",
                 target = "Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;topPos:I",
                 opcode = Opcodes.GETFIELD))
    private int ediblebackpacks$effectStackTop(EffectRenderingInventoryScreen<?> screen) {
        EffectPlacement.Placement placement = BackpackEffectPlacement.resolve(screen);
        return placement == null ? screen.getGuiTop() : placement.top();
    }
}
