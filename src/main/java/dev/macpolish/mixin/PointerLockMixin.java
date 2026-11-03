package dev.macpolish.mixin;

import dev.macpolish.CocoaPointerLock;
import dev.macpolish.MacPolishConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fix 1: after Cmd-Tab the macOS pointer stays unlocked, so the cursor shows at the screen edges and
 * the menu bar and Dock can take input.
 *
 * <p>SDL can be left believing relative mode is on when it actually skipped locking the pointer, see
 * {@link CocoaPointerLock}. This repairs that during play and discards mouse movement for a few frames
 * so the camera cannot jump.
 */
@Mixin(MouseHandler.class)
abstract class PointerLockMixin {
	@Unique
	private static final int FRAMES_TO_DISCARD_MOVEMENT = 3;

	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	private boolean mouseGrabbed;
	@Shadow
	private boolean ignoreFirstMove;
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Unique
	private int macpolish$framesToDiscard;

	// Runs every frame on the render thread, which is the main thread on macOS.
	@Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
	private void macpolish$repairPointerLock(CallbackInfo ci) {
		if (MacPolishConfig.get().pointerLockRepair() && this.mouseGrabbed && this.minecraft.isWindowActive() && this.minecraft.gui.screen() == null
				&& CocoaPointerLock.clearIfStuck(this.minecraft.getWindow().handle())) {
			this.ignoreFirstMove = true;
			this.macpolish$framesToDiscard = FRAMES_TO_DISCARD_MOVEMENT;
		}
		if (this.macpolish$framesToDiscard > 0) {
			this.macpolish$framesToDiscard--;
			this.accumulatedDX = 0;
			this.accumulatedDY = 0;
		}
	}
}
