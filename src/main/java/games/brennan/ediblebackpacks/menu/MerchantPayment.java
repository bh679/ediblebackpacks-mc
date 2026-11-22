package games.brennan.ediblebackpacks.menu;

import java.util.List;
import java.util.Optional;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Lets the backpack pay for a trade. Picking a trade in the villager's list makes vanilla fill
 * the payment slots from the player's inventory ({@code MerchantMenu#tryMoveItems}); this runs
 * after it and tops each payment slot up from the backpack, up to a full stack, the same amount
 * vanilla would have taken from the inventory. Inventory first, backpack second.
 *
 * <p>Only slots the player could take from by hand count: {@link BackpackSlot#mayPickup} is
 * false for locked slots, closed panels and a host-mod access lock, so a shut backpack is never
 * spent. Runs on both sides with the same inputs, like vanilla's own fill.</p>
 */
public final class MerchantPayment {

    private MerchantPayment() {}

    /** Tops up payment slots 0 (cost A) and 1 (cost B) of {@code payment} from the backpack. */
    public static void topUp(List<Slot> slots, Container payment, MerchantOffer offer) {
        int start = BackpackQuickMove.backpackStart(slots);
        if (start < 0 || offer == null) return;
        topUp(slots, start, payment, 0, offer.getItemCostA());
        Optional<ItemCost> costB = offer.getItemCostB();
        costB.ifPresent(cost -> topUp(slots, start, payment, 1, cost));
    }

    private static void topUp(List<Slot> slots, int start, Container payment, int paymentSlot, ItemCost cost) {
        ItemStack held = payment.getItem(paymentSlot);
        // Something that is not this trade's cost sits there (vanilla could not hand the old
        // payment back to a full inventory): leave it, as vanilla does.
        if (!held.isEmpty() && !cost.test(held)) return;
        for (int i = start; i < start + BackpackLayout.MAX_SLOTS; i++) {
            if (!(slots.get(i) instanceof BackpackSlot slot)) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !cost.test(stack) || !slot.mayPickup(slot.owner())) continue;
            if (!held.isEmpty() && !ItemStack.isSameItemSameComponents(stack, held)) continue;
            int max = stack.getMaxStackSize();
            int take = Math.min(max - held.getCount(), stack.getCount());
            if (take <= 0) break;
            ItemStack taken = slot.remove(take);
            held = taken.copyWithCount(held.getCount() + taken.getCount());
            payment.setItem(paymentSlot, held);
            if (held.getCount() >= max) break;
        }
    }

}
