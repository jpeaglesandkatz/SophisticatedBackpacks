package net.p3pp3rf1y.sophisticatedbackpacks.client.init;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;

import static net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.*;

public class ModItemColors {
	private ModItemColors() {
	}

	public static void registerItemColorHandlers(RegisterColorHandlersEvent.Item event) {
		event.register((backpack, layer) -> {
			if (layer > 1 || !(backpack.getItem() instanceof BackpackItem)) {
				return -1;
			}
			IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(backpack);
			if (layer == 0) {
				return backpackWrapper.getMainColor();
			} else if (layer == 1) {
				return backpackWrapper.getAccentColor();
			}
			return -1;
		}, BACKPACK.get(), COPPER_BACKPACK.get(), IRON_BACKPACK.get(), GOLD_BACKPACK.get(), DIAMOND_BACKPACK.get(), NETHERITE_BACKPACK.get());
	}
}
