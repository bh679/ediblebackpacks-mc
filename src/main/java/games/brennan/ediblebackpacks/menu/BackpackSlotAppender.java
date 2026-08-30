package games.brennan.ediblebackpacks.menu;

import games.brennan.ediblebackpacks.mixin.AbstractContainerMenuAccessor;
import games.brennan.ediblebackpacks.mixin.SlotAccessor;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

/**
 * The one way backpack slots get attached to a menu, shared by every menu mixin so the two
 * invariants below can only be got right once.
 *
 * <p>Lives outside the mixin package on purpose: Mixin refuses to class-load an ordinary class
 * from a package it owns, and the mixins would fail the moment they called into it. The accessor
 * interfaces it uses are exempt from that rule, being generated interfaces.</p>
 */
public final class BackpackSlotAppender {

    private BackpackSlotAppender() {}

    /**
     * Appends all {@link BackpackLayout#MAX_SLOTS} slots to {@code menu}, in index order.
     *
     * <p>Always the full 108, never just the unlocked ones: the slot indices have to stay
     * stable for vanilla's container sync, and locked slots are inert anyway (see
     * {@link BackpackSlot}). Called on BOTH logical sides, so both build the same list.</p>
     *
     * <p>Each menu gets its OWN marker container. NeoForge hands every {@code SlotItemHandler}
     * the same shared {@code emptyInventory}, and vanilla's {@code transferState} matches slots
     * by {@code (container, containerSlot)} on every container close — a shared identity lets
     * another mod's item-handler slots overwrite this menu's sync bookkeeping. See
     * {@link SlotAccessor}.</p>
     */
    public static void append(AbstractContainerMenuAccessor menu, Player owner) {
        SimpleContainer marker = new SimpleContainer(0);
        for (int i = 0; i < BackpackLayout.MAX_SLOTS; i++) {
            // The slot resolves the player's attachment on every access — see BackpackSlot:
            // NeoForge swaps the attachment object out on login and respawn, and the survival
            // menu at least is built before either happens.
            menu.ediblebackpacks$invokeAddSlot(new BackpackSlot(owner, i, marker));
        }
    }
}
