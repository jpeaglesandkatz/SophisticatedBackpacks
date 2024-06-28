package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;

import java.util.Map;

public class UpgradeTogglePacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacks.MOD_ID, "upgrade_toggle");
	private final int upgradeSlot;

	public UpgradeTogglePacket(int upgradeSlot) {
		this.upgradeSlot = upgradeSlot;
	}

	public UpgradeTogglePacket(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
			Map<Integer, IUpgradeWrapper> slotWrappers = BackpackWrapper.fromData(backpack).getUpgradeHandler().getSlotWrappers();
			if (slotWrappers.containsKey(upgradeSlot)) {
				IUpgradeWrapper upgradeWrapper = slotWrappers.get(upgradeSlot);
				if (upgradeWrapper.canBeDisabled()) {
					upgradeWrapper.setEnabled(!upgradeWrapper.isEnabled());
					String translKey = upgradeWrapper.isEnabled() ? "gui.sophisticatedbackpacks.status.upgrade_switched_on" : "gui.sophisticatedbackpacks.status.upgrade_switched_off";
					player.displayClientMessage(Component.translatable(translKey, upgradeWrapper.getUpgradeStack().getHoverName()), true);
				}
			}
			return true;
		});
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(upgradeSlot);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}