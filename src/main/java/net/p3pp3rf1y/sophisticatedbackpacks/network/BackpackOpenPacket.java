package net.p3pp3rf1y.sophisticatedbackpacks.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.IContextAwareContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import javax.annotation.Nullable;

public class BackpackOpenPacket implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacks.MOD_ID, "backpack_open");
	private static final int CHEST_SLOT = 38;
	private static final int OFFHAND_SLOT = 40;
	private final int slotIndex;
	private final String identifier;
	private final String handlerName;

	public BackpackOpenPacket() {
		this(-1);
	}

	public BackpackOpenPacket(int backpackSlot) {
		this(backpackSlot, "");
	}

	public BackpackOpenPacket(int backpackSlot, String identifier, String handlerName) {
		slotIndex = backpackSlot;
		this.identifier = identifier;
		this.handlerName = handlerName;
	}

	public BackpackOpenPacket(int backpackSlot, String identifier) {
		this(backpackSlot, identifier, "");
	}

	public BackpackOpenPacket(FriendlyByteBuf buffer) {
		this(buffer.readInt(), buffer.readUtf(), buffer.readUtf());
	}

	public void handle(IPayloadContext context) {
		context.workHandler().execute(() -> context.player().ifPresent(this::handlePacket));
	}

	private void handlePacket(@Nullable Player player) {
		if (player == null) {
			return;
		}

		if (!handlerName.isEmpty()) {
			int adjustedSlotIndex = slotIndex;
			if (adjustedSlotIndex == CHEST_SLOT) {
				adjustedSlotIndex -= 36;
			} else if (adjustedSlotIndex == OFFHAND_SLOT) {
				adjustedSlotIndex = 0;
			}
			BackpackContext.Item backpackContext = new BackpackContext.Item(handlerName, identifier, adjustedSlotIndex,
					player.containerMenu instanceof InventoryMenu || (player.containerMenu instanceof BackpackContainer backpackContainer && backpackContainer.getBackpackContext().wasOpenFromInventory()));
			openBackpack(player, backpackContext);
		} else if (player.containerMenu instanceof BackpackContainer backpackContainer) {
			BackpackContext backpackContext = backpackContainer.getBackpackContext();
			if (slotIndex == -1) {
				openBackpack(player, backpackContext.getParentBackpackContext());
			} else if (backpackContainer.isStorageInventorySlot(slotIndex)) {
				openBackpack(player, backpackContext.getSubBackpackContext(slotIndex));
			}
		} else if (player.containerMenu instanceof IContextAwareContainer contextAwareContainer) {
			BackpackContext backpackContext = contextAwareContainer.getBackpackContext();
			openBackpack(player, backpackContext);
		} else {
			findAndOpenFirstBackpack(player);
		}
	}

	private void findAndOpenFirstBackpack(Player player) {
		PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
			BackpackContext.Item backpackContext = new BackpackContext.Item(inventoryName, identifier, slot);
			player.openMenu(new SimpleMenuProvider((w, p, pl) -> new BackpackContainer(w, pl, backpackContext), backpack.getHoverName()), backpackContext::toBuffer);
			return true;
		});
	}

	private void openBackpack(Player player, BackpackContext backpackContext) {
		player.openMenu(new SimpleMenuProvider((w, p, pl) -> new BackpackContainer(w, pl, backpackContext), backpackContext.getDisplayName(player)), backpackContext::toBuffer);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(slotIndex);
		buffer.writeUtf(identifier);
		buffer.writeUtf(handlerName);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}
}
