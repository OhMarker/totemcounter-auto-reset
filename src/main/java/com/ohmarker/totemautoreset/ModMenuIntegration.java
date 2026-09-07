package com.ohmarker.totemautoreset;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registered under the {@code modmenu} entrypoint in fabric.mod.json, so it is only ever loaded when
 * Mod Menu is installed. Gives Mod Menu the "Configure" button for this mod.
 */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return AutoResetConfigScreen::new;
	}
}
