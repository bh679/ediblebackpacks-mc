package games.brennan.ediblebackpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.WorkstationQuickMove;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack panels on the loom (0 banner, 1 dye, 2 pattern, 3 result, 4..39 inventory + hotbar). Each input slot's own mayPlace sorts a panel click into the right one.
 * Vanilla's quick-move uses fixed ranges and keeps running; see {@link WorkstationQuickMove}.
 */
@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin {

    private static final int RESULT = 3;
    private static final int INV_START = 4;
    private static final int INV_END = 40;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(int containerId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    /** A panel slot: into the station the way vanilla feeds it from the inventory, then the inventory. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveFromBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = WorkstationQuickMove.fromBackpack(
            ((LoomMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index,
            INV_START, INV_END, s -> accessor.ediblebackpacks$invokeMoveItemStackTo(s, 0, 3, false), true);
        if (result != null) cir.setReturnValue(result);
    }

    /** Only once vanilla gave up on a station or inventory slot does the backpack take it. */
    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void ediblebackpacks$overflowIntoBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack moved = WorkstationQuickMove.overflowIntoBackpack(
            ((LoomMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index, RESULT, INV_END);
        if (moved != null) cir.setReturnValue(moved);
    }

    /** Vanilla's output move (ordinal 0, the only reverse = true move out of the output slot). */
    @WrapOperation(method = "quickMoveStack",
            at = @At(value = "INVOKE", ordinal = 0,
                     target = "Lnet/minecraft/world/inventory/LoomMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"))
    private boolean ediblebackpacks$resultIntoBackpack(LoomMenu self, ItemStack stack, int from, int to,
                                                       boolean reverse, Operation<Boolean> original) {
        if (from != INV_START || to != INV_END || !reverse) return original.call(self, stack, from, to, reverse);
        return WorkstationQuickMove.moveResult(self.slots, stack, INV_START, INV_END,
            s -> original.call(self, s, from, to, reverse));
    }
}
