package games.brennan.ediblebackpacks.client;

import games.brennan.ediblebackpacks.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The open/close button on the survival inventory screen, sitting directly above the offhand
 * (shield) slot. Vanilla recipe-button footprint of 20×18, vanilla button chrome, with an
 * {@code edible_backpack} item drawn on it as the glyph — no new texture asset, which also
 * keeps the icon in step if the item art ever changes.
 *
 * <p>Pressing it flips {@code BackpackData#panelsOpen} on the client and tells the server
 * (see {@link ClientPanelState#toggle()}). Closing is a real lock, not a hide: the slots go
 * inert on both sides, so a shift-click while closed just does what vanilla would.</p>
 *
 * <p>Position is rewritten every frame from {@code BackpackScreenPanels} — opening the
 * recipe book slides the whole GUI right, and vanilla only moves its own widgets.</p>
 */
public final class BackpackToggleButton extends Button {

    public static final int WIDTH = 20;
    public static final int HEIGHT = 18;
    /**
     * Left edge relative to the screen's leftPos. The offhand slot is at x 77..93, so a 20px
     * button centred over it would start at 75 — one pixel into the player-portrait viewport
     * (26..75). Starting at 76 clears the viewport and still covers the slot; the crafting
     * grid does not begin until 98.
     */
    public static final int X_OFFSET = 76;
    /**
     * Top edge relative to the screen's topPos: the offhand slot's chrome starts at y 61, so
     * an 18px button ending at 60 sits right on top of it without touching.
     */
    public static final int Y_OFFSET = 42;

    private final ItemStack icon = new ItemStack(ModItems.EDIBLE_BACKPACK.get());

    public BackpackToggleButton() {
        super(0, 0, WIDTH, HEIGHT, Component.empty(), b -> {
            ClientPanelState.toggle();
            ((BackpackToggleButton) b).refreshTooltip();
        }, DEFAULT_NARRATION);
        refreshTooltip();
    }

    /** Says what the next press will do, so it tracks the state rather than describing it. */
    public void refreshTooltip() {
        setTooltip(Tooltip.create(Component.translatable(
            ClientPanelState.panelsOpen() ? "ediblebackpacks.button.close" : "ediblebackpacks.button.open")));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        // Centred in the 20×18 face: the item renders 16×16.
        graphics.renderItem(icon, getX() + 2, getY() + 1);
    }
}
