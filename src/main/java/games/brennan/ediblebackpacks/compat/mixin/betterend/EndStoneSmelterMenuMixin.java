package games.brennan.ediblebackpacks.compat.mixin.betterend;

import games.brennan.ediblebackpacks.menu.BackpackQuickMove;
import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.WorkstationQuickMove;
import games.brennan.ediblebackpacks.mixin.AbstractContainerMenuAccessor;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Backpack panels on BetterEnd's End Stone Smelter (0, 1 alloy ingredients, 2 fuel, 3 result,
 * 4..39 inventory + hotbar). Its quick-move uses fixed ranges, so this is the vanilla-furnace
 * pattern ({@link WorkstationQuickMove}), applied only when BetterEnd is installed
 * ({@code CompatMixinPlugin}) and written without any BetterEnd type on the compile path.
 *
 * <p>Without BetterEnd's class there is no typed receiver for a {@code @WrapOperation}, so the
 * result click is handled here in full, copying the smelter's own result branch (BetterEnd
 * 21.0.35: {@code moveItemStackTo(stack, 4, 40, true)}, {@code onQuickCraft}, set/changed,
 * {@code onTake}). Re-check it on a BetterEnd bump.</p>
 */
@Pseudo
@Mixin(targets = "org.betterx.betterend.client.gui.EndStoneSmelterMenu")
public abstract class EndStoneSmelterMenuMixin {

    private static final int FUEL = 2;
    private static final int RESULT = 3;
    private static final int INV_START = 4;
    private static final int INV_END = 40;

    @Shadow protected abstract boolean isSmeltable(ItemStack stack);

    @Shadow public abstract boolean isFuel(ItemStack stack);

    private List<Slot> ediblebackpacks$slots() {
        return ((AbstractContainerMenu) (Object) this).slots;
    }

    /** The two-argument (client) constructor delegates here, so one TAIL covers both sides. */
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
            at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(int containerId, Inventory playerInventory,
                                                  Container container, ContainerData data, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        List<Slot> slots = ediblebackpacks$slots();
        if (index == RESULT) {
            ItemStack result = ediblebackpacks$takeResult(player, slots, accessor);
            if (result != null) cir.setReturnValue(result);
            return;
        }
        // A panel slot: alloy ingredients to the inputs, fuel to the fuel slot (the smelter's
        // own tests), then the inventory.
        ItemStack result = WorkstationQuickMove.fromBackpack(
            slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index, INV_START, INV_END, s -> {
                if (isSmeltable(s)) return accessor.ediblebackpacks$invokeMoveItemStackTo(s, 0, FUEL, false);
                if (isFuel(s)) return accessor.ediblebackpacks$invokeMoveItemStackTo(s, FUEL, RESULT, false);
                return false;
            });
        if (result != null) cir.setReturnValue(result);
    }

    /**
     * The smelter's result branch with {@link WorkstationQuickMove#moveResult} as its move, so
     * the output can join a backpack stack and the take still fires once.
     *
     * @return the stack to return, or {@code null} to let the smelter handle the click
     */
    private static ItemStack ediblebackpacks$takeResult(Player player, List<Slot> slots,
                                                        AbstractContainerMenuAccessor accessor) {
        if (BackpackQuickMove.backpackStart(slots) < INV_END) return null;
        Slot slot = slots.get(RESULT);
        if (slot == null || !slot.hasItem()) return null;
        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();
        if (!WorkstationQuickMove.moveResult(slots, inSlot, INV_START, INV_END,
                s -> accessor.ediblebackpacks$invokeMoveItemStackTo(s, INV_START, INV_END, true))) {
            return ItemStack.EMPTY;
        }
        slot.onQuickCraft(inSlot, original);
        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (inSlot.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, inSlot);
        return original;
    }

    /** Only once the smelter gave up on an input, fuel or inventory slot does the backpack take it. */
    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void ediblebackpacks$overflowIntoBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack moved = WorkstationQuickMove.overflowIntoBackpack(
            ediblebackpacks$slots(), accessor::ediblebackpacks$invokeMoveItemStackTo, index, RESULT, INV_END);
        if (moved != null) cir.setReturnValue(moved);
    }
}
