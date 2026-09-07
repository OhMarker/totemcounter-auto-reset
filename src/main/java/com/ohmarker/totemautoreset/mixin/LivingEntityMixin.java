package com.ohmarker.totemautoreset.mixin;

import com.ohmarker.totemautoreset.FightTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Observes two server-confirmed facts about living entities on the client, without changing them:
 * <ul>
 *   <li>{@code handleEntityEvent(DEATH)} - the server broadcast that this entity died. Player death
 *       on the server ({@code ServerPlayer#die}) always sends it, to the dying player as well as to
 *       everyone watching, and Bukkit's {@code EntityEffect.DEATH} (used by practice servers for
 *       fake deaths) is the same packet.</li>
 *   <li>{@code handleDamageEvent} - the server's damage-event packet, whose source names the player
 *       responsible, including for crystal and anchor explosions.</li>
 * </ul>
 * Both are reached from {@code ClientPacketListener} after the packet has been bounced onto the
 * render thread, so each is observed exactly once. Vanilla behaviour is untouched.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "handleEntityEvent", at = @At("HEAD"))
	private void totemautoreset$onEntityEvent(byte id, CallbackInfo ci) {
		if (id != EntityEvent.DEATH) {
			return;
		}
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide()) {
			FightTracker.onDeathEvent(self);
		}
	}

	@Inject(method = "handleDamageEvent", at = @At("HEAD"))
	private void totemautoreset$onDamageEvent(DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self.level().isClientSide()) {
			FightTracker.onDamage(self, source);
		}
	}
}
