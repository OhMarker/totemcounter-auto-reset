package com.ohmarker.totemautoreset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * The mod's settings screen, reached through Mod Menu's "Configure" button. It is a plain vanilla
 * options sub-screen (same widgets as Options > Accessibility): three ON/OFF toggles and one slider.
 * Changes apply immediately and are written to {@code config/totemautoreset.json} when the screen closes.
 */
public final class AutoResetConfigScreen extends OptionsSubScreen {
	public AutoResetConfigScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable("totemautoreset.config.title"));
	}

	@Override
	protected void addOptions() {
		AutoResetConfig config = AutoResetConfig.get();

		list.addBig(OptionInstance.createBoolean("totemautoreset.option.resetOnOwnDeath",
				OptionInstance.cachedConstantTooltip(Component.translatable("totemautoreset.option.resetOnOwnDeath.tooltip")),
				config.resetOnOwnDeath, value -> config.resetOnOwnDeath = value));

		list.addBig(OptionInstance.createBoolean("totemautoreset.option.resetOnOpponentDeath",
				OptionInstance.cachedConstantTooltip(Component.translatable("totemautoreset.option.resetOnOpponentDeath.tooltip")),
				config.resetOnOpponentDeath, value -> config.resetOnOpponentDeath = value));

		list.addBig(new OptionInstance<>("totemautoreset.option.combatTimeout",
				OptionInstance.cachedConstantTooltip(Component.translatable("totemautoreset.option.combatTimeout.tooltip")),
				(caption, value) -> Options.genericValueLabel(caption, Component.translatable("totemautoreset.option.seconds", value)),
				new OptionInstance.IntRange(5, 120),
				config.combatTimeoutSeconds, value -> config.combatTimeoutSeconds = value));

		list.addBig(OptionInstance.createBoolean("totemautoreset.option.showToast",
				OptionInstance.cachedConstantTooltip(Component.translatable("totemautoreset.option.showToast.tooltip")),
				config.showToast, value -> config.showToast = value));
	}

	@Override
	public void removed() {
		super.removed();
		AutoResetConfig.get().save();
	}
}
