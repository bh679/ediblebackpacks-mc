package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.compat.CompatMixinPlugin;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Compat mixins apply only when the mod named by their package is installed. */
class CompatMixinPluginTest {

    private static final String SMELTER =
        "games.brennan.ediblebackpacks.compat.mixin.betterend.EndStoneSmelterMenuMixin";

    @Test
    void readsTheModIdFromThePackage() {
        assertEquals("betterend", CompatMixinPlugin.requiredModId(SMELTER));
        assertNull(CompatMixinPlugin.requiredModId("games.brennan.ediblebackpacks.compat.mixin.Loose"));
        assertNull(CompatMixinPlugin.requiredModId("games.brennan.ediblebackpacks.mixin.ChestMenuMixin"));
    }

    @Test
    void appliesOnlyWithTheModInstalled() {
        assertTrue(CompatMixinPlugin.shouldApply(SMELTER, Set.of("betterend")::contains));
        assertFalse(CompatMixinPlugin.shouldApply(SMELTER, Set.of("betternether")::contains));
        assertFalse(CompatMixinPlugin.shouldApply(SMELTER, id -> false));
    }
}
