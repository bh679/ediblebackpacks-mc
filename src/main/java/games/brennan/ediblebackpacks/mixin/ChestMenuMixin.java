package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.ContainerQuickMove;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the backpack panels inside every chest-like GUI: chests, trapped chests, ender chests,
 * barrels — anything backed by a {@code GENERIC_9x1..9x6} menu. A backpack the player has to
 * close the chest to reach is not much of a backpack.
 *
 * <p>Injected on the one public constructor because every other constructor and factory on
 * {@link ChestMenu} delegates to it through {@code this(...)}, so a single TAIL fires exactly
 * once per menu, on both logical sides, for every row count.</p>
 */
@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V",
            at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(MenuType<?> type, int containerId, Inventory playerInventory,
                                                  Container container, int rows, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    /**
     * Vanilla's own quick-move hands {@code slots.size()} to {@code moveItemStackTo}, which now
     * runs past the player's rows into the panels — so this has to be replaced outright rather
     * than merely extended. See {@link ContainerQuickMove}.
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        ChestMenu self = (ChestMenu) (Object) this;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = ContainerQuickMove.quickMove(
            self.slots, accessor::ediblebackpacks$invokeMoveItemStackTo, player, index,
            self.getRowCount() * 9);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
