package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.ContainerQuickMove;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The shulker box twin of {@link ChestMenuMixin} — same shape, fixed 27-slot container, and the
 * two-argument constructor delegates here so one TAIL still covers both.
 *
 * <p>Nothing extra is needed to keep a shulker box out of a shulker box: the container's own
 * {@code ShulkerBoxSlot} refuses it in {@code mayPlace}, and {@code moveItemStackTo} asks.</p>
 */
@Mixin(ShulkerBoxMenu.class)
public abstract class ShulkerBoxMenuMixin {

    /** A shulker box is always 27 slots ({@code ShulkerBoxMenu} checks this itself). */
    private static final int CONTAINER_SIZE = 27;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V",
            at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(int containerId, Inventory playerInventory,
                                                  Container container, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ShulkerBoxMenu self = (ShulkerBoxMenu) (Object) this;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = ContainerQuickMove.quickMove(
            self.slots, accessor::ediblebackpacks$invokeMoveItemStackTo, player, index, CONTAINER_SIZE);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
