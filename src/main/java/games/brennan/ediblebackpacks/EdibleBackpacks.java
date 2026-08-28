package games.brennan.ediblebackpacks;

import games.brennan.ediblebackpacks.config.EBClientConfig;
import games.brennan.ediblebackpacks.config.EBConfig;
import games.brennan.ediblebackpacks.registry.ModAttachments;
import games.brennan.ediblebackpacks.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * Edible Backpacks — eat a backpack, gain a permanent +1 backpack storage
 * slot. Storage renders as two 6×9 panels flanking the vanilla survival
 * inventory screen (up to {@link games.brennan.ediblebackpacks.menu.BackpackLayout#MAX_SLOTS}
 * slots).
 *
 * <p>Standalone generic mod: no Dungeon Train dependency. Host mods can steer
 * the death-reset default via {@link EdibleBackpacksApi}.</p>
 */
@Mod(EdibleBackpacks.MOD_ID)
public final class EdibleBackpacks {

    public static final String MOD_ID = "ediblebackpacks";

    public EdibleBackpacks(IEventBus modBus, ModContainer modContainer) {
        ModItems.register(modBus);
        ModAttachments.register(modBus);

        // SERVER config: per-world, auto-synced to clients on login.
        modContainer.registerConfig(ModConfig.Type.SERVER, EBConfig.SPEC);
        // CLIENT config: the toggle button's own appearance — never read on a server.
        modContainer.registerConfig(ModConfig.Type.CLIENT, EBClientConfig.SPEC);
    }
}
