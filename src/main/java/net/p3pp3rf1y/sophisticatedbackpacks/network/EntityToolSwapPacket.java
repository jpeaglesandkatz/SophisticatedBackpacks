package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IEntityToolSwapUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.concurrent.atomic.AtomicBoolean;

public class EntityToolSwapPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedBackpacks.MOD_ID, "entity_tool_swap");
	private final int entityId;

	public EntityToolSwapPacket(int entityId) {
		this.entityId = entityId;
	}

	public EntityToolSwapPacket(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		Level level = player.level();
		Entity entity = level.getEntity(entityId);

		if (entity == null) {
			return;
		}

		AtomicBoolean result = new AtomicBoolean(false);
		AtomicBoolean anyUpgradeCanInteract = new AtomicBoolean(false);
		PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
					BackpackWrapper.fromData(backpack)
							.getUpgradeHandler().getWrappersThatImplement(IEntityToolSwapUpgrade.class)
							.forEach(upgrade -> {
								if (!upgrade.canProcessEntityInteract() || result.get()) {
									return;
								}
								anyUpgradeCanInteract.set(true);

								result.set(upgrade.onEntityInteract(level, entity, player));
							});
					return result.get();
				}
		);

		if (!anyUpgradeCanInteract.get()) {
			player.displayClientMessage(Component.translatable("gui.sophisticatedbackpacks.status.no_tool_swap_upgrade_present"), true);
			return;
		}
		if (!result.get()) {
			player.displayClientMessage(Component.translatable("gui.sophisticatedbackpacks.status.no_tool_found_for_entity"), true);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(entityId);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
