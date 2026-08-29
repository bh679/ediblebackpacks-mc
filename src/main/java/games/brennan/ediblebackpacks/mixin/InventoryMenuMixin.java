package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackQuickMove;
import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends the 108 backpack slots to the survival inventory menu on BOTH
 * sides (identical order ⇒ vanilla container slot sync carries contents for
 * free) — see {@link games.brennan.ediblebackpacks.menu.BackpackSlotAppender}. The chest-shaped menus get the
 * same treatment in {@link ChestMenuMixin} and {@link ShulkerBoxMenuMixin}.
 *
 * <p>Also routes shift-clicks through {@link BackpackQuickMove} so the panels
 * behave like an always-open container.</p>
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V",
            at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, owner);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = BackpackQuickMove.quickMove(
            self.slots, accessor::ediblebackpacks$invokeMoveItemStackTo, player, index);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
