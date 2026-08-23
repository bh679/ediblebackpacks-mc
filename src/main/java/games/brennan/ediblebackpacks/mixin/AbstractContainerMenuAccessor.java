package games.brennan.ediblebackpacks.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the protected {@code addSlot} so the InventoryMenu mixin can append backpack slots,
 * and {@code moveItemStackTo} so it can implement shift-click transfers.
 */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {

    @Invoker("addSlot")
    Slot ediblebackpacks$invokeAddSlot(Slot slot);

    @Invoker("moveItemStackTo")
    boolean ediblebackpacks$invokeMoveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse);
}
