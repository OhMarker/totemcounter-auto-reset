package com.ohmarker.totemautoreset;

import com.mojang.authlib.GameProfile;
import com.terraformersmc.modmenu.ModMenu;
import com.terraformersmc.modmenu.gui.ModsScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.level.GameType;
import net.uku3lig.totemcounter.TotemCounter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Dev-only end-to-end check. Not in the release jar; loaded reflectively when the game runs with
 * {@code -Dtotemautoreset.smoke=true} ({@code gradlew runClient -Psmoke -PquickPlay="New World"}).
 * In a singleplayer world it:
 * <ol>
 *   <li>seeds a fake pop into TotemCounter's map and kills the local player through the integrated
 *       server (the real death path), then checks the map was cleared exactly once;</li>
 *   <li>adds a client-side fake player, feeds it through the same damage-source call the damage
 *       packet makes so it becomes the opponent, delivers a DEATH entity event to it through the
 *       real mixin, and checks the map was cleared again;</li>
 *   <li>opens the settings screen and Mod Menu's mod list (filtered to this mod), saves a screenshot
 *       of each to {@code run/screenshots/}, and asks Mod Menu for this mod's config screen.</li>
 * </ol>
 * Result goes to the log as {@code [smoke] RESULT: PASS/FAIL}.
 */
public final class SmokeTest {
	private static int ticks;
	private static boolean done;
	private static boolean ownDeathOk;
	private static boolean opponentOk;
	private static boolean modMenuOk;
	private static int resetsBefore;
	private static RemotePlayer fakeOpponent;

	private SmokeTest() {
	}

	/** Called reflectively from {@link TotemAutoReset}. */
	public static void install() {
		ClientTickEvents.END_CLIENT_TICK.register(SmokeTest::tick);
		TotemAutoReset.LOGGER.info("[smoke] self-test armed");
	}

	private static void tick(Minecraft mc) {
		if (done || mc.player == null || mc.level == null) {
			return;
		}
		ticks++;
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null) {
			return;
		}
		switch (ticks) {
			case 40 -> {
				TotemAutoReset.LOGGER.info("[smoke] in world; totemcounter loaded = {}, modmenu loaded = {}",
						FabricLoader.getInstance().isModLoaded("totemcounter"), FabricLoader.getInstance().isModLoaded("modmenu"));
				if (mc.player.isDeadOrDying()) {
					// A previous run may have saved the world with a dead player; start from a live one.
					TotemAutoReset.LOGGER.info("[smoke] player joined dead, respawning first");
					mc.player.respawn();
				}
			}
			case 50 -> server.execute(() -> forPlayer(server, p -> p.setGameMode(GameType.SURVIVAL)));
			case 60 -> server.execute(() -> forPlayer(server, p -> {
				TotemAutoReset.LOGGER.info("[smoke] hurting player for 1 heart");
				p.hurtServer(p.level(), p.damageSources().generic(), 2.0f);
			}));
			case 80 -> {
				resetsBefore = CounterReset.resets;
				TotemCounter.getPops().put(mc.player.getUUID(), 3);
				TotemAutoReset.LOGGER.info("[smoke] seeded TotemCounter map: {} (resets so far: {})", TotemCounter.getPops(), resetsBefore);
			}
			case 100 -> server.execute(() -> forPlayer(server, p -> {
				TotemAutoReset.LOGGER.info("[smoke] killing player");
				p.kill(p.level());
			}));
			case 160 -> {
				ownDeathOk = CounterReset.resets == resetsBefore + 1 && TotemCounter.getPops().isEmpty();
				TotemAutoReset.LOGGER.info("[smoke] own death: resets = {} (expected {}), map = {} -> {}",
						CounterReset.resets, resetsBefore + 1, TotemCounter.getPops(), ownDeathOk ? "PASS" : "FAIL");
			}
			case 200 -> {
				fakeOpponent = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "SmokeOpponent"));
				fakeOpponent.setPos(mc.player.getX() + 2.0, mc.player.getY(), mc.player.getZ());
				mc.level.addEntity(fakeOpponent);
				// Exactly what the mixin passes on when a damage-event packet says this player hit us.
				FightTracker.onDamage(mc.player, mc.level.damageSources().playerAttack(fakeOpponent));
				TotemCounter.getPops().put(fakeOpponent.getUUID(), 2);
				TotemCounter.getPops().put(mc.player.getUUID(), 1);
				TotemAutoReset.LOGGER.info("[smoke] fake opponent {} engaged; seeded map: {}",
						fakeOpponent.getScoreboardName(), TotemCounter.getPops());
			}
			case 220 -> {
				TotemAutoReset.LOGGER.info("[smoke] delivering DEATH entity event to fake opponent");
				fakeOpponent.handleEntityEvent(EntityEvent.DEATH);
			}
			case 240 -> {
				opponentOk = CounterReset.resets == resetsBefore + 2 && TotemCounter.getPops().isEmpty();
				TotemAutoReset.LOGGER.info("[smoke] opponent death: resets = {} (expected {}), map = {} -> {}",
						CounterReset.resets, resetsBefore + 2, TotemCounter.getPops(), opponentOk ? "PASS" : "FAIL");
				mc.level.removeEntity(fakeOpponent.getId(), Entity.RemovalReason.DISCARDED);
			}
			case 300 -> mc.setScreen(new AutoResetConfigScreen(null));
			case 330 -> screenshot(mc, "smoke-config.png");
			case 360 -> {
				if (FabricLoader.getInstance().isModLoaded("modmenu")) {
					mc.setScreen(new ModsScreen(null));
					filterModsScreen(mc.screen, "Auto Reset");
				}
			}
			case 400 -> {
				if (FabricLoader.getInstance().isModLoaded("modmenu")) {
					screenshot(mc, "smoke-modmenu.png");
					Screen configured = ModMenu.getConfigScreen(TotemAutoReset.MOD_ID, null);
					modMenuOk = ModMenu.hasConfigScreen(TotemAutoReset.MOD_ID) && configured instanceof AutoResetConfigScreen;
					TotemAutoReset.LOGGER.info("[smoke] Mod Menu config screen for {}: {} -> {}", TotemAutoReset.MOD_ID,
							configured == null ? "none" : configured.getClass().getSimpleName(), modMenuOk ? "PASS" : "FAIL");
				} else {
					modMenuOk = true;
					TotemAutoReset.LOGGER.info("[smoke] Mod Menu not on the classpath; skipping");
				}
			}
			case 420 -> {
				mc.setScreen(null);
				TotemAutoReset.LOGGER.info("[smoke] RESULT: {}", ownDeathOk && opponentOk && modMenuOk ? "PASS" : "FAIL");
				if (mc.player.isDeadOrDying()) {
					mc.player.respawn(); // leave the world saved with a live player for the next run
				}
			}
			case 440 -> {
				done = true;
				TotemAutoReset.LOGGER.info("[smoke] stopping client");
				mc.stop();
			}
			default -> {
			}
		}
	}

	private static void screenshot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1,
				msg -> TotemAutoReset.LOGGER.info("[smoke] screenshot {}: {}", name, msg.getString()));
	}

	/** Types into Mod Menu's search box and selects the first remaining entry (private fields, dev-only). */
	private static void filterModsScreen(Screen screen, String query) {
		try {
			Field searchField = screen.getClass().getDeclaredField("searchBox");
			searchField.setAccessible(true);
			((EditBox) searchField.get(screen)).setValue(query);
			Field listField = screen.getClass().getDeclaredField("modList");
			listField.setAccessible(true);
			Object modList = listField.get(screen);
			List<?> children = (List<?>) modList.getClass().getMethod("children").invoke(modList);
			if (!children.isEmpty()) {
				for (Method m : modList.getClass().getMethods()) {
					if (m.getName().equals("select") && m.getParameterCount() == 1) {
						m.invoke(modList, children.get(0));
						break;
					}
				}
			}
			TotemAutoReset.LOGGER.info("[smoke] Mod Menu filtered to '{}': {} entries", query, children.size());
		} catch (ReflectiveOperationException | RuntimeException e) {
			TotemAutoReset.LOGGER.warn("[smoke] could not drive Mod Menu's list (screenshot will show the unfiltered list)", e);
		}
	}

	private static void forPlayer(IntegratedServer server, Consumer<ServerPlayer> action) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			action.accept(p);
		}
	}
}
