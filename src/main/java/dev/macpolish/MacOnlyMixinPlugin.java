package dev.macpolish;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies this mod's mixins only on macOS, so on other systems the game classes stay untouched.
 * New fixes are covered automatically once they are listed in macpolish.mixins.json.
 */
public final class MacOnlyMixinPlugin implements IMixinConfigPlugin {
	private static final boolean MACOS =
			System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("mac");

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return MACOS;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
