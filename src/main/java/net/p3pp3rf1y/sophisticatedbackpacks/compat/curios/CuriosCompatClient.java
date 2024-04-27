package net.p3pp3rf1y.sophisticatedbackpacks.compat.curios;

import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

public class CuriosCompatClient {
	public static void registerRenderers() {
		CuriosRendererRegistry.register(ModItems.BACKPACK.get(), BackpackCurioRenderer::new);
		CuriosRendererRegistry.register(ModItems.COPPER_BACKPACK.get(), BackpackCurioRenderer::new);
		CuriosRendererRegistry.register(ModItems.IRON_BACKPACK.get(), BackpackCurioRenderer::new);
		CuriosRendererRegistry.register(ModItems.GOLD_BACKPACK.get(), BackpackCurioRenderer::new);
		CuriosRendererRegistry.register(ModItems.DIAMOND_BACKPACK.get(), BackpackCurioRenderer::new);
		CuriosRendererRegistry.register(ModItems.NETHERITE_BACKPACK.get(), BackpackCurioRenderer::new);
	}
}
