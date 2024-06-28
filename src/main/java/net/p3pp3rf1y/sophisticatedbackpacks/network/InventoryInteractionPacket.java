package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.util.InventoryInteractionHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

public class InventoryInteractionPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacks.MOD_ID, "inventory_interaction");
	private final BlockPos pos;
	private final Direction face;

	public InventoryInteractionPacket(BlockPos pos, Direction face) {
		this.pos = pos;
		this.face = face;
	}

	public InventoryInteractionPacket(FriendlyByteBuf buffer) {
		this(buffer.readBlockPos(), buffer.readEnum(Direction.class));
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
			InventoryInteractionHelper.tryInventoryInteraction(pos, player.level(), backpack, face, player);
			player.swing(InteractionHand.MAIN_HAND, true);
			return true;
		});
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeEnum(face);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
