package com.ohmarker.totemautoreset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The mod's only settings, stored as plain JSON in {@code config/totemautoreset.json}.
 * The file is created with defaults on first launch. Edit it in Mod Menu (Configure) or by hand and restart.
 */
public final class AutoResetConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "totemautoreset.json";
	private static AutoResetConfig instance;

	/** Trigger TotemCounter's reset when the local player dies. */
	public boolean resetOnOwnDeath = true;
	/** Trigger TotemCounter's reset when the tracked opponent dies. */
	public boolean resetOnOpponentDeath = true;
	/** Seconds without any damage exchanged before the tracked opponent is forgotten. */
	public int combatTimeoutSeconds = 30;
	/**
	 * true: call TotemCounter's own reset, which also shows its "Successfully reset pop counter" toast
	 * (identical to pressing TotemCounter's reset key). false: clear the same counter silently.
	 */
	public boolean showToast = true;

	public static AutoResetConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static AutoResetConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		AutoResetConfig config = null;
		boolean readOk = false;
		if (Files.exists(path)) {
			try {
				config = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), AutoResetConfig.class);
				readOk = config != null;
			} catch (IOException | RuntimeException e) {
				TotemAutoReset.LOGGER.warn("Could not read {} - using defaults (file left untouched)", path, e);
			}
		}
		if (config == null) {
			config = new AutoResetConfig();
		}
		config.combatTimeoutSeconds = Math.max(1, config.combatTimeoutSeconds);

		// (Re)write only when the file is missing or parsed fine, so a typo never wipes the user's edits.
		if (!Files.exists(path) || readOk) {
			config.save();
		}
		return config;
	}

	/** Writes the current values to disk (called when the settings screen closes). */
	public void save() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		combatTimeoutSeconds = Math.max(1, combatTimeoutSeconds);
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (IOException e) {
			TotemAutoReset.LOGGER.warn("Could not write {}", path, e);
		}
	}
}
