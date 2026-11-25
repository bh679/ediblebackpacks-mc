package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.WorkstationQuickMove;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack panels on the crafter (0..8 grid, 9..44 inventory + hotbar, then a display-only result slot). Its quick-move uses fixed ranges, so this is the workstation pattern with the grid as the input; a disabled slot refuses through its own mayPlace.
 * Vanilla's quick-move uses fixed ranges and keeps running; see {@link WorkstationQuickMove}.
 */
@Mixin(CrafterMenu.class)
public abstract class CrafterMenuMixin {

    private static final int RESULT = -1;
    private static final int INV_START = 9;
    private static final int INV_END = 45;

    @Shadow @Final private Player player;

    /** Both constructors build every slot and neither delegates, so each gets one TAIL. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, player);
    }

    /** A panel slot: into the station the way vanilla feeds it from the inventory, then the inventory. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveFromBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = WorkstationQuickMove.fromBackpack(
            ((CrafterMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index,
            INV_START, INV_END, s -> accessor.ediblebackpacks$invokeMoveItemStackTo(s, 0, 9, false), true);
        if (result != null) cir.setReturnValue(result);
    }

    /** Only once vanilla gave up on a station or inventory slot does the backpack take it. */
    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void ediblebackpacks$overflowIntoBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack moved = WorkstationQuickMove.overflowIntoBackpack(
            ((CrafterMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index, RESULT, INV_END);
        if (moved != null) cir.setReturnValue(moved);
    }
}
