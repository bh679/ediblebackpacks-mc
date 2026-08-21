package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.config.EBConfig.ResetMode;
import games.brennan.ediblebackpacks.storage.BackpackPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackPolicyTest {

    @Test
    void onAlwaysResets() {
        assertTrue(BackpackPolicy.shouldResetOnDeath(ResetMode.ON, null));
        assertTrue(BackpackPolicy.shouldResetOnDeath(ResetMode.ON, false));
    }

    @Test
    void offNeverResets() {
        assertFalse(BackpackPolicy.shouldResetOnDeath(ResetMode.OFF, null));
        assertFalse(BackpackPolicy.shouldResetOnDeath(ResetMode.OFF, true));
    }

    @Test
    void defaultFollowsHostAndFallsBackToPersist() {
        assertFalse(BackpackPolicy.shouldResetOnDeath(ResetMode.DEFAULT, null));
        assertFalse(BackpackPolicy.shouldResetOnDeath(ResetMode.DEFAULT, false));
        assertTrue(BackpackPolicy.shouldResetOnDeath(ResetMode.DEFAULT, true));
    }

    @Test
    void grantsTopUpToTheCap() {
        assertEquals(1, BackpackPolicy.effectiveGrant(0, 1, 108));
        assertEquals(9, BackpackPolicy.effectiveGrant(99, 9, 108));
        // Near the cap an upgraded backpack grants what's left, not nothing —
        // there is no craft-back-to-singles escape hatch (non-stackable item).
        assertEquals(8, BackpackPolicy.effectiveGrant(100, 9, 108));
        assertEquals(0, BackpackPolicy.effectiveGrant(108, 9, 108));
        assertEquals(0, BackpackPolicy.effectiveGrant(108, 1, 108));
    }

    @Test
    void clampUnlocked() {
        assertEquals(0, BackpackPolicy.clampUnlocked(-5, 108));
        assertEquals(7, BackpackPolicy.clampUnlocked(7, 108));
        assertEquals(108, BackpackPolicy.clampUnlocked(9999, 108));
    }
}
