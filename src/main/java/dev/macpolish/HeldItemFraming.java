package dev.macpolish;

/**
 * Works out how to slide the first-person hand so it looks the way it does at 16:9.
 *
 * <p>The vertical field of view is fixed, so at the same window height the hand is the same image on every
 * screen, and only the distance to the side edge differs. Moving the hand in 3D would change the angle it is
 * seen from, so this shears it instead: {@code x' = x + shear * z}. Dividing by depth, that moves every point
 * by the same amount on screen, leaving the image itself exactly as it is at 16:9.
 */
public final class HeldItemFraming {
	private static final double REFERENCE_ASPECT = 16.0 / 9.0;
	/** Below this the item would crowd the crosshair, for example in a tall, narrow window. */
	private static final double MIN_ASPECT = 4.0 / 3.0;
	/** The game's base FOV for the hand, which ignores the player's FOV setting. */
	private static final double HAND_FOV_DEGREES = 70.0;
	private static final double TAN_HALF_FOV = Math.tan(Math.toRadians(HAND_FOV_DEGREES / 2.0));

	private HeldItemFraming() {
	}

	/** @return the shear that moves the hand toward the screen center; 0 if no change is needed */
	public static float shearTowardCenter(int windowWidth, int windowHeight) {
		if (windowWidth <= 0 || windowHeight <= 0) {
			return 0f;
		}
		double aspect = Math.max((double) windowWidth / windowHeight, MIN_ASPECT);
		return (float) (TAN_HALF_FOV * Math.max(REFERENCE_ASPECT - aspect, 0.0));
	}
}
