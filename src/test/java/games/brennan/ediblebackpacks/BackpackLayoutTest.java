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
                assertTrue(x >= BackpackLayout.RIGHT_X0, "right-panel slot left of panel: " + i);
            } else {
                assertTrue(x < 0, "left-panel slot not at negative x: " + i);
            }
        }
    }

    @Test
    void panelSplitIs72_72() {
        assertFalse(BackpackLayout.isRightPanel(71));
        assertTrue(BackpackLayout.isRightPanel(72));
        assertEquals(144, BackpackLayout.MAX_SLOTS);
    }

    @Test
    void unlockedDistributionFillsLeftThenRight() {
        assertEquals(0, BackpackLayout.unlockedOnPanel(0, false));
        assertEquals(5, BackpackLayout.unlockedOnPanel(5, false));
        assertEquals(0, BackpackLayout.unlockedOnPanel(5, true));
        assertEquals(72, BackpackLayout.unlockedOnPanel(100, false));
        assertEquals(28, BackpackLayout.unlockedOnPanel(100, true));
        assertEquals(72, BackpackLayout.unlockedOnPanel(999, true));
    }

    @Test
    void rowsForRoundsUp() {
        assertEquals(0, BackpackLayout.rowsFor(0));
        assertEquals(1, BackpackLayout.rowsFor(1));
        assertEquals(1, BackpackLayout.rowsFor(8));
        assertEquals(2, BackpackLayout.rowsFor(9));
        assertEquals(9, BackpackLayout.rowsFor(72));
    }
}
