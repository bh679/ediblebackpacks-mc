package games.brennan.ediblebackpacks.menu;

/**
 * Pure slot-geometry math for the two 6×9 backpack panels flanking the
 * vanilla survival inventory screen (176×166 GUI). No Minecraft imports —
 * unit-tested.
 *
 * <p>Fill order: slots unlock <b>down a column, then across</b>, starting on
 * the column CLOSEST to the player's inventory and growing outward. Indices
 * 0..53 fill the LEFT panel (its rightmost column first, moving left),
 * indices 54..107 the RIGHT panel (its leftmost column first, moving right).
 * Coordinates are relative to the screen's {@code leftPos}/{@code topPos}
 * like every vanilla slot.</p>
 */
public final class BackpackLayout {

    public static final int COLS = 6;
    public static final int ROWS = 9;
    public static final int PANEL_SLOTS = COLS * ROWS;   // 54
    public static final int MAX_SLOTS = PANEL_SLOTS * 2; // 108

    public static final int SLOT = 18;

    /** Gap between a panel's inner edge and the GUI edge. */
    public static final int GAP = 8;
    /** Left panel's innermost (closest) column x: right edge 8px left of the GUI. */
    public static final int LEFT_INNER_X = -(GAP + SLOT);   // -26
    /** Right panel's innermost (closest) column x: 8px right of the 176px GUI edge. */
    public static final int RIGHT_INNER_X = 176 + GAP;      // 184
    /** Top of both panels. */
    public static final int Y0 = 2;
    /** Chrome thickness drawn around a panel's slots, on every edge. */
    public static final int BORDER = 4;

    private BackpackLayout() {}

    /** True when the index belongs to the right-hand panel. */
    public static boolean isRightPanel(int index) {
        return index >= PANEL_SLOTS;
    }

    /** Column fill-order index within the panel: 0 = closest to the GUI, growing outward. */
    public static int columnOf(int index) {
        return (index % PANEL_SLOTS) / ROWS;
    }

    /** Row within the column (0 = top): fill goes DOWN the column first. */
    public static int rowOf(int index) {
        return (index % PANEL_SLOTS) % ROWS;
    }

    /** Slot x relative to the screen's leftPos. */
    public static int slotX(int index) {
        int col = columnOf(index);
        return isRightPanel(index)
            ? RIGHT_INNER_X + col * SLOT     // right panel grows rightward
            : LEFT_INNER_X - col * SLOT;     // left panel grows leftward
    }

    /** Slot y relative to the screen's topPos. */
    public static int slotY(int index) {
        return Y0 + rowOf(index) * SLOT;
    }

    /** Unlocked slots that sit on the given panel (left panel fills first). */
    public static int unlockedOnPanel(int unlocked, boolean rightPanel) {
        int clamped = Math.max(0, Math.min(unlocked, MAX_SLOTS));
        return rightPanel
            ? Math.max(0, clamped - PANEL_SLOTS)
            : Math.min(clamped, PANEL_SLOTS);
    }

    /** Columns (1..6) in use for {@code slotsOnPanel} slots; 0 when the panel is empty. */
    public static int columnsFor(int slotsOnPanel) {
        return (Math.max(0, Math.min(slotsOnPanel, PANEL_SLOTS)) + ROWS - 1) / ROWS;
    }

    /** Slots present in column {@code col} (fill-order index) for {@code slotsOnPanel} slots: 0..9. */
    public static int slotsInColumn(int slotsOnPanel, int col) {
        int n = Math.max(0, Math.min(slotsOnPanel, PANEL_SLOTS));
        return Math.max(0, Math.min(n - col * ROWS, ROWS));
    }
}
