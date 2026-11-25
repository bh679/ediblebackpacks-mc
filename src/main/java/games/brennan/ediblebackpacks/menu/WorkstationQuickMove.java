package games.brennan.ediblebackpacks.menu;

import java.util.List;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shift-click routing for the backpack slots appended to menus whose vanilla quick-move uses
 * FIXED slot ranges and so keeps working untouched: the villager trade menu, the crafting table,
 * the furnaces and the item combiners (anvil, smithing table). Each of those is the station's own
 * slots, then the player's 27 + 9 rows ({@code [invStart, invEnd)}), then the 108 panel slots.
 *
 * <p>Vanilla always runs; the backpack is extra space behind it:</p>
 * <ul>
 *   <li>{@link #fromBackpack}: a click ON a panel slot, which vanilla's ranges never reach. It
 *       goes where the same item would go from the inventory (the station's inputs), then to
 *       the inventory;</li>
 *   <li>{@link #overflowIntoBackpack}: what vanilla could not place anywhere goes into the
 *       backpack;</li>
 *   <li>{@link #moveResult}: replaces vanilla's one {@code moveItemStackTo} for the output slot,
 *       so a craft, smelt or trade result can join a backpack stack. Replacing the move rather
 *       than adding a second pass keeps the output's side effects (experience, the anvil's cost,
 *       crafting triggers, a trade's use) to exactly once per take.</li>
 * </ul>
 *
 * <p>Runs on both sides. Locked or closed panel slots refuse {@link BackpackSlot#mayPlace}, and
 * every placement here asks it, so nothing can leak into one.</p>
 */
public final class WorkstationQuickMove {

    private WorkstationQuickMove() {}

    /** The station's own "where would vanilla put this from the inventory" step. */
    @FunctionalInterface
    public interface InputMove {
        /** @return whether any of {@code stack} moved. */
        boolean move(ItemStack stack);
    }

    /** Calls vanilla's own {@code moveItemStackTo(stack, invStart, invEnd, true)} for the output slot. */
    @FunctionalInterface
    public interface VanillaResultMove {
        boolean move(ItemStack stack);
    }

    /**
     * @param inputs the station's inputs, or {@code null} when a panel click goes straight to
     *               the inventory (the trade menu)
     * @return the stack {@code quickMoveStack} should return for a click on a panel slot, or
     *         {@code null} when {@code index} is not a panel slot (vanilla handles it)
     */
    public static ItemStack fromBackpack(List<Slot> slots, BackpackQuickMove.StackMover mover, int index,
                                         int invStart, int invEnd, InputMove inputs) {
        int start = BackpackQuickMove.backpackStart(slots);
        if (start < invEnd || index < start || index >= start + BackpackLayout.MAX_SLOTS) return null;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();
        boolean moved = inputs != null && inputs.move(inSlot);
        if (!inSlot.isEmpty()) moved |= mover.move(inSlot, invStart, invEnd, false);
        if (!moved) return ItemStack.EMPTY;
        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return inSlot.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    /**
     * Runs after vanilla's quick-move for {@code index}, a station or inventory slot other than
     * the output: whatever is still there goes into the backpack. Call it only once vanilla has
     * given up (returned EMPTY), so vanilla's own moves, such as the inventory/hotbar shuffle,
     * always come first.
     *
     * @return a copy of what moved (so vanilla's repeat-while-same-item loop keeps going), or
     *         {@code null} when nothing did and vanilla's own return value stands.
     */
    public static ItemStack overflowIntoBackpack(List<Slot> slots, BackpackQuickMove.StackMover mover, int index,
                                                 int resultSlot, int invEnd) {
        if (index == resultSlot || index < 0 || index >= invEnd) return null;
        int start = BackpackQuickMove.backpackStart(slots);
        if (start < invEnd) return null;
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
     * Shift-clicking the output slot. Vanilla only looks at the inventory, so the output landed
     * in a free inventory slot even with a part-full stack of it in the backpack, and a full
     * inventory took nothing. Order: top up inventory stacks, top up backpack stacks, vanilla
     * (its own merge, then empty inventory slots), then empty backpack slots.
     *
     * @return whether any of {@code stack} moved, as vanilla's call would.
     */
    public static boolean moveResult(List<Slot> slots, ItemStack stack, int invStart, int invEnd,
                                     VanillaResultMove vanilla) {
        int start = BackpackQuickMove.backpackStart(slots);
        if (start < invEnd) return vanilla.move(stack);
        int end = start + BackpackLayout.MAX_SLOTS;
        int before = stack.getCount();
        mergeOnly(slots, stack, invStart, invEnd);
        mergeOnly(slots, stack, start, end);
        if (!stack.isEmpty()) vanilla.move(stack);
        if (!stack.isEmpty()) fillEmpty(slots, stack, start, end);
        return stack.getCount() != before;
    }

    /** Tops up existing stacks of the same item in {@code [from, to)}, last slot first like vanilla's reverse merge. */
    static void mergeOnly(List<Slot> slots, ItemStack stack, int from, int to) {
        for (int i = to - 1; i >= from && !stack.isEmpty(); i--) {
            Slot slot = slots.get(i);
            ItemStack there = slot.getItem();
            if (there.isEmpty() || !ItemStack.isSameItemSameComponents(stack, there) || !slot.mayPlace(stack)) continue;
            int room = slot.getMaxStackSize(stack) - there.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, stack.getCount());
            // A fresh stack through set(): a backpack slot's handler only saves on a set.
            slot.set(there.copyWithCount(there.getCount() + moved));
            stack.shrink(moved);
        }
    }

    /** Places what is left into empty slots of {@code [from, to)}. */
    static void fillEmpty(List<Slot> slots, ItemStack stack, int from, int to) {
        for (int i = from; i < to && !stack.isEmpty(); i++) {
            Slot slot = slots.get(i);
            if (slot.hasItem() || !slot.mayPlace(stack)) continue;
            slot.setByPlayer(stack.split(Math.min(stack.getCount(), slot.getMaxStackSize(stack))));
        }
    }
}
