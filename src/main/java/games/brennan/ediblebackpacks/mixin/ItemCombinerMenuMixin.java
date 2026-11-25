package games.brennan.ediblebackpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.WorkstationQuickMove;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack panels on the anvil and smithing table: their inputs, then the result, then the
 * player's 36 slots. One constructor builds every slot, so its TAIL is where the panels go,
 * after vanilla's last slot and before any subclass work. Vanilla's quick-move keeps running;
 * see {@link WorkstationQuickMove}.
 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    /** The player's inventory + hotbar, right after the result slot. */
    private static final int PLAYER_SLOTS = 36;

    @Shadow public abstract int getResultSlot();

    @Shadow protected abstract boolean canMoveIntoInputSlots(ItemStack stack);

    @Shadow public abstract int getSlotToQuickMoveTo(ItemStack stack);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(@Nullable MenuType<?> type, int containerId,
                                                  Inventory playerInventory, ContainerLevelAccess access,
                                                  CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    private int ediblebackpacks$invStart() {
        return getResultSlot() + 1;
    }

    /** A panel slot: into the input vanilla would pick, then the inventory. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveFromBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        int invStart = ediblebackpacks$invStart();
        ItemStack result = WorkstationQuickMove.fromBackpack(
            ((ItemCombinerMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index,
            invStart, invStart + PLAYER_SLOTS,
            s -> canMoveIntoInputSlots(s)
                && accessor.ediblebackpacks$invokeMoveItemStackTo(s, getSlotToQuickMoveTo(s), getResultSlot(), false));
        if (result != null) cir.setReturnValue(result);
    }

    /** Only once vanilla gave up on an input or inventory slot does the backpack take it. */
    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void ediblebackpacks$overflowIntoBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack moved = WorkstationQuickMove.overflowIntoBackpack(
            ((ItemCombinerMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index,
            getResultSlot(), ediblebackpacks$invStart() + PLAYER_SLOTS);
        if (moved != null) cir.setReturnValue(moved);
    }

    /** Vanilla's result move (ordinal 0, the only reverse = true one). */
    @WrapOperation(method = "quickMoveStack",
            at = @At(value = "INVOKE", ordinal = 0,
                     target = "Lnet/minecraft/world/inventory/ItemCombinerMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"))
    private boolean ediblebackpacks$resultIntoBackpack(ItemCombinerMenu self, ItemStack stack, int from, int to,
                                                       boolean reverse, Operation<Boolean> original) {
        int invStart = ediblebackpacks$invStart();
        if (from != invStart || to != invStart + PLAYER_SLOTS || !reverse) return original.call(self, stack, from, to, reverse);
        return WorkstationQuickMove.moveResult(self.slots, stack, from, to,
            s -> original.call(self, s, from, to, reverse));
    }
}
