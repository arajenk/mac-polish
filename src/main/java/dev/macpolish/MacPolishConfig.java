package dev.macpolish;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Settings from config/macpolish.properties. Changes take effect immediately and are saved straight away. */
public final class MacPolishConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("macpolish");
	private static final String POINTER_LOCK_REPAIR = "pointerLockRepair";
	private static final String FLIP_SHIFT_SCROLL = "flipShiftScroll";
	private static final String HELD_ITEM_FRAMING = "heldItemFraming";
	private static final boolean DEFAULT_POINTER_LOCK_REPAIR = true;
	private static final boolean DEFAULT_FLIP_SHIFT_SCROLL = false;
	private static final boolean DEFAULT_HELD_ITEM_FRAMING = true;
	private static final String FILE_TEMPLATE = """
			# Mac Polish settings. With Mod Menu installed you can also change these in game.

			# Keeps the pointer locked after Cmd-Tab, so the cursor stays hidden while you play.
			pointerLockRepair=%s

			# Reverses Shift+scroll. Turn this on if Shift+scroll goes the opposite way to normal scrolling,
			# which happens if an app like Scroll Reverser sets your mouse to Classic while macOS stays on natural.
			flipShiftScroll=%s

			# Frames held items the way they look on a 16:9 screen.
			heldItemFraming=%s
			""";

	private static MacPolishConfig instance;

	private final Path file;
	private volatile boolean pointerLockRepair;
	private volatile boolean flipShiftScroll;
	private volatile boolean heldItemFraming;

	private MacPolishConfig(Path file, boolean pointerLockRepair, boolean flipShiftScroll, boolean heldItemFraming) {
		this.file = file;
		this.pointerLockRepair = pointerLockRepair;
		this.flipShiftScroll = flipShiftScroll;
		this.heldItemFraming = heldItemFraming;
	}

	public static synchronized MacPolishConfig get() {
		if (instance == null) {
			instance = load(FabricLoader.getInstance().getConfigDir().resolve("macpolish.properties"));
		}
		return instance;
	}

	static MacPolishConfig load(Path file) {
		Properties properties = new Properties();
		boolean exists = Files.exists(file);
		if (exists) {
			try (Reader reader = Files.newBufferedReader(file)) {
				properties.load(reader);
			} catch (IOException e) {
				LOGGER.warn("Could not read {}, using default settings", file, e);
			}
		}
		MacPolishConfig config = new MacPolishConfig(
				file,
				bool(properties, POINTER_LOCK_REPAIR, DEFAULT_POINTER_LOCK_REPAIR),
				bool(properties, FLIP_SHIFT_SCROLL, DEFAULT_FLIP_SHIFT_SCROLL),
				bool(properties, HELD_ITEM_FRAMING, DEFAULT_HELD_ITEM_FRAMING));
		if (!exists) {
			config.save();
		}
		return config;
	}

	private static boolean bool(Properties properties, String key, boolean fallback) {
		String value = properties.getProperty(key, "").trim();
		if (value.equalsIgnoreCase("true")) {
			return true;
		}
		return !value.equalsIgnoreCase("false") && fallback;
	}

	private void save() {
		try {
			Files.createDirectories(this.file.getParent());
			Files.writeString(this.file, FILE_TEMPLATE.formatted(this.pointerLockRepair, this.flipShiftScroll, this.heldItemFraming));
		} catch (IOException e) {
			LOGGER.warn("Could not save {}", this.file, e);
		}
	}

	public boolean pointerLockRepair() {
		return this.pointerLockRepair;
	}

	public boolean flipShiftScroll() {
		return this.flipShiftScroll;
	}

	public boolean heldItemFraming() {
		return this.heldItemFraming;
	}

	public void setPointerLockRepair(boolean value) {
		this.pointerLockRepair = value;
		save();
	}

	public void setFlipShiftScroll(boolean value) {
		this.flipShiftScroll = value;
		save();
	}

	public void setHeldItemFraming(boolean value) {
		this.heldItemFraming = value;
		save();
	}
}
