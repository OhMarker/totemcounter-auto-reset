package com.ohmarker.totemautoreset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Tracks the one player the local player is currently fighting and turns confirmed deaths into
 * counter resets.
 *
 * <p><b>Opponent</b> = the last player who damaged us, or the last player we damaged or swung at.
 * Damage is read from the server's damage-event packets, so crystal and anchor explosions count:
 * their damage source names the player who set them off. The opponent is forgotten after their
 * death, our death, a respawn, a world/server change, or {@code combatTimeoutSeconds} with no damage.
 *
 * <p><b>Death</b> is confirmed by the server's DEATH entity event (sent to everyone tracking the
 * entity, and also what practice servers send for their "fake" round-ending deaths), by the
 * player-combat-kill packet for the local player, or by synced health reaching 0. Every signal goes
 * through {@link CounterReset}, which swallows duplicates for the same player, so a death that is
 * reported three ways still resets once. Nothing here ever resets without one of those signals.
 *
 * <p>All entry points run on the render thread (packet handlers run after
 * {@code ensureRunningOnSameThread}; Fabric tick/attack events are main-thread), so no locking.
 */
public final class FightTracker {
	private static UUID opponentId;
	private static String opponentName;
	private static long lastCombatMs;
	private static float lastOpponentHealth = -1f;

	private static LocalPlayer lastPlayer;
	private static ClientLevel lastLevel;
	private static boolean localWasAlive;

	private FightTracker() {
	}

	// ---- signals -------------------------------------------------------------------------------

	/** {@code LivingEntity#handleDamageEvent}: the server confirmed {@code victim} took damage from {@code source}. */
	public static void onDamage(LivingEntity victim, DamageSource source) {
		LocalPlayer self = Minecraft.getInstance().player;
		if (self == null || !(victim instanceof Player victimPlayer)) {
			return;
		}
		Entity attacker = source.getEntity();
		if (isSelf(victimPlayer, self)) {
			if (attacker instanceof Player attackerPlayer && !isSelf(attackerPlayer, self)) {
				engage(attackerPlayer, "they hit you");
			}
		} else if (attacker instanceof Player attackerPlayer && isSelf(attackerPlayer, self)) {
			engage(victimPlayer, "you hit them");
		}
	}

	/** {@code AttackEntityCallback}: the local player swung at {@code target}. */
	public static void onAttack(Entity target) {
		LocalPlayer self = Minecraft.getInstance().player;
		if (self != null && target instanceof Player targetPlayer && !isSelf(targetPlayer, self)) {
			engage(targetPlayer, "you attacked them");
		}
	}

	/** {@code LivingEntity#handleEntityEvent(DEATH)}: the server announced that this entity died. */
	public static void onDeathEvent(LivingEntity entity) {
		LocalPlayer self = Minecraft.getInstance().player;
		if (self == null || !(entity instanceof Player player)) {
			return;
		}
		if (isSelf(player, self)) {
			localDied(self, "death event");
		} else if (isOpponent(player)) {
			opponentDied("death event");
		}
	}

	/** {@code ClientPacketListener#handlePlayerCombatKill}: the server told us which player entity was killed. */
	public static void onCombatKill(int entityId) {
		LocalPlayer self = Minecraft.getInstance().player;
		if (self != null && self.getId() == entityId) {
			localDied(self, "combat kill packet");
		}
	}

	/** {@code ClientTickEvents.END_CLIENT_TICK}: timeout, world/respawn changes and the health fallback. */
	public static void tick(Minecraft mc) {
		LocalPlayer self = mc.player;
		ClientLevel level = mc.level;
		if (self == null || level == null) {
			if (opponentId != null) {
				clear("left the world");
			}
			lastPlayer = null;
			lastLevel = null;
			localWasAlive = false;
			return;
		}

		// A new LocalPlayer instance means a respawn or a server switch; a new level means a
		// dimension/world change. Either way the old fight is over.
		if (self != lastPlayer || level != lastLevel) {
			if (opponentId != null) {
				clear(level != lastLevel ? "world changed" : "respawned");
			}
			lastPlayer = self;
			lastLevel = level;
			localWasAlive = !self.isDeadOrDying();
		}

		boolean alive = !self.isDeadOrDying();
		if (localWasAlive && !alive) {
			localDied(self, "health reached 0");
		}
		localWasAlive = alive;

		if (opponentId == null) {
			return;
		}
		if (System.currentTimeMillis() - lastCombatMs > AutoResetConfig.get().combatTimeoutSeconds * 1000L) {
			clear("combat timeout");
			return;
		}
		Player opponent = level.getPlayerByUUID(opponentId);
		if (opponent == null) {
			return; // out of render distance or not loaded; kept until the timeout
		}
		float health = opponent.getHealth();
		if (lastOpponentHealth > 0f && (health <= 0f || opponent.isDeadOrDying())) {
			opponentDied("health reached 0");
			return;
		}
		lastOpponentHealth = health;
	}

	// ---- internals -----------------------------------------------------------------------------

	private static void engage(Player opponent, String why) {
		UUID id = opponent.getUUID();
		if (!id.equals(opponentId)) {
			opponentId = id;
			opponentName = opponent.getScoreboardName();
			lastOpponentHealth = -1f;
			TotemAutoReset.LOGGER.debug("Now fighting {} ({})", opponentName, why);
		}
		lastCombatMs = System.currentTimeMillis();
	}

	private static void localDied(LocalPlayer self, String how) {
		if (AutoResetConfig.get().resetOnOwnDeath) {
			CounterReset.reset(self.getUUID(), "you died (" + how + ")");
		}
		if (opponentId != null) {
			clear("you died");
		}
	}

	private static void opponentDied(String how) {
		UUID id = opponentId;
		String name = opponentName;
		if (AutoResetConfig.get().resetOnOpponentDeath) {
			CounterReset.reset(id, name + " died (" + how + ")");
		}
		clear(name + " died");
	}

	private static void clear(String why) {
		if (opponentId != null) {
			TotemAutoReset.LOGGER.debug("Stopped tracking {} ({})", opponentName, why);
		}
		opponentId = null;
		opponentName = null;
		lastCombatMs = 0L;
		lastOpponentHealth = -1f;
	}

	private static boolean isSelf(Player player, LocalPlayer self) {
		return player == self || player.getUUID().equals(self.getUUID());
	}

	private static boolean isOpponent(Player player) {
		return opponentId != null && opponentId.equals(player.getUUID());
	}
}
