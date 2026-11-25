package games.brennan.ediblebackpacks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.WorkstationQuickMove;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack panels on the furnace, blast furnace and smoker (0 input, 1 fuel, 2 output,
 * 3..38 inventory + hotbar). The short constructor delegates to this full one, so one TAIL
 * fires once per menu on both sides. Vanilla's quick-move keeps running; see
 * {@link WorkstationQuickMove}.
 */
@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuMixin {

    private static final int INPUT = 0;
    private static final int FUEL = 1;
    private static final int RESULT = 2;
    private static final int INV_START = 3;
    private static final int INV_END = 39;

    @Shadow protected abstract boolean canSmelt(ItemStack stack);

    @Shadow protected abstract boolean isFuel(ItemStack stack);

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/inventory/RecipeBookType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
            at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(MenuType<?> menuType, RecipeType<?> recipeType,
                                                  RecipeBookType recipeBookType, int containerId,
                                                  Inventory playerInventory, Container container,
                                                  ContainerData data, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    /** A panel slot: smeltables to the input, fuel to the fuel slot (vanilla's tests), then the inventory. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveFromBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = WorkstationQuickMove.fromBackpack(
            ((AbstractFurnaceMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index,
            INV_START, INV_END, s -> {
                if (canSmelt(s)) return accessor.ediblebackpacks$invokeMoveItemStackTo(s, INPUT, INPUT + 1, false);
                if (isFuel(s)) return accessor.ediblebackpacks$invokeMoveItemStackTo(s, FUEL, FUEL + 1, false);
                return false;
            });
        if (result != null) cir.setReturnValue(result);
    }

    /** Only once vanilla gave up on an input, fuel or inventory slot does the backpack take it. */
    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void ediblebackpacks$overflowIntoBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack moved = WorkstationQuickMove.overflowIntoBackpack(
            ((AbstractFurnaceMenu) (Object) this).slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index, RESULT, INV_END);
        if (moved != null) cir.setReturnValue(moved);
    }

    /** Vanilla's output move (ordinal 0, the only reverse = true one). */
    @WrapOperation(method = "quickMoveStack",
            at = @At(value = "INVOKE", ordinal = 0,
                     target = "Lnet/minecraft/world/inventory/AbstractFurnaceMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"))
    private boolean ediblebackpacks$resultIntoBackpack(AbstractFurnaceMenu self, ItemStack stack, int from, int to,
                                                       boolean reverse, Operation<Boolean> original) {
        if (from != INV_START || to != INV_END || !reverse) return original.call(self, stack, from, to, reverse);
        return WorkstationQuickMove.moveResult(self.slots, stack, INV_START, INV_END,
            s -> original.call(self, s, from, to, reverse));
    }
}
