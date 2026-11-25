package games.brennan.ediblebackpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import games.brennan.ediblebackpacks.storage.BackpackPickup;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Taking a book off a chiseled bookshelf fills the backpack like a ground pickup — see
 * {@link BackpackPickup} for the order and the open/locked rules.
 *
 * <p>Vanilla's {@code removeBook} gives the book with {@code Inventory.add} and drops it at the
 * player's feet when that fails, so with a full inventory the book hit the floor first and only
 * reached the backpack when picked back up. Wrapping just that call keeps vanilla's drop as the
 * fallback for whatever the backpack can't hold.</p>
 */
@Mixin(ChiseledBookShelfBlock.class)
public abstract class ChiseledBookShelfBlockMixin {

    @WrapOperation(method = "removeBook",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean ediblebackpacks$bookIntoBackpack(Inventory inventory, ItemStack stack, Operation<Boolean> original) {
        return BackpackPickup.collect(inventory.player, stack, s -> original.call(inventory, s));
    }
}
