package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.settings.BackpackMainSettingsCategory;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;

public class AnotherPlayerBackpackOpenPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacks.MOD_ID, "another_player_backpack_open");
	private final int anotherPlayerId;

	public AnotherPlayerBackpackOpenPacket(int anotherPlayerId) {
		this.anotherPlayerId = anotherPlayerId;
	}

	public AnotherPlayerBackpackOpenPacket(FriendlyByteBuf buffer) {
		this.anotherPlayerId = buffer.readInt();
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(Player player) {
		if (Boolean.FALSE.equals(Config.SERVER.allowOpeningOtherPlayerBackpacks.get())) {
			return;
		}

		if (player.level().getEntity(anotherPlayerId) instanceof Player anotherPlayer) {
			PlayerInventoryProvider.get().runOnBackpacks(anotherPlayer, (backpack, inventoryName, identifier, slot) -> {
				if (canAnotherPlayerOpenBackpack(anotherPlayer, backpack)) {

					BackpackContext.AnotherPlayer backpackContext = new BackpackContext.AnotherPlayer(inventoryName, identifier, slot, anotherPlayer);
					player.openMenu(new SimpleMenuProvider((w, p, pl) -> new BackpackContainer(w, pl, backpackContext), backpack.getHoverName()), backpackContext::toBuffer);
				} else {
					player.displayClientMessage(Component.translatable("gui.sophisticatedbackpacks.status.backpack_cannot_be_open_by_another_player"), true);
				}
				return true;
			}, true);
		}
	}

	private boolean canAnotherPlayerOpenBackpack(Player anotherPlayer, ItemStack backpack) {
		MainSettingsCategory<?> category = BackpackWrapper.fromData(backpack).getSettingsHandler().getGlobalSettingsCategory();
		return SettingsManager.getSettingValue(anotherPlayer, category.getPlayerSettingsTagName(), category, BackpackMainSettingsCategory.ANOTHER_PLAYER_CAN_OPEN);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(anotherPlayerId);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
