package games.brennan.ediblebackpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.WorkstationQuickMove;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack panels on the stonecutter (0 input, 1 result, 2..37 inventory + hotbar). A panel click goes to the input only if it has a stonecutting recipe, vanilla's own test.
 * Vanilla's quick-move uses fixed ranges and keeps running; see {@link WorkstationQuickMove}.
 */
@Mixin(StonecutterMenu.class)
public abstract class StonecutterMenuMixin {

    private static final int RESULT = 1;
    private static final int INV_START = 2;
    private static final int INV_END = 38;

    @Shadow @Final private Level level;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(int containerId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    /** A panel slot: into the station the way vanilla feeds it from the inventory, then the inventory. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveFromBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = WorkstationQuickMove.fromBackpack(
            ((StonecutterMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index,
            INV_START, INV_END, s -> level.getRecipeManager().getRecipeFor(RecipeType.STONECUTTING, new SingleRecipeInput(s), level).isPresent()
                && accessor.ediblebackpacks$invokeMoveItemStackTo(s, 0, 1, false), true);
        if (result != null) cir.setReturnValue(result);
    }

    /** Only once vanilla gave up on a station or inventory slot does the backpack take it. */
    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void ediblebackpacks$overflowIntoBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack moved = WorkstationQuickMove.overflowIntoBackpack(
            ((StonecutterMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index, RESULT, INV_END);
        if (moved != null) cir.setReturnValue(moved);
    }

    /** Vanilla's output move (ordinal 0, the only reverse = true move out of the output slot). */
    @WrapOperation(method = "quickMoveStack",
            at = @At(value = "INVOKE", ordinal = 0,
                     target = "Lnet/minecraft/world/inventory/StonecutterMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"))
    private boolean ediblebackpacks$resultIntoBackpack(StonecutterMenu self, ItemStack stack, int from, int to,
                                                       boolean reverse, Operation<Boolean> original) {
        if (from != INV_START || to != INV_END || !reverse) return original.call(self, stack, from, to, reverse);
        return WorkstationQuickMove.moveResult(self.slots, stack, INV_START, INV_END,
            s -> original.call(self, s, from, to, reverse));
    }
}
