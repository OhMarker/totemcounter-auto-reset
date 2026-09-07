package com.ohmarker.totemautoreset.mixin;

import com.ohmarker.totemautoreset.FightTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Observes the player-combat-kill packet, the server's explicit "you died" message for the local
 * player (it is what opens the death screen). Read after vanilla has bounced the packet onto the
 * render thread so it is seen once; the packet itself is not changed or cancelled.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(method = "handlePlayerCombatKill",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
					shift = At.Shift.AFTER))
	private void totemautoreset$onCombatKill(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
		FightTracker.onCombatKill(packet.playerId());
	}
}
