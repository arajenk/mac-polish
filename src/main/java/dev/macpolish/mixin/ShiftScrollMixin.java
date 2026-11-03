package dev.macpolish.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.SDLEventHandler;
import dev.macpolish.MacPolishConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Optional fix: Shift+scroll goes the opposite way to normal scrolling (flipShiftScroll in the config).
 *
 * <p>macOS turns Shift+wheel into horizontal scroll, and vanilla converts that back to vertical in
 * SDLEventHandler. That direction is right on a stock setup. It only comes out reversed when something
 * changes vertical scrolling but not horizontal, such as Scroll Reverser setting a mouse to Classic while
 * macOS stays on natural, so this is off by default. It negates the result only when that conversion ran.
 */
@Mixin(SDLEventHandler.class)
abstract class ShiftScrollMixin {
	// The method's double locals are the horizontal delta (ordinal 0) and vertical delta (ordinal 1).
	// Its only boolean local holds the result of isShiftInvertedScroll.
	@ModifyVariable(method = "handleMouseWheelEvent", at = @At("STORE"), ordinal = 1)
	private double macpolish$matchNormalScrollDirection(double vertical, @Local boolean shiftInverted) {
		return shiftInverted && MacPolishConfig.get().flipShiftScroll() ? -vertical : vertical;
	}
}
