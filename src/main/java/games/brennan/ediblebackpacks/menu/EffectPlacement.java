package games.brennan.ediblebackpacks.menu;

/**
 * Where the vanilla mob-effect stack goes once the RIGHT backpack panel is
 * occupying the strip it normally renders into. No Minecraft imports —
 * unit-tested.
 *
 * <p>Vanilla draws the stack at {@code leftPos + imageWidth + 2}, starting at
 * {@code topPos} and running down — exactly through the right panel. The fix,
 * in preference order:</p>
 *
 * <ol>
 *   <li>Lift the whole stack so it sits <b>above</b> the panel's top edge.</li>
 *   <li>If the screen is too short for that, slide it sideways past the panel's
 *       outer edge instead (compact when what is left is narrower than a
 *       full-size entry).</li>
 *   <li>If neither fits, push it as high as the screen allows and accept that
 *       the tail overlaps — there is nowhere else for it to go.</li>
 * </ol>
 */
public final class EffectPlacement {

    /** Height of one vanilla effect entry (its background sprite). */
    public static final int ENTRY = 32;
    /** Width of a full-size entry — icon plus name and duration. */
    public static final int WIDE = 120;
    /** Width of a compact, icon-only entry; also the least room worth sliding into. */
    public static final int NARROW = 32;
    /** Breathing room between the stack and the panel it is dodging. */
    public static final int GAP = 2;
    /** The stack never goes higher than this, however tall it is. */
    public static final int MIN_TOP = 2;

    private EffectPlacement() {}

    /** Resolved screen position for the stack. {@code compact} = icon-only entries. */
    public record Placement(int left, int top, boolean compact) {}

    /** Vanilla's per-entry stride: 33px, compressed once more than 5 effects are active. */
    public static int stride(int count) {
        return count > 5 ? 132 / (count - 1) : 33;
    }

    /** Total height a stack of {@code count} entries covers. */
    public static int stackHeight(int count) {
        return count <= 0 ? 0 : (count - 1) * stride(count) + ENTRY;
    }

    /** True when the right panel has any slots and therefore covers the effect strip. */
    public static boolean rightPanelPresent(int unlocked) {
        return BackpackLayout.unlockedOnPanel(unlocked, true) > 0;
    }

    /** Screen y of the top edge of the right panel's chrome. */
    public static int panelTop(int guiTop) {
        return guiTop + BackpackLayout.Y0 - BackpackLayout.BORDER;
    }

    /** Screen x of the right panel's outer edge; {@code guiLeft} when the panel is empty. */
    public static int panelRight(int guiLeft, int unlocked) {
        int cols = BackpackLayout.columnsFor(BackpackLayout.unlockedOnPanel(unlocked, true));
        if (cols <= 0) return guiLeft;
        return guiLeft + BackpackLayout.RIGHT_INNER_X + cols * BackpackLayout.SLOT + BackpackLayout.BORDER;
    }

    /** True when the whole stack clears the panel's top edge without leaving the screen. */
    public static boolean fitsAbove(int guiTop, int count) {
        return count > 0 && panelTop(guiTop) - GAP - stackHeight(count) >= MIN_TOP;
    }

    /**
     * @param vanillaLeft   x vanilla picked ({@code leftPos + imageWidth + 2})
     * @param vanillaCompact whether vanilla picked compact entries for that x
     * @param unlocked      the player's unlocked backpack-slot count
     * @param count         effects vanilla is about to draw
     */
    public static Placement resolve(int guiLeft, int guiTop, int screenWidth,
                                    int vanillaLeft, boolean vanillaCompact,
                                    int unlocked, int count) {
        if (count <= 0 || !rightPanelPresent(unlocked)) {
            return new Placement(vanillaLeft, guiTop, vanillaCompact);
        }
        if (fitsAbove(guiTop, count)) {
            return new Placement(vanillaLeft, panelTop(guiTop) - GAP - stackHeight(count), vanillaCompact);
        }
        int aside = panelRight(guiLeft, unlocked) + GAP;
        int room = screenWidth - aside;
        if (room >= NARROW) {
            return new Placement(aside, guiTop, room < WIDE);
        }
        return new Placement(vanillaLeft, MIN_TOP, vanillaCompact);
    }
}
