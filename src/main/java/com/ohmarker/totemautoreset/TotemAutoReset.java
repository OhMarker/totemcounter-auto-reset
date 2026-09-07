package com.ohmarker.totemautoreset;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Companion mod for uku's TotemCounter. It adds exactly one behaviour: when the local player dies,
 * or the player they are fighting dies, TotemCounter's own reset is triggered so the next fight
 * starts at 0. Nothing about how TotemCounter detects, counts or draws pops is touched.
 */
public final class TotemAutoReset implements ClientModInitializer {
	public static final String MOD_ID = "totemautoreset";
	public static final Logger LOGGER = LoggerFactory.getLogger("TotemAutoReset");

	@Override
	public void onInitializeClient() {
		AutoResetConfig config = AutoResetConfig.get();

		// Combat timeout, world/respawn changes and the health-based death fallback.
		ClientTickEvents.END_CLIENT_TICK.register(FightTracker::tick);
		// Dev-only self-test (excluded from the release jar), see SmokeTest.java.
		if (Boolean.getBoolean("totemautoreset.smoke")) {
			try {
				Class.forName("com.ohmarker.totemautoreset.SmokeTest").getMethod("install").invoke(null);
			} catch (ReflectiveOperationException e) {
				LOGGER.warn("Smoke test requested but it is not part of this build", e);
			}
		}

		// "Most recently attacked player": a left-click on another player marks them as the opponent
		// even before any damage packet comes back. Fires on the integrated server too, hence the check.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide()) {
				FightTracker.onAttack(entity);
			}
			return InteractionResult.PASS;
		});

		LOGGER.info("TotemCounter Auto Reset ready (reset on own death: {}, on opponent death: {}, combat timeout: {}s, toast: {})",
				config.resetOnOwnDeath, config.resetOnOpponentDeath, config.combatTimeoutSeconds, config.showToast);
	}
}
