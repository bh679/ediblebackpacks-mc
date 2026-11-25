package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.WorkstationQuickMove;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack panels on the enchanting table (0 item, 1 lapis, 2..37 inventory + hotbar). It has no output slot, so nothing is wrapped; a panel click puts ONE item in the item slot, as vanilla does from the inventory, and leaves the rest of the stack in the backpack.
 * Vanilla's quick-move uses fixed ranges and keeps running; see {@link WorkstationQuickMove}.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    private static final int RESULT = -1;
    private static final int INV_START = 2;
    private static final int INV_END = 38;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(int containerId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    /** A panel slot: into the station the way vanilla feeds it from the inventory, then the inventory. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveFromBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = WorkstationQuickMove.fromBackpack(
            ((EnchantmentMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index,
            INV_START, INV_END, s -> {
                if (s.is(Items.LAPIS_LAZULI)) return accessor.ediblebackpacks$invokeMoveItemStackTo(s, 1, 2, true);
                Slot item = ((EnchantmentMenu) (Object) this).slots.get(0);
                if (item.hasItem() || !item.mayPlace(s)) return false;
                item.setByPlayer(s.copyWithCount(1));
                s.shrink(1);
                return true;
            }, false);
        // EMPTY either way: a non-empty return makes vanilla's click loop call this again, and the
        // second call (item slot now full) would carry the rest of the stack to the inventory.
        if (result != null) cir.setReturnValue(ItemStack.EMPTY);
    }

    /** Only once vanilla gave up on a station or inventory slot does the backpack take it. */
    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void ediblebackpacks$overflowIntoBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack moved = WorkstationQuickMove.overflowIntoBackpack(
            ((EnchantmentMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index, RESULT, INV_END);
        if (moved != null) cir.setReturnValue(moved);
    }
}
