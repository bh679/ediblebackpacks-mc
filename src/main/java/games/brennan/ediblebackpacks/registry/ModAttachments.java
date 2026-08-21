package games.brennan.ediblebackpacks.registry;

import games.brennan.ediblebackpacks.EdibleBackpacks;
import games.brennan.ediblebackpacks.storage.BackpackData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * AttachmentType registry. One serializable per-player attachment holds the
 * unlocked-slot count + backing inventory. {@code copyOnDeath} makes
 * persist-through-death the standalone default; the death-reset policy (see
 * {@code BackpackEvents}) clears it explicitly when a host/server opts in.
 */
public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EdibleBackpacks.MOD_ID);

    public static final Supplier<AttachmentType<BackpackData>> BACKPACK =
        TYPES.register("backpack",
            () -> AttachmentType.serializable(BackpackData::new)
                .copyOnDeath()
                .build()
        );

    private ModAttachments() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
