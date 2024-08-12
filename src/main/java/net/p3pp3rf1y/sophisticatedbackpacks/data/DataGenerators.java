package net.p3pp3rf1y.sophisticatedbackpacks.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

public class DataGenerators {
	private DataGenerators() {}

	public static void gatherData(GatherDataEvent evt) {
		DataGenerator generator = evt.getGenerator();
		PackOutput packOutput = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> registries = evt.getLookupProvider();
		generator.addProvider(evt.includeServer(), new SBLootTableProvider(packOutput, registries));
		generator.addProvider(evt.includeServer(), new SBLootModifierProvider(packOutput, registries));
		generator.addProvider(evt.includeServer(), new SBPRecipeProvider(packOutput, registries));
	}
}
