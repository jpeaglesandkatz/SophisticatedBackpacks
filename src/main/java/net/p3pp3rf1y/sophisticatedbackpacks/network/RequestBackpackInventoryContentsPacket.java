package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;

import java.util.UUID;

public class RequestBackpackInventoryContentsPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacks.MOD_ID, "request_backpack_inventory_contents");
	private final UUID backpackUuid;

	public RequestBackpackInventoryContentsPacket(UUID backpackUuid) {
		this.backpackUuid = backpackUuid;
	}

	public RequestBackpackInventoryContentsPacket(FriendlyByteBuf buffer) {
		this(buffer.readUUID());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		CompoundTag backpackContents = BackpackStorage.get().getOrCreateBackpackContents(backpackUuid);

		CompoundTag inventoryContents = new CompoundTag();
		Tag inventoryNbt = backpackContents.get(InventoryHandler.INVENTORY_TAG);
		if (inventoryNbt != null) {
			inventoryContents.put(InventoryHandler.INVENTORY_TAG, inventoryNbt);
		}
		Tag upgradeNbt = backpackContents.get(UpgradeHandler.UPGRADE_INVENTORY_TAG);
		if (upgradeNbt != null) {
			inventoryContents.put(UpgradeHandler.UPGRADE_INVENTORY_TAG, upgradeNbt);
		}
		PacketHelper.sendToPlayer(new BackpackContentsPacket(backpackUuid, inventoryContents), player);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(backpackUuid);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
