package net.p3pp3rf1y.sophisticatedbackpacks.init;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.network.*;

public class ModPackets {
	private ModPackets() {
	}

	public static void registerPackets(final RegisterPayloadHandlerEvent event) {
		final IPayloadRegistrar registrar = event.registrar(SophisticatedBackpacks.MOD_ID).versioned("1.0");
		registrar.play(BackpackOpenPacket.ID, BackpackOpenPacket::new, play -> play.server(BackpackOpenPacket::handle));
		registrar.play(UpgradeTogglePacket.ID, UpgradeTogglePacket::new, play -> play.server(UpgradeTogglePacket::handle));
		registrar.play(RequestBackpackInventoryContentsPacket.ID, RequestBackpackInventoryContentsPacket::new, play -> play.server(RequestBackpackInventoryContentsPacket::handle));
		registrar.play(BackpackContentsPacket.ID, BackpackContentsPacket::new, play -> play.client(BackpackContentsPacket::handle));
		registrar.play(InventoryInteractionPacket.ID, InventoryInteractionPacket::new, play -> play.server(InventoryInteractionPacket::handle));
		registrar.play(BlockToolSwapPacket.ID, BlockToolSwapPacket::new, play -> play.server(BlockToolSwapPacket::handle));
		registrar.play(EntityToolSwapPacket.ID, EntityToolSwapPacket::new, play -> play.server(EntityToolSwapPacket::handle));
		registrar.play(BackpackClosePacket.ID, buffer -> new BackpackClosePacket(), play -> play.server(BackpackClosePacket::handle));
		registrar.play(SyncClientInfoPacket.ID, SyncClientInfoPacket::new, play -> play.client(SyncClientInfoPacket::handle));
		registrar.play(AnotherPlayerBackpackOpenPacket.ID, AnotherPlayerBackpackOpenPacket::new, play -> play.server(AnotherPlayerBackpackOpenPacket::handle));
		registrar.play(BlockPickPacket.ID, BlockPickPacket::new, play -> play.server(BlockPickPacket::handle));
		registrar.play(RequestPlayerSettingsPacket.ID, buf -> new RequestPlayerSettingsPacket(), play -> play.server(RequestPlayerSettingsPacket::handle));
	}
}
