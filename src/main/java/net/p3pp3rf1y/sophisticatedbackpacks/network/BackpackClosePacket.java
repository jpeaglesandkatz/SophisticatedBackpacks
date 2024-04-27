package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;

@SuppressWarnings("java:S1118")
public class BackpackClosePacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedBackpacks.MOD_ID, "backpack_close");

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (player.containerMenu instanceof BackpackContainer) {
			player.closeContainer();
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		//noop
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
