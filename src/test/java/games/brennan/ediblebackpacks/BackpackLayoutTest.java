package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackLayoutTest {

    @Test
    void positionsAreUniqueAndOnTheCorrectSide() {
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            int x = BackpackLayout.slotX(i);
            int y = BackpackLayout.slotY(i);
            assertTrue(seen.add(((long) x << 32) | (y & 0xffffffffL)), "duplicate pos at " + i);
            if (BackpackLayout.isRightPanel(i)) {
                assertTrue(x >= BackpackLayout.RIGHT_INNER_X, "right-panel slot left of panel: " + i);
            } else {
                assertTrue(x < 0, "left-panel slot not at negative x: " + i);
            }
        }
    }

    @Test
    void bookModePutsEveryPanelSlotRightOfTheGui() {
        // The recipe book covers the left panel, so book mode moves BOTH panels right of the
        // GUI rather than hiding them — a hidden slot still accepts quick-moved items.
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            int x = BackpackLayout.slotX(i, true);
            int y = BackpackLayout.slotY(i);
            assertTrue(seen.add(((long) x << 32) | (y & 0xffffffffL)), "duplicate pos at " + i);
            assertTrue(x >= BackpackLayout.RIGHT_INNER_X, "slot " + i + " still left of the GUI");
        }
    }

    @Test
    void bookModeKeepsTheLeftPanelClosestAndFillOrderIntact() {
        // Slot 0 stays the column nearest the GUI, and the right panel starts beyond the
        // left panel's six columns — so items keep landing closest-first, outward.
        assertEquals(BackpackLayout.RIGHT_INNER_X, BackpackLayout.slotX(0, true));
        assertEquals(BackpackLayout.RIGHT_INNER_X + BackpackLayout.COLS * BackpackLayout.SLOT,
            BackpackLayout.slotX(BackpackLayout.PANEL_SLOTS, true));
        for (int i = 0; i < 9; i++) {
            assertEquals(BackpackLayout.slotX(0, true), BackpackLayout.slotX(i, true), "column of slot " + i);
        }
        assertTrue(BackpackLayout.slotX(9, true) > BackpackLayout.slotX(0, true), "second column grows outward");
    }

    @Test
    void normalModeIsUnchanged() {
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            assertEquals(BackpackLayout.slotX(i), BackpackLayout.slotX(i, false));
        }
    }

    @Test
    void rightExtentCoversTheOutermostColumn() {
        assertEquals(BackpackLayout.GUI_WIDTH, BackpackLayout.rightExtent(0, true), "no slots, no panel");
        assertEquals(BackpackLayout.RIGHT_INNER_X + BackpackLayout.SLOT + BackpackLayout.BORDER,
            BackpackLayout.rightExtent(1, true));
        assertEquals(BackpackLayout.RIGHT_INNER_X + 2 * BackpackLayout.COLS * BackpackLayout.SLOT
                + BackpackLayout.BORDER,
            BackpackLayout.rightExtent(BackpackLayout.MAX_SLOTS, true));
        // Normal mode only ever has the right panel on that side.
        assertEquals(BackpackLayout.GUI_WIDTH, BackpackLayout.rightExtent(BackpackLayout.PANEL_SLOTS, false));

        for (int unlocked = 1; unlocked <= BackpackLayout.MAX_SLOTS; unlocked++) {
            int extent = BackpackLayout.rightExtent(unlocked, true);
            for (int i = 0; i < unlocked; i++) {
                assertTrue(BackpackLayout.slotX(i, true) + BackpackLayout.SLOT <= extent,
                    "slot " + i + " sticks out past the extent for " + unlocked + " unlocked");
            }
        }
    }

    @Test
    void panelSplitIs54_54() {
        assertFalse(BackpackLayout.isRightPanel(53));
        assertTrue(BackpackLayout.isRightPanel(54));
        assertEquals(108, BackpackLayout.MAX_SLOTS);
    }

    @Test
    void fillGoesDownTheClosestColumnFirst() {
        // First 9 slots: same x (the column nearest the GUI), y walking down.
        for (int i = 0; i < 9; i++) {
            assertEquals(BackpackLayout.LEFT_INNER_X, BackpackLayout.slotX(i), "slot " + i);
            assertEquals(BackpackLayout.Y0 + i * BackpackLayout.SLOT, BackpackLayout.slotY(i));
        }
        // Slot 9 starts the next column, one step OUTWARD (further left), back at the top.
        assertEquals(BackpackLayout.LEFT_INNER_X - BackpackLayout.SLOT, BackpackLayout.slotX(9));
        assertEquals(BackpackLayout.Y0, BackpackLayout.slotY(9));
        // Right panel: first column hugs the GUI's right edge and grows rightward.
        assertEquals(BackpackLayout.RIGHT_INNER_X, BackpackLayout.slotX(54));
        assertEquals(BackpackLayout.Y0, BackpackLayout.slotY(54));
        assertEquals(BackpackLayout.RIGHT_INNER_X + BackpackLayout.SLOT, BackpackLayout.slotX(63));
    }

    @Test
    void unlockedDistributionFillsLeftThenRight() {
        assertEquals(0, BackpackLayout.unlockedOnPanel(0, false));
        assertEquals(5, BackpackLayout.unlockedOnPanel(5, false));
        assertEquals(0, BackpackLayout.unlockedOnPanel(5, true));
        assertEquals(54, BackpackLayout.unlockedOnPanel(80, false));
        assertEquals(26, BackpackLayout.unlockedOnPanel(80, true));
        assertEquals(54, BackpackLayout.unlockedOnPanel(999, true));
    }

    @Test
    void columnMathRoundsCorrectly() {
        assertEquals(0, BackpackLayout.columnsFor(0));
        assertEquals(1, BackpackLayout.columnsFor(1));
        assertEquals(1, BackpackLayout.columnsFor(9));
        assertEquals(2, BackpackLayout.columnsFor(10));
        assertEquals(6, BackpackLayout.columnsFor(54));
        assertEquals(9, BackpackLayout.slotsInColumn(54, 0));
        assertEquals(9, BackpackLayout.slotsInColumn(20, 1));
        assertEquals(2, BackpackLayout.slotsInColumn(20, 2));
        assertEquals(0, BackpackLayout.slotsInColumn(20, 3));
    }
}
