package games.brennan.ediblebackpacks.menu;

import java.util.List;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shift-click routing for the backpack slots appended to the villager trade menu
 * ({@code MerchantMenu}: 0..1 payment, 2 result, 3..29 main inventory, 30..38 hotbar).
 *
 * <p>Two halves, because vanilla's own trade quick-move has to keep running for everything that
 * is not a panel slot: the result slot's take is what completes a trade, and Dungeon Train hooks
 * the same method for its extra-cost grid. So this never replaces vanilla's click, it only:</p>
 * <ul>
 *   <li>{@link #fromBackpack}: handles a click ON a panel slot, which vanilla would ignore
 *       (its ranges stop at 39), sending it to the player's inventory;</li>
 *   <li>{@link #overflowIntoBackpack}: after vanilla has run for a payment or inventory slot,
 *       moves whatever it could not place into the backpack, the same "extra space behind
 *       vanilla's destination" rule the survival inventory follows.</li>
 * </ul>
 * <p>The result slot is left to vanilla alone: a partial take there fires the trade's side
 * effects, and splitting one click across two passes would fire them twice.</p>
 */
public final class MerchantQuickMove {

    /** First player-inventory slot of {@code MerchantMenu}. */
    public static final int INV_START = 3;
    /** Exclusive end of the player's inventory + hotbar in {@code MerchantMenu}. */
    public static final int INV_END = 39;
    /** The trade result slot. */
    public static final int RESULT = 2;

    private MerchantQuickMove() {}

    /**
     * @return the stack {@code quickMoveStack} should return for a click on a panel slot, or
     *         {@code null} when {@code index} is not a panel slot (vanilla handles it).
     */
    public static ItemStack fromBackpack(List<Slot> slots, BackpackQuickMove.StackMover mover, int index) {
        int start = BackpackQuickMove.backpackStart(slots);
        if (start < INV_END || index < start || index >= start + BackpackLayout.MAX_SLOTS) return null;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();
        if (!mover.move(inSlot, INV_START, INV_END, false)) return ItemStack.EMPTY;
        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return inSlot.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    /**
     * Runs after vanilla's quick-move for {@code index}: whatever is still in a payment or
     * inventory slot goes into the backpack.
     *
     * @return a copy of what moved (so vanilla's repeat-while-same-item loop keeps going), or
     *         {@code null} when nothing did and vanilla's own return value stands.
     */
    public static ItemStack overflowIntoBackpack(List<Slot> slots, BackpackQuickMove.StackMover mover, int index) {
        if (index == RESULT || index < 0 || index >= INV_END) return null;
        int start = BackpackQuickMove.backpackStart(slots);
        if (start < INV_END) return null;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return null;

        ItemStack inSlot = slot.getItem();
        int before = inSlot.getCount();
        ItemStack moved = inSlot.copy();
        if (!mover.move(inSlot, start, start + BackpackLayout.MAX_SLOTS, false)) return null;
        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        moved.setCount(before - inSlot.getCount());
        return moved.isEmpty() ? null : moved;
    }

    /**
     * Closing the trade screen: vanilla hands the payment back with
     * {@code placeItemBackInInventory}, which drops whatever does not fit. Payment can now come
     * out of the backpack, so a player with a full inventory could close the screen and watch
     * their emeralds hit the floor. Send it to the inventory first, then the backpack; anything
     * left is vanilla's to place or drop as before. Server side, living connected players only,
     * matching vanilla's own branch.
     */
    public static void returnPayment(List<Slot> slots, BackpackQuickMove.StackMover mover) {
        int start = BackpackQuickMove.backpackStart(slots);
        if (start < INV_END) return;
        for (int i = 0; i < RESULT; i++) {
            Slot slot = slots.get(i);
            if (!slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            mover.move(stack, INV_START, INV_END, false);
            if (!stack.isEmpty()) mover.move(stack, start, start + BackpackLayout.MAX_SLOTS, false);
            slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }
}
