package games.brennan.ediblebackpacks.storage;

import java.util.function.Predicate;

import games.brennan.ediblebackpacks.config.EBConfig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Ground-pickup routing into the backpack, wrapped around vanilla's {@code Inventory.add}
 * by {@code mixin/ItemEntityMixin}:
 * <ol>
 *   <li>top up backpack stacks that already hold this item — even while the inventory has
 *       room, so a stack the player parked in the backpack keeps collecting;</li>
 *   <li>vanilla's inventory add (hotbar, then main inventory);</li>
 *   <li>whatever is still left goes into empty backpack slots.</li>
 * </ol>
 *
 * <p>Only unlocked slots take part. When it runs is the player's own
 * {@link games.brennan.ediblebackpacks.config.AutoCollectMode}: by default nothing is
 * collected while the panels are closed — closed is a lock, as {@code menu/BackpackSlot}
 * enforces for clicks — but a player who picks ALWAYS has asked for pickups into slots they
 * can't see. The server's {@link EBConfig#autoCollect()} turns it all off.</p>
 *
 * <p>Every step shrinks the passed stack in place, the same contract {@code Inventory.add}
 * follows, so vanilla's {@code playerTouch} reads the picked-up count off the item entity's
 * own stack afterwards (take animation, stats, discard).</p>
 */
public final class BackpackPickup {

    private BackpackPickup() {}

    /**
     * @param vanillaAdd the wrapped {@code Inventory.add} call
     * @return whether any of {@code stack} was taken — {@code Inventory.add}'s own meaning
     */
    public static boolean collect(BackpackData data, ItemStack stack, Predicate<ItemStack> vanillaAdd) {
        if (!accepts(data)) return vanillaAdd.test(stack);

        int before = stack.getCount();
        topUp(data.items(), data.unlocked(), stack);
        if (!stack.isEmpty()) vanillaAdd.test(stack);
        if (!stack.isEmpty()) fillEmpty(data.items(), data.unlocked(), stack);
        return stack.getCount() < before;
    }

    private static boolean accepts(BackpackData data) {
        return EBConfig.autoCollect() && data.autoCollect().collects(data.panelsOpen()) && data.unlocked() > 0;
    }

    /** Merge {@code stack} into the first {@code unlocked} slots that already hold the same item. */
    public static void topUp(IItemHandler items, int unlocked, ItemStack stack) {
        insert(items, unlocked, stack, true);
    }

    /** Put {@code stack} into the first empty slots among the first {@code unlocked}. */
    public static void fillEmpty(IItemHandler items, int unlocked, ItemStack stack) {
        insert(items, unlocked, stack, false);
    }

    private static void insert(IItemHandler items, int unlocked, ItemStack stack, boolean matching) {
        int limit = Math.min(unlocked, items.getSlots());
        for (int i = 0; i < limit && !stack.isEmpty(); i++) {
            ItemStack existing = items.getStackInSlot(i);
            boolean candidate = matching
                ? !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)
                : existing.isEmpty();
            if (!candidate) continue;
            ItemStack remainder = items.insertItem(i, stack.copy(), false);
            stack.setCount(remainder.getCount());
        }
    }
}
