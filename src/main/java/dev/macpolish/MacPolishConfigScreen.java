package dev.macpolish;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * One on/off button per fix. Toggles apply instantly, and inside a world the background is left clear so the
 * held item can be seen changing behind the screen.
 */
public final class MacPolishConfigScreen extends Screen {
	public static final int FIRST_ROW_Y = 46;
	public static final int ROW_HEIGHT = 24;
	private static final int BUTTON_WIDTH = 220;
	private static final int BUTTON_HEIGHT = 20;
	private static final int TEXT_WHITE = 0xFFFFFFFF;
	private static final int TEXT_GRAY = 0xFFA0A0A0;

	private final Screen parent;

	public MacPolishConfigScreen(Screen parent) {
		super(Component.translatable("macpolish.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		MacPolishConfig config = MacPolishConfig.get();
		addToggle(0, "pointer_lock_repair", config::pointerLockRepair, config::setPointerLockRepair);
		addToggle(1, "flip_shift_scroll", config::flipShiftScroll, config::setFlipShiftScroll);
		addToggle(2, "held_item_framing", config::heldItemFraming, config::setHeldItemFraming);
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds((this.width - BUTTON_WIDTH) / 2, FIRST_ROW_Y + 3 * ROW_HEIGHT + 6, BUTTON_WIDTH, BUTTON_HEIGHT)
				.build());
	}

	private void addToggle(int row, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
		Component label = Component.translatable("macpolish.config." + key);
		Tooltip tooltip = Tooltip.create(Component.translatable("macpolish.config." + key + ".tooltip"));
		addRenderableWidget(CycleButton.onOffBuilder(getter.getAsBoolean())
				.withTooltip(value -> tooltip)
				.create((this.width - BUTTON_WIDTH) / 2, FIRST_ROW_Y + row * ROW_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, label,
						(button, value) -> setter.accept(value)));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (this.minecraft.level == null) {
			super.extractBackground(graphics, mouseX, mouseY, partialTick);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(this.font, this.title, this.width / 2, 16, TEXT_WHITE);
		graphics.centeredText(this.font, Component.translatable("macpolish.config.hint"), this.width / 2, 28, TEXT_GRAY);
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
