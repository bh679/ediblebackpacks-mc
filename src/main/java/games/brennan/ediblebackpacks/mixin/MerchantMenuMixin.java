package games.brennan.ediblebackpacks.mixin;

import games.brennan.ediblebackpacks.menu.BackpackSlotAppender;
import games.brennan.ediblebackpacks.menu.MerchantPayment;
import games.brennan.ediblebackpacks.menu.MerchantQuickMove;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the backpack panels on the villager and wandering-trader screen, and lets the backpack
 * pay for a trade.
 *
 * <p>The two-argument constructor (the client's) delegates to this three-argument one, so one
 * TAIL fires once per menu on both sides. Unlike the chest mixins, vanilla's quick-move is NOT
 * replaced: see {@link MerchantQuickMove} for why it runs first and the backpack takes what it
 * leaves.</p>
 */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {

    @Shadow @Final private MerchantContainer tradeContainer;

    @Shadow public abstract MerchantOffers getOffers();

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V",
            at = @At("TAIL"))
    private void ediblebackpacks$addBackpackSlots(int containerId, Inventory playerInventory,
                                                  Merchant trader, CallbackInfo ci) {
        BackpackSlotAppender.append((AbstractContainerMenuAccessor) this, playerInventory.player);
    }

    /** A click on a panel slot, which vanilla's ranges (0..38) never reach. */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void ediblebackpacks$quickMoveFromBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        MerchantMenu self = (MerchantMenu) (Object) this;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack result = MerchantQuickMove.fromBackpack(
            self.slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index);
        if (result != null) cir.setReturnValue(result);
    }

    /**
     * Whatever vanilla left in a payment or inventory slot goes into the backpack. Not reached
     * when an earlier HEAD inject (Dungeon Train's extra-cost grid) already handled the click.
     */
    @Inject(method = "quickMoveStack", at = @At("RETURN"), cancellable = true)
    private void ediblebackpacks$overflowIntoBackpack(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        MerchantMenu self = (MerchantMenu) (Object) this;
        AbstractContainerMenuAccessor accessor = (AbstractContainerMenuAccessor) this;
        ItemStack moved = MerchantQuickMove.overflowIntoBackpack(
            self.slots, accessor::ediblebackpacks$invokeMoveItemStackTo, index);
        if (moved != null && cir.getReturnValue().isEmpty()) cir.setReturnValue(moved);
    }

    /** Picking a trade: vanilla fills the payment from the inventory, the backpack tops it up. */
    @Inject(method = "tryMoveItems", at = @At("RETURN"))
    private void ediblebackpacks$payFromBackpack(int selected, CallbackInfo ci) {
        MerchantOffers offers = getOffers();
        if (selected < 0 || selected >= offers.size()) return;
        MerchantPayment.topUp(((MerchantMenu) (Object) this).slots, tradeContainer, offers.get(selected));
    }
}
