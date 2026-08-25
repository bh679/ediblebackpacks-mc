package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.menu.BackpackQuickMove;
import games.brennan.ediblebackpacks.menu.BackpackSlot;
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
 * free). Locked slots are inert — see {@link BackpackSlot}.
 *
 * <p>Also routes shift-clicks through {@link BackpackQuickMove} so the panels
 * behave like an always-open container.</p>
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V",
            at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        AbstractContainerMenuAccessor self = (AbstractContainerMenuAccessor) this;
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            // The slot resolves the player's attachment on every access — see
            // BackpackSlot: NeoForge swaps the attachment object out on login
            // and respawn, both of which happen after this constructor runs.
            self.ediblebackpacks$invokeAddSlot(new BackpackSlot(owner, i));
        }
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
