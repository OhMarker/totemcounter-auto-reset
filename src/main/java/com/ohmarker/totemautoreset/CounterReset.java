package com.ohmarker.totemautoreset;

import net.uku3lig.totemcounter.TotemCounter;

import java.util.UUID;

/**
 * The single place this mod touches TotemCounter. It calls the same public reset that TotemCounter's
 * own reset keybind and {@code /totemcounter reset} use ({@link TotemCounter#resetPopCounter()}),
 * or clears the same map silently. Detection, counting and rendering stay entirely TotemCounter's.
 */
public final class CounterReset {
	/** One death is confirmed by up to three signals within a few ticks; reset once per player per window. */
	private static final long DEBOUNCE_MS = 1500L;
	private static UUID lastResetFor;
	private static long lastResetMs;
	private static boolean apiUnavailable;
	/** Number of resets actually performed; read by the smoke test. */
	static int resets;

	private CounterReset() {
	}

	/**
	 * @param deadPlayer the player whose death ends the fight (used only to swallow duplicate signals)
	 * @param reason     human readable, goes to the log
	 */
	public static void reset(UUID deadPlayer, String reason) {
		long now = System.currentTimeMillis();
		if (deadPlayer.equals(lastResetFor) && now - lastResetMs < DEBOUNCE_MS) {
			TotemAutoReset.LOGGER.debug("Ignoring duplicate death signal: {}", reason);
			return;
		}
		lastResetFor = deadPlayer;
		lastResetMs = now;
		if (apiUnavailable) {
			return;
		}
		try {
			if (AutoResetConfig.get().showToast) {
				TotemCounter.resetPopCounter();
			} else {
				TotemCounter.getPops().clear();
			}
			resets++;
			TotemAutoReset.LOGGER.info("Reset TotemCounter: {}", reason);
		} catch (LinkageError e) {
			// TotemCounter's internals changed in a version this mod was not built against. Log once and
			// go quiet rather than crash the game mid-fight.
			apiUnavailable = true;
			TotemAutoReset.LOGGER.error("Installed TotemCounter version has no compatible reset API; auto reset disabled", e);
		}
	}
}
