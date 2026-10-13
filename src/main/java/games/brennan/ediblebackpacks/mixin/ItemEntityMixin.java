package games.brennan.ediblebackpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.storage.BackpackPickup;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets the backpack collect items off the ground — see {@link BackpackPickup} for the order.
 *
 * <p>Wraps only the {@code Inventory.add} call inside {@code playerTouch}, so vanilla keeps
 * everything around it: the pickup delay and owner checks, NeoForge's pickup events, the take
 * animation, stats, and discarding the item entity once its stack is empty. Pickup events
 * alone can't do this — {@code Inventory.add} returns false on a full inventory, and vanilla
 * then skips the rest of the pickup entirely.</p>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @WrapOperation(method = "playerTouch",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean ediblebackpacks$collectIntoBackpack(Inventory inventory, ItemStack stack, Operation<Boolean> original) {
        return BackpackPickup.collect(
            inventory.player.getData(ModAttachments.BACKPACK), stack, s -> original.call(inventory, s));
    }
}
