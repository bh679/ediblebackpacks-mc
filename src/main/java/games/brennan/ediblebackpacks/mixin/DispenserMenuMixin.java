package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.ContainerQuickMove;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack panels on the dispenser and dropper (both {@link DispenserMenu}): 9 container slots,
 * then the player's 36. Handled like the chests via {@link ContainerQuickMove}; the
 * two-argument constructor delegates here.
 */
@Mixin(DispenserMenu.class)
public abstract class DispenserMenuMixin {

    private static final int CONTAINER_SIZE = 9;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V",
            at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(int containerId, Inventory playerInventory,
                                                  Container container, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        DispenserMenu self = (DispenserMenu) (Object) this;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = ContainerQuickMove.quickMove(
            self.slots, accessor::ediblebackpacks$invokeMoveItemStackTo, player, index, CONTAINER_SIZE);
        if (result != null) cir.setReturnValue(result);
    }
}
