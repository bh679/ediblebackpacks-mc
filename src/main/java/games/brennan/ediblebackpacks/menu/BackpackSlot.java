package games.brennan.ediblebackpacks.menu;

import games.brennan.ediblebackpacks.client.BackpackPanelLayout;
import games.brennan.ediblebackpacks.mixin.SlotAccessor;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * One backpack slot appended to the vanilla {@code InventoryMenu}. All 108
 * slots always exist (stable indices for vanilla container sync); locked
 * slots are inactive and refuse interaction.
 *
 * <p>The backing handler is looked up from the player's attachment on every
 * access instead of being captured once. NeoForge <em>replaces</em> the
 * attachment object when it deserializes it (login) or copies it (respawn),
 * both of which happen after {@code InventoryMenu} is built in the
 * {@code Player} constructor — a captured handler would be orphaned from the
 * first relog on, silently voiding anything placed in the panels.</p>
 *
 * <p>The inherited {@code Container} identity is replaced with a marker owned
 * by this menu. NeoForge gives every {@code SlotItemHandler} the same shared
 * {@code emptyInventory}, and vanilla keys slots on {@code (container,
 * containerSlot)} when a player closes a container, which would let another
 * mod's item-handler slots overwrite this panel's sync bookkeeping — see
 * {@link SlotAccessor}.</p>
 *
 * <p>{@link #isActive()} is display-only — the server never consults it for click
 * validation, so {@link #mayPlace}/{@link #mayPickup} carry the authoritative lock
 * check. That asymmetry is why the recipe book makes the panels MOVE rather than
 * switch off: an inactive slot still accepts items, so hiding one hides the items
 * quick-move puts in it. See {@code client/BackpackPanelLayout}.</p>
 */
public final class BackpackSlot extends SlotItemHandler {

    private final Player owner;
    private final int backpackIndex;

    public BackpackSlot(Player owner, int backpackIndex, Container marker) {
        // The super handler is only a seed; getItemHandler() below is what
        // every read/write actually goes through.
        super(owner.getData(ModAttachments.BACKPACK).items(), backpackIndex,
            BackpackLayout.slotX(backpackIndex), BackpackLayout.slotY(backpackIndex));
        this.owner = owner;
        this.backpackIndex = backpackIndex;
        // Same kind of object SlotItemHandler would have left here (an empty
        // SimpleContainer), just not one shared with every other mod's slots.
        ((SlotAccessor) (Object) this).ediblebackpacks$setContainer(marker);
    }

    /** Always the player's current attachment, never the one captured at menu build time. */
    @Override
    public IItemHandler getItemHandler() {
        return owner.getData(ModAttachments.BACKPACK).items();
    }

    /** This slot's index into the backpack, which is also its place in the fill order. */
    public int backpackIndex() {
        return backpackIndex;
    }

    private boolean unlockedNow() {
        return backpackIndex < owner.getData(ModAttachments.BACKPACK).unlocked();
    }

    @Override
    public boolean isActive() {
        if (!unlockedNow()) return false;
        // Client-only: the panels normally MOVE out of the recipe book's way rather than
        // switch off — a slot that is inactive but still accepts items swallows quick-moved
        // stacks in plain sight of nobody. Only a screen too narrow for the moved layout
        // hides them. On the logical server isClientSide is false, so the client class
        // never loads there.
        if (owner.level().isClientSide && BackpackPanelLayout.hidden()) return false;
        return true;
    }

    // mayPlace/getMaxStackSize are re-implemented rather than delegated to
    // super: SlotItemHandler reads its captured field directly in those three,
    // where every other accessor goes through getItemHandler().

    @Override
    public boolean mayPlace(ItemStack stack) {
        return unlockedNow() && !stack.isEmpty() && getItemHandler().isItemValid(backpackIndex, stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return unlockedNow() && super.mayPickup(player);
    }

    @Override
    public int getMaxStackSize() {
        return getItemHandler().getSlotLimit(backpackIndex);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), getItemHandler().getSlotLimit(backpackIndex));
    }
}
