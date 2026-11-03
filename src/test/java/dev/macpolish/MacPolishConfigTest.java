package dev.macpolish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MacPolishConfigTest {
	@TempDir
	Path dir;

	private Path file() {
		return this.dir.resolve("macpolish.properties");
	}

	@Test
	void missingFileGivesDefaultsAndIsCreated() {
		MacPolishConfig config = MacPolishConfig.load(file());
		assertFalse(config.flipShiftScroll());
		assertTrue(config.heldItemFraming());
		assertTrue(Files.exists(file()));
	}

	@Test
	void createdFileLoadsBackToTheSameValues() {
		MacPolishConfig.load(file());
		MacPolishConfig config = MacPolishConfig.load(file());
		assertFalse(config.flipShiftScroll());
		assertTrue(config.heldItemFraming());
	}

	@Test
	void readsValuesFromTheFile() throws IOException {
		Files.writeString(file(), "flipShiftScroll=true\nheldItemFraming=false\n");
		MacPolishConfig config = MacPolishConfig.load(file());
		assertTrue(config.flipShiftScroll());
		assertFalse(config.heldItemFraming());
	}

	@Test
	void existingFileIsNotOverwritten() throws IOException {
		String contents = "flipShiftScroll=true\n";
		Files.writeString(file(), contents);
		MacPolishConfig.load(file());
		assertEquals(contents, Files.readString(file()));
	}

	@Test
	void missingOrInvalidValuesFallBackToDefaults() throws IOException {
		Files.writeString(file(), "flipShiftScroll=maybe\n");
		MacPolishConfig config = MacPolishConfig.load(file());
		assertFalse(config.flipShiftScroll());
		assertTrue(config.heldItemFraming());
	}

	@Test
	void unreadableConfigFallsBackToDefaults() throws IOException {
		Files.createDirectory(file());
		MacPolishConfig config = MacPolishConfig.load(file());
		assertFalse(config.flipShiftScroll());
		assertTrue(config.heldItemFraming());
	}

	@Test
	void cursorRepairDefaultsToOn() {
		assertTrue(MacPolishConfig.load(file()).pointerLockRepair());
	}

	@Test
	void togglesTakeEffectImmediatelyAndPersist() {
		MacPolishConfig config = MacPolishConfig.load(file());
		config.setFlipShiftScroll(true);
		config.setHeldItemFraming(false);
		config.setPointerLockRepair(false);
		assertTrue(config.flipShiftScroll());
		assertFalse(config.heldItemFraming());
		assertFalse(config.pointerLockRepair());

		MacPolishConfig reloaded = MacPolishConfig.load(file());
		assertTrue(reloaded.flipShiftScroll());
		assertFalse(reloaded.heldItemFraming());
		assertFalse(reloaded.pointerLockRepair());
	}

	@Test
	void savedFileKeepsItsExplanations() throws IOException {
		MacPolishConfig.load(file()).setHeldItemFraming(false);
		assertTrue(Files.readString(file()).contains("# Frames held items"));
	}

	@Test
	void failedSaveStillAppliesTheToggle() throws IOException {
		Files.createDirectory(file());
		MacPolishConfig config = MacPolishConfig.load(file());
		config.setHeldItemFraming(false);
		assertFalse(config.heldItemFraming());
	}
}
