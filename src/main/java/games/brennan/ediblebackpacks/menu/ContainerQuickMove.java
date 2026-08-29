package games.brennan.ediblebackpacks.menu;

import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shift-click routing for the chest-shaped menus the panels are appended to — {@code ChestMenu}
 * (chests, trapped chests, ender chests, barrels, every {@code GENERIC_9xN} size) and
 * {@code ShulkerBoxMenu}. Their slot lists have the same shape once the container's size is
 * known: container, then the player's 27 + 9 rows, then this mod's 108 panel slots.
 *
 * <p>The routing keeps vanilla's meaning of a shift-click and treats the backpack as extra
 * space behind it:</p>
 * <ul>
 *   <li>container → player inventory, overflowing into the backpack;</li>
 *   <li>player inventory / hotbar → the container, overflowing into the backpack;</li>
 *   <li>backpack → the container, overflowing into the player inventory. Inside a chest a
 *       shift-click has always meant "deposit", so the backpack unloads the same way the
 *       inventory does.</li>
 * </ul>
 *
 * <p>Intercepting is not optional here. Vanilla's own {@code quickMoveStack} passes
 * {@code slots.size()} as the end of the player-side range, which now runs past the player's
 * rows and into the panels — so left alone it would push chest contents straight into the
 * backpack, filling it from the far end first and ignoring the toggle entirely.</p>
 *
 * <p>Unlike {@link BackpackQuickMove} there is no armour, offhand or crafting result to route
 * around, so none of the ordering hazards documented there apply. Runs on both sides; locked
 * or closed panel slots refuse {@link BackpackSlot#mayPlace}, so nothing can leak into one.</p>
 */
public final class ContainerQuickMove {

    private ContainerQuickMove() {}

    /**
     * @param containerEnd exclusive end of the block's own slots — {@code rows * 9} for a chest,
     *                     27 for a shulker box
     * @return the stack {@code quickMoveStack} should return, or {@code null} when vanilla
     *         should handle the click unchanged
     */
    public static ItemStack quickMove(List<Slot> slots, BackpackQuickMove.StackMover mover,
                                      Player player, int index, int containerEnd) {
        if (index < 0 || index >= slots.size()) return null;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return null;

        int start = BackpackQuickMove.backpackStart(slots);
        if (start < 0) return null;
        int end = start + BackpackLayout.MAX_SLOTS;
        // Slots appended after ours by another mod stay vanilla's business.
        if (index >= end) return null;
        // The player's rows have to sit between the container and the panels for the ranges
        // below to mean anything; if they don't, this isn't a menu we understand.
        if (containerEnd <= 0 || containerEnd >= start) return null;

        // Everything from the container's end up to the first panel slot is the player's own
        // inventory and hotbar — read from the slot list rather than assumed to be 36, so a
        // mod that inserted slots there doesn't silently shift the range.
        int playerEnd = start;

        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();
        boolean moved;

        if (index < containerEnd) {
            // Reverse, like vanilla: out of a chest, the hotbar fills before the main rows.
            moved = mover.move(inSlot, containerEnd, playerEnd, true);
            if (!inSlot.isEmpty()) moved |= mover.move(inSlot, start, end, false);
        } else if (index < playerEnd) {
            moved = mover.move(inSlot, 0, containerEnd, false);
            if (!inSlot.isEmpty()) moved |= mover.move(inSlot, start, end, false);
        } else {
            moved = mover.move(inSlot, 0, containerEnd, false);
            if (!inSlot.isEmpty()) moved |= mover.move(inSlot, containerEnd, playerEnd, false);
        }

        if (!moved) return ItemStack.EMPTY;

        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        // Vanilla's chest and shulker branches deliberately skip onTake — none of these slots
        // have a take side effect, and calling it would fire hooks a vanilla chest never does.
        return inSlot.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }
}
