package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.menu.BackpackLayout;
import games.brennan.ediblebackpacks.menu.BackpackSlot;
import games.brennan.ediblebackpacks.mixin.SlotAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/**
 * Where the panels sit this frame, client-side.
 *
 * <p>The recipe book covers the whole left panel. The panels used to simply switch off while
 * it was open — but {@code isActive} is display-only: vanilla's {@code moveItemStackTo} asks
 * {@code mayPlace}, never {@code isActive}, so the slots kept accepting items nobody could
 * see. A shift-clicked crafting result would leave the grid and land nowhere visible.</p>
 *
 * <p>So instead of hiding them, {@link Mode#BOOK} moves both panels right of the GUI, clear
 * of the book (see {@link BackpackLayout#slotX(int, boolean)}). {@link Mode#HIDDEN} remains
 * only for screens too narrow to fit them there, and is the one case where
 * {@link BackpackSlot#isActive()} still switches slots off.</p>
 *
 * <p>Slot positions are client-local — the server never reads {@code Slot#x} — so rewriting
 * them per frame is safe. The mode is resolved during the background render, which runs
 * before the frame's input handling, so hit-testing follows the layout that was drawn.</p>
 */
public final class BackpackPanelLayout {

    /** Panel placement for the current frame. */
    public enum Mode {
        /** Recipe book closed: one panel either side of the GUI. */
        NORMAL,
        /** Recipe book open: both panels right of the GUI, clear of the book. */
        BOOK,
        /** Nowhere to put them — too narrow. The only case where the slots go inert. */
        HIDDEN
    }

    private static Mode mode = Mode.NORMAL;

    private BackpackPanelLayout() {}

    /** The mode the panels were last laid out in. */
    public static Mode mode() {
        return mode;
    }

    /** True while the panels have nowhere to go and their slots must be inert. */
    public static boolean hidden() {
        return mode == Mode.HIDDEN;
    }

    /**
     * Resolves the mode for {@code unlocked} slots and writes the slot positions to match.
     * Call before anything reads a slot's position or {@code isActive}.
     *
     * @return the resolved mode, so the caller can draw the chrome the same way
     */
    public static Mode update(AbstractContainerScreen<?> screen, int unlocked) {
        mode = resolve(screen, unlocked);
        boolean bookMode = mode == Mode.BOOK;
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof BackpackSlot backpack) {
                ((SlotAccessor) (Object) slot)
                    .ediblebackpacks$setX(BackpackLayout.slotX(backpack.backpackIndex(), bookMode));
            }
        }
        return mode;
    }

    private static Mode resolve(AbstractContainerScreen<?> screen, int unlocked) {
        if (!ClientPanelState.recipeBookOpen()) return Mode.NORMAL;
        // The book has already pushed the GUI right; the panels get what is left over.
        return screen.getGuiLeft() + BackpackLayout.rightExtent(unlocked, true) <= screen.width
            ? Mode.BOOK
            : Mode.HIDDEN;
    }
}
