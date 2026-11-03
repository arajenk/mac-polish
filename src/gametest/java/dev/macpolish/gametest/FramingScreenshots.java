package dev.macpolish.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.macpolish.MacPolishConfig;
import dev.macpolish.MacPolishConfigScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;

/** Renders the held item at 16:9 and at Mac aspect ratios, and drives the settings screen with real clicks. */
public final class FramingScreenshots implements FabricClientGameTest {
	private static final int HEIGHT = 720;
	private static final int MAC_WIDTH = 1146;
	private static final int[][] SIZES = {{1280, HEIGHT}, {MAC_WIDTH, HEIGHT}, {1152, HEIGHT}};
	private static final int HELD_ITEM_ROW = 2;

	@Override
	public void runTest(ClientGameTestContext context) {
		assertModMenuOffersTheScreen();
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:netherite_sword");
			shots(context, "sword");
			settingsScreen(context);
			world.getServer().runCommand("item replace entity @p weapon.offhand with minecraft:netherite_sword");
			shots(context, "both");
			world.getServer().runCommand("item replace entity @p weapon.offhand with minecraft:air");
			world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:air");
			shots(context, "empty");
		}
	}

	private static void assertModMenuOffersTheScreen() {
		boolean offered = FabricLoader.getInstance().getEntrypointContainers("modmenu", ModMenuApi.class).stream()
				.anyMatch(entry -> entry.getProvider().getMetadata().getId().equals("macpolish")
						&& entry.getEntrypoint().getModConfigScreenFactory() != null);
		if (!offered) {
			throw new AssertionError("Mod Menu does not offer the settings screen");
		}
	}

	private static void settingsScreen(ClientGameTestContext context) {
		context.getInput().resizeWindow(MAC_WIDTH, HEIGHT);
		context.waitTicks(10);
		context.setScreen(() -> new MacPolishConfigScreen(null));
		context.waitTicks(10);
		shot(context, "toggle-on");

		clickHeldItemToggle(context);
		context.waitTicks(10);
		if (MacPolishConfig.get().heldItemFraming()) {
			throw new AssertionError("Clicking the toggle did not turn framing off");
		}
		shot(context, "toggle-off");

		clickHeldItemToggle(context);
		context.waitTicks(10);
		if (!MacPolishConfig.get().heldItemFraming()) {
			throw new AssertionError("Clicking the toggle did not turn framing back on");
		}
		context.setScreen(() -> null);
		context.waitTicks(5);
	}

	private static void clickHeldItemToggle(ClientGameTestContext context) {
		double[] position = context.computeOnClient(client -> {
			// Mouse positions are in window units, which differ from framebuffer pixels on Retina displays.
			double scale = (double) client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth();
			int guiX = client.getWindow().getGuiScaledWidth() / 2;
			int guiY = MacPolishConfigScreen.FIRST_ROW_Y + HELD_ITEM_ROW * MacPolishConfigScreen.ROW_HEIGHT + 10;
			return new double[] {guiX * scale, guiY * scale};
		});
		context.getInput().setCursorPos(position[0], position[1]);
		context.waitTick();
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
	}

	private static void shots(ClientGameTestContext context, String name) {
		for (int[] size : SIZES) {
			context.getInput().resizeWindow(size[0], size[1]);
			context.waitTicks(40);
			shot(context, name + "-" + size[0]);
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		System.out.println("[macpolish-shot] " + context.takeScreenshot(TestScreenshotOptions.of(name)));
	}
}
