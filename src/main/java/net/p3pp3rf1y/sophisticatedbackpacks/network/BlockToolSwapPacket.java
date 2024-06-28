package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IBlockToolSwapUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.concurrent.atomic.AtomicBoolean;

public class BlockToolSwapPacket implements CustomPacketPayload {

	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacks.MOD_ID, "block_tool_swap");
	private final BlockPos pos;

	public BlockToolSwapPacket(BlockPos pos) {
		this.pos = pos;
	}

	public BlockToolSwapPacket(FriendlyByteBuf buffer) {
		this(BlockPos.of(buffer.readLong()));
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		AtomicBoolean result = new AtomicBoolean(false);
		AtomicBoolean anyUpgradeCanInteract = new AtomicBoolean(false);
		PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
					BackpackWrapper.fromData(backpack).getUpgradeHandler().getWrappersThatImplement(IBlockToolSwapUpgrade.class)
							.forEach(upgrade -> {
								if (!upgrade.canProcessBlockInteract() || result.get()) {
									return;
								}
								anyUpgradeCanInteract.set(true);

								result.set(upgrade.onBlockInteract(player.level(), pos, player.level().getBlockState(pos), player));
							});
					return result.get();
				}
		);

		if (!anyUpgradeCanInteract.get()) {
			player.displayClientMessage(Component.translatable("gui.sophisticatedbackpacks.status.no_tool_swap_upgrade_present"), true);
			return;
		}
		if (!result.get()) {
			player.displayClientMessage(Component.translatable("gui.sophisticatedbackpacks.status.no_tool_found_for_block"), true);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeLong(pos.asLong());
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
