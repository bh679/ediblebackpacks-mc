package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.menu.BackpackQuickMove;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slot-range invariants for {@link BackpackQuickMove}.
 *
 * <p>{@code quickMove} itself can't be exercised here — it takes {@code Slot}/{@code Player},
 * and this suite is pure-logic (no Minecraft on the test classpath). What these tests pin is the
 * arithmetic that decides where a quick-move is allowed to land, which is where the duplication
 * bug lived.</p>
 */
class BackpackQuickMoveTest {

    @Test
    void destinationRangeNeverContainsTheOffhand() {
        // A quick-move OUT of the offhand hands [INV_START, INV_END) to moveItemStackTo. If that
        // range covered slot 45, the merge pass would reach the source stack itself and run
        // `count + count` on one ItemStack instance — setCount(0) then setCount(2n) — doubling
        // it, after which the empty-slot pass splits the doubled stack out into the inventory.
        // Vanilla's own offhand branch uses 9..45 for exactly this reason.
        assertTrue(BackpackQuickMove.OFFHAND >= BackpackQuickMove.INV_END,
            "destination range [" + BackpackQuickMove.INV_START + ", " + BackpackQuickMove.INV_END
                + ") must not contain the offhand slot " + BackpackQuickMove.OFFHAND
                + " — quick-moving out of the offhand would duplicate the stack");
    }

    @Test
    void rangeIsNonEmptyAndOrdered() {
        assertTrue(BackpackQuickMove.INV_START < BackpackQuickMove.INV_END,
            "destination range must be non-empty");
    }

    @Test
    void vanillaInventoryMenuSlotLayout() {
        // The routing assumes vanilla's InventoryMenu indices: 0 result, 1..4 grid, 5..8 armour,
        // 9..35 main, 36..44 hotbar, 45 offhand. A layout change upstream invalidates the branches.
        assertEquals(9, BackpackQuickMove.INV_START);
        assertEquals(45, BackpackQuickMove.OFFHAND);
    }
}
