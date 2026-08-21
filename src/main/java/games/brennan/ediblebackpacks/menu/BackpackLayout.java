package games.brennan.ediblebackpacks.menu;

/**
 * Pure slot-geometry math for the two 8×9 backpack panels flanking the
 * vanilla survival inventory screen (176×166 GUI). No Minecraft imports —
 * unit-tested.
 *
 * <p>Slot indices 0..71 fill the LEFT panel row-major (8 columns × 9 rows),
 * indices 72..143 the RIGHT panel. Coordinates are relative to the screen's
 * {@code leftPos}/{@code topPos} like every vanilla slot: the left panel sits
 * at negative x, the right panel past the 176px GUI width.</p>
 */
public final class BackpackLayout {

    public static final int COLS = 8;
    public static final int ROWS = 9;
    public static final int PANEL_SLOTS = COLS * ROWS;   // 72
    public static final int MAX_SLOTS = PANEL_SLOTS * 2; // 144

    public static final int SLOT = 18;

    /** Left panel: 8 cols ending 8px left of the GUI edge. */
    public static final int LEFT_X0 = -(COLS * SLOT + 8);   // -152
    /** Right panel: starts 8px right of the 176px GUI edge. */
    public static final int RIGHT_X0 = 176 + 8;             // 184
    /** Top of both panels. */
    public static final int Y0 = 2;

    private BackpackLayout() {}

    /** True when the index belongs to the right-hand panel. */
    public static boolean isRightPanel(int index) {
        return index >= PANEL_SLOTS;
    }

    /** Slot x relative to the screen's leftPos. */
    public static int slotX(int index) {
        int local = index % PANEL_SLOTS;
        int col = local % COLS;
        return (isRightPanel(index) ? RIGHT_X0 : LEFT_X0) + col * SLOT;
    }

    /** Slot y relative to the screen's topPos. */
    public static int slotY(int index) {
        int local = index % PANEL_SLOTS;
        int row = local / COLS;
        return Y0 + row * SLOT;
    }

    /** Unlocked slots that sit on the given panel (for panel-height rendering). */
    public static int unlockedOnPanel(int unlocked, boolean rightPanel) {
        int clamped = Math.max(0, Math.min(unlocked, MAX_SLOTS));
        return rightPanel
            ? Math.max(0, clamped - PANEL_SLOTS)
            : Math.min(clamped, PANEL_SLOTS);
    }

    /** Rows (1..9) needed to show {@code slotsOnPanel} slots; 0 when the panel is empty. */
    public static int rowsFor(int slotsOnPanel) {
        return (Math.max(0, Math.min(slotsOnPanel, PANEL_SLOTS)) + COLS - 1) / COLS;
    }
}
