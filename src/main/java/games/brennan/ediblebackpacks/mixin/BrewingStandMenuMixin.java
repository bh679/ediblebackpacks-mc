package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.WorkstationQuickMove;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack panels on the brewing stand (0..2 bottles, 3 ingredient, 4 fuel, 5..40 inventory + hotbar). A panel click tries fuel, then the ingredient, then the bottles, vanilla's order, with each slot's own mayPlace deciding. Bottles go one per call (vanilla's empty-slot pass stops at the first slot) and the click loop repeats, so the rest only reaches the inventory once the station takes nothing. Potions do not stack, so the bottle moves are not wrapped.
 * Vanilla's quick-move uses fixed ranges and keeps running; see {@link WorkstationQuickMove}.
 */
@Mixin(BrewingStandMenu.class)
public abstract class BrewingStandMenuMixin {

    private static final int RESULT = -1;
    private static final int INV_START = 5;
    private static final int INV_END = 41;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V", at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(int containerId, Inventory playerInventory, Container container, ContainerData data, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    /** A panel slot: into the station the way vanilla feeds it from the inventory, then the inventory. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveFromBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = WorkstationQuickMove.fromBackpack(
            ((BrewingStandMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index,
            INV_START, INV_END, s -> {
                boolean moved = accessor.ediblebackpacks$invokeMoveItemStackTo(s, 4, 5, false);
                if (!s.isEmpty()) moved |= accessor.ediblebackpacks$invokeMoveItemStackTo(s, 3, 4, false);
                if (!s.isEmpty()) moved |= accessor.ediblebackpacks$invokeMoveItemStackTo(s, 0, 3, false);
                return moved;
            }, false);
        if (result != null) cir.setReturnValue(result);
    }

    /** Only once vanilla gave up on a station or inventory slot does the backpack take it. */
    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void ediblebackpacks$overflowIntoBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack moved = WorkstationQuickMove.overflowIntoBackpack(
            ((BrewingStandMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index, RESULT, INV_END);
        if (moved != null) cir.setReturnValue(moved);
    }
}
