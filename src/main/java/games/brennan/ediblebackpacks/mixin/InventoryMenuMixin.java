package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.menu.BackpackSlot;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Appends the 144 backpack slots to the survival inventory menu on BOTH
 * sides (identical order ⇒ vanilla container slot sync carries contents for
 * free). Locked slots are inert — see {@link BackpackSlot}.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Inventory;ZLnet/minecraft/world/entity/player/Player;)V",
            at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        BackpackData data = owner.getData(ModAttachments.BACKPACK);
        AbstractContainerMenuAccessor self = (AbstractContainerMenuAccessor) this;
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            self.ediblebackpacks$invokeAddSlot(new BackpackSlot(data.items(), owner, i));
        }
    }
}
