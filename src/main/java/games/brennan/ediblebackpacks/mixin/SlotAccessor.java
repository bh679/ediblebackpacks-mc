package games.brennan.ediblebackpacks.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets {@code BackpackSlot} replace the {@code Container} identity it inherits
 * from NeoForge's {@code SlotItemHandler}, which hands every one of its slots
 * the same shared {@code emptyInventory} instance.
 *
 * <p>That identity is what vanilla matches slots by in
 * {@code AbstractContainerMenu#transferState} (run whenever a player closes a
 * container): slots are keyed on {@code (container, containerSlot)}, so our
 * backpack slot <i>n</i> collides with slot <i>n</i> of any other mod's
 * {@code SlotItemHandler} menu and inherits its sync bookkeeping. Giving the
 * panels their own marker container makes them unmatchable from outside.</p>
 */
@Mixin(Slot.class)
public interface SlotAccessor {

    @Mutable
    @Accessor("container")
    void ediblebackpacks$setContainer(Container container);

    /**
     * Moves a slot horizontally. {@code Slot#x} is final in vanilla, but it is read only by
     * the client — rendering and {@code AbstractContainerScreen#findSlot} hit-testing; the
     * server never looks at it — so re-placing the panels is client-local state. See
     * {@code client/BackpackPanelLayout}.
     */
    @Mutable
    @Accessor("x")
    void ediblebackpacks$setX(int x);
}
