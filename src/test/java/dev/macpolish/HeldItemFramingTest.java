package dev.macpolish;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HeldItemFramingTest {
	private static final double EPSILON = 1e-4;
	private static final double TAN_HALF_FOV = Math.tan(Math.toRadians(35));

	@Test
	void sixteenByNineIsUnchanged() {
		assertEquals(0f, HeldItemFraming.shearTowardCenter(1920, 1080), EPSILON);
	}

	@Test
	void widerScreensAreUnchanged() {
		assertEquals(0f, HeldItemFraming.shearTowardCenter(3440, 1440), EPSILON);
	}

	@Test
	void macScreenKeepsTheSixteenByNineEdgeGap() {
		assertEquals(TAN_HALF_FOV * (16.0 / 9.0 - 3024.0 / 1899.0), HeldItemFraming.shearTowardCenter(3024, 1899), EPSILON);
	}

	@Test
	void narrowWindowsAreCappedAtFourByThree() {
		double cap = TAN_HALF_FOV * (16.0 / 9.0 - 4.0 / 3.0);
		assertEquals(cap, HeldItemFraming.shearTowardCenter(800, 1200), EPSILON);
		assertEquals(cap, HeldItemFraming.shearTowardCenter(1, 1000), EPSILON);
	}

	@Test
	void invalidSizesDoNothing() {
		assertEquals(0f, HeldItemFraming.shearTowardCenter(0, 0), EPSILON);
		assertEquals(0f, HeldItemFraming.shearTowardCenter(1920, 0), EPSILON);
	}
}
