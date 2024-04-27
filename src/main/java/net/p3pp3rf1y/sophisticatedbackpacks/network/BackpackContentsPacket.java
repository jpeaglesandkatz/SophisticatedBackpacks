package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedcore.client.render.ClientStorageContentsTooltipBase;

import javax.annotation.Nullable;
import java.util.UUID;

public class BackpackContentsPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(SophisticatedBackpacks.MOD_ID, "backpack_contents");
	private final UUID backpackUuid;
	@Nullable
	private final CompoundTag backpackContents;

	public BackpackContentsPacket(UUID backpackUuid, @Nullable CompoundTag backpackContents) {
		this.backpackUuid = backpackUuid;
		this.backpackContents = backpackContents;
	}

	public BackpackContentsPacket(FriendlyByteBuf buffer) {
		this(buffer.readUUID(), buffer.readNbt());
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (backpackContents == null) {
			return;
		}

		BackpackStorage.get().setBackpackContents(backpackUuid, backpackContents);
		ClientStorageContentsTooltipBase.refreshContents();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(backpackUuid);
		buffer.writeNbt(backpackContents);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
