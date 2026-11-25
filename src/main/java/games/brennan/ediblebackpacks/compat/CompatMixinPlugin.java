package games.brennan.ediblebackpacks.compat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies a compat mixin only when the mod it patches is installed. Edible Backpacks never
 * depends on those mods, not even to compile: each mixin targets its class by name
 * ({@code @Pseudo}), and this plugin keeps it from being applied at all when the mod is absent.
 *
 * <p>A mixin opts in by living in a sub-package named after the mod id it needs, e.g.
 * {@code compat.mixin.betterend.*} needs {@code betterend}.</p>
 */
public final class CompatMixinPlugin implements IMixinConfigPlugin {

    private static final String MIXIN_PACKAGE = "games.brennan.ediblebackpacks.compat.mixin.";

    /** The mod id a compat mixin needs, read from its package, or {@code null} if it names none. */
    public static String requiredModId(String mixinClassName) {
        if (mixinClassName == null || !mixinClassName.startsWith(MIXIN_PACKAGE)) return null;
        String rest = mixinClassName.substring(MIXIN_PACKAGE.length());
        int dot = rest.indexOf('.');
        return dot > 0 ? rest.substring(0, dot) : null;
    }

    /** Pure decision, unit-tested: apply only when the named mod is loaded. */
    public static boolean shouldApply(String mixinClassName, Predicate<String> modLoaded) {
        String modId = requiredModId(mixinClassName);
        return modId != null && modLoaded.test(modId);
    }

    private static boolean isModLoaded(String modId) {
        LoadingModList mods = LoadingModList.get();
        return mods != null && mods.getModFileById(modId) != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return shouldApply(mixinClassName, CompatMixinPlugin::isModLoaded);
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
