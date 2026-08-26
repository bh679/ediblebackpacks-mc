package games.brennan.ediblebackpacks.menu;

import java.util.List;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Shift-click (quick-move) routing for the backpack slots appended to the
 * vanilla {@code InventoryMenu}.
 *
 * <p>The backpack behaves like an open container that is always on screen:</p>
 * <ul>
 *   <li>backpack → main inventory, then hotbar (vanilla's own 9..45 destination);</li>
 *   <li>main inventory / hotbar → backpack, falling back to vanilla's
 *       hotbar↔inventory shuffle when nothing fits. Armour and offhand items
 *       still auto-equip first, exactly as they do without this mod;</li>
 *   <li>crafting result / crafting grid / armour / offhand → the player
 *       inventory as usual, overflowing into the backpack when it is full.</li>
 * </ul>
 *
 * <p>Runs on both sides; locked backpack slots are empty and refuse
 * {@link BackpackSlot#mayPlace}, so a transfer can never leak into one.</p>
 */
public final class BackpackQuickMove {

    /** First main-inventory slot of {@code InventoryMenu} (0 result, 1..4 grid, 5..8 armour). */
    public static final int INV_START = 9;
    /** Offhand slot of {@code InventoryMenu}; 9..35 main, 36..44 hotbar. */
    public static final int OFFHAND = 45;
    /**
     * Exclusive end of vanilla's usual quick-move destination range: 9..44, stopping BEFORE
     * the offhand.
     *
     * <p>It must never include {@link #OFFHAND}. A quick-move <em>out of</em> the offhand
     * passes this range to {@code moveItemStackTo}, whose merge pass would then reach slot 45
     * and find the source stack itself — computing {@code count + count} on a single
     * {@code ItemStack} instance, then {@code setCount(0)} followed by {@code setCount(2n)}.
     * That doubles the stack, and the following empty-slot pass splits the doubled stack out
     * into the inventory: a straight duplication bug.</p>
     *
     * <p>Filling an <em>empty</em> offhand is unaffected — {@link #equipPreferred} still hands
     * those clicks back to vanilla, which equips them.</p>
     */
    public static final int INV_END = OFFHAND;

    private BackpackQuickMove() {}

    /** Calls back into {@code AbstractContainerMenu#moveItemStackTo}. */
    @FunctionalInterface
    public interface StackMover {
        boolean move(ItemStack stack, int startIndex, int endIndex, boolean reverse);
    }

    /**
     * @return the stack {@code quickMoveStack} should return, or {@code null}
     *         when vanilla should handle the click unchanged.
     */
    public static ItemStack quickMove(List<Slot> slots, StackMover mover, Player player, int index) {
        if (index < 0 || index >= slots.size()) return null;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return null;

        int start = backpackStart(slots);
        if (start < 0) return null;
        int end = start + BackpackLayout.MAX_SLOTS;
        // Slots appended after ours by another mod stay vanilla's business.
        if (index >= end) return null;

        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();
        boolean moved;

        if (index >= start) {
            moved = mover.move(inSlot, INV_START, INV_END, false);
        } else if (index < INV_START || index == OFFHAND) {
            moved = mover.move(inSlot, INV_START, INV_END, index == 0);
            if (!inSlot.isEmpty()) moved |= mover.move(inSlot, start, end, false);
        } else {
            if (equipPreferred(slots, player, original)) return null;
            moved = mover.move(inSlot, start, end, false);
            // Nothing fit in the backpack: let vanilla do its hotbar↔inventory move.
            if (!moved) return null;
        }

        if (!moved) return ItemStack.EMPTY;

        if (index == 0) slot.onQuickCraft(inSlot, original);
        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (inSlot.getCount() == original.getCount()) return ItemStack.EMPTY;

        slot.onTake(player, inSlot);
        if (index == 0) player.drop(inSlot, false);
        return original;
    }

    /**
     * Vanilla equips armour/offhand items before shuffling them around the
     * inventory; keep that priority ahead of the backpack.
     */
    private static boolean equipPreferred(List<Slot> slots, Player player, ItemStack stack) {
        EquipmentSlot equipment = player.getEquipmentSlotForItem(stack);
        return switch (equipment) {
            case HEAD, CHEST, LEGS, FEET -> !slots.get(8 - equipment.getIndex()).hasItem();
            case OFFHAND -> !slots.get(OFFHAND).hasItem();
            default -> false;
        };
    }

    /** Index of the first backpack slot, or -1 when the block isn't present/contiguous. */
    private static int backpackStart(List<Slot> slots) {
        int end = -1;
        for (int i = slots.size() - 1; i >= 0; i--) {
            if (slots.get(i) instanceof BackpackSlot) {
                end = i + 1;
                break;
            }
        }
        if (end < BackpackLayout.MAX_SLOTS) return -1;
        int start = end - BackpackLayout.MAX_SLOTS;
        return slots.get(start) instanceof BackpackSlot ? start : -1;
    }
}
