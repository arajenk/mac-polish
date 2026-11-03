package dev.macpolish.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.macpolish.HeldItemFraming;
import dev.macpolish.MacPolishConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frames held items on Mac displays the way they look at 16:9, see {@link HeldItemFraming}. An empty hand is left alone.
 *
 * <p>The move is applied before any of the arm's own transforms, so swing, equip, eat and block animations
 * all play as normal around the new position.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class HeldItemFramingMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(
			method = "submitArmWithItem",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER))
	private void macpolish$frameHeldItem(
			CallbackInfo ci,
			@Local(argsOnly = true) FirstPersonHandsAndItemsRenderState state,
			@Local(argsOnly = true) ItemStack item,
			@Local(argsOnly = true) PoseStack poseStack,
			@Local(ordinal = 0) boolean isMainHand,
			@Local HumanoidArm arm) {
		if (!MacPolishConfig.get().heldItemFraming()) {
			return;
		}
		// The bare hand stays where vanilla puts it, as framing it reads as too close to the center.
		if (item.isEmpty()) {
			return;
		}
		// A map held with both hands is drawn in the center of the screen.
		if (isMainHand && item.has(DataComponents.MAP_ID) && state.offHandItem.isEmpty()) {
			return;
		}
		Window window = this.minecraft.getWindow();
		float shear = HeldItemFraming.shearTowardCenter(window.getWidth(), window.getHeight());
		if (shear != 0f) {
			// The camera looks down -z, so a positive shear moves things toward screen left.
			poseStack.mulPose(new Matrix4f().m20(arm == HumanoidArm.RIGHT ? shear : -shear));
		}
	}
}
