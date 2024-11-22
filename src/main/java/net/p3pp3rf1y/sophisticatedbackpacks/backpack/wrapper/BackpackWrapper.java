package net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedbackpacks.SophisticatedBackpacks;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IEnergyStorageUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.api.IFluidHandlerWrapperUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.*;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventorySorter;
import net.p3pp3rf1y.sophisticatedcore.util.LootHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RandHelper;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.IntConsumer;

public class BackpackWrapper implements IBackpackWrapper {
	public static final int DEFAULT_MAIN_COLOR = 0xFF_CC613A;
	public static final int DEFAULT_ACCENT_COLOR = 0xFF_622E1A;

	@Nullable
	private ItemStack backpack;
	private Runnable backpackSaveHandler = () -> {
	};
	private Runnable inventorySlotChangeHandler = () -> {
	};

	@Nullable
	private InventoryHandler handler = null;
	@Nullable
	private UpgradeHandler upgradeHandler = null;
	@Nullable
	private InventoryIOHandler inventoryIOHandler = null;
	@Nullable
	private InventoryModificationHandler inventoryModificationHandler = null;
	@Nullable
	private BackpackSettingsHandler settingsHandler = null;
	private boolean fluidHandlerInitialized = false;
	@Nullable
	private IStorageFluidHandler fluidHandler = null;
	private boolean energyStorageInitialized = false;
	@Nullable
	private IEnergyStorage energyStorage = null;

	@Nullable
	private BackpackRenderInfo renderInfo;

	private IntConsumer onSlotsChange = diff -> {
	};

	private Runnable onInventoryHandlerRefresh = () -> {
	};
	private Runnable upgradeCachesInvalidatedHandler = () -> {
	};
	public BackpackWrapper(ItemStack backpackStack) {
		setBackpackStack(backpackStack);
	}

	public static IBackpackWrapper fromStack(ItemStack stack) {
		return StorageWrapperRepository.getStorageWrapper(stack, IBackpackWrapper.class, BackpackWrapper::new);
		/* TODO try to add uuid based caching in the future
		UUID uuid = stack.get(ModCoreDataComponents.STORAGE_UUID);
		if (uuid == null) {
			return StorageWrapperRepository.getStorageWrapper(stack, IBackpackWrapper.class, BackpackWrapper::new);
		} else {
			return StorageWrapperRepository.getStorageWrapper(uuid, IBackpackWrapper.class, BackpackWrapper::new);
		}
*/
	}

	public static Optional<IBackpackWrapper> fromExistingData(ItemStack stack) {
		if (stack.getItem() instanceof BackpackItem) {
			return StorageWrapperRepository.getExistingStorageWrapper(stack, IBackpackWrapper.class);
		}

		return Optional.empty();
	}

	@Override
	public void setSaveHandler(Runnable saveHandler) {
		backpackSaveHandler = saveHandler;
		refreshInventoryForUpgradeProcessing();
	}

	@Override
	public void setInventorySlotChangeHandler(Runnable slotChangeHandler) {
		inventorySlotChangeHandler = slotChangeHandler;
	}

	@Override
	public ITrackedContentsItemHandler getInventoryForUpgradeProcessing() {
		if (inventoryModificationHandler == null) {
			inventoryModificationHandler = new InventoryModificationHandler(this);
		}
		return inventoryModificationHandler.getModifiedInventoryHandler();
	}

	@Override
	public InventoryHandler getInventoryHandler() {
		if (handler == null) {
			handler = new BackpackInventoryHandler(getNumberOfInventorySlots() - (getNumberOfSlotRows() * getColumnsTaken()),
					this, getBackpackContentsNbt(), () -> {
				markBackpackContentsDirty();
				inventorySlotChangeHandler.run();
			}, StackUpgradeItem.getInventorySlotLimit(this));
			handler.addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
		}
		return handler;
	}

	private int getNumberOfInventorySlots() {
		Integer inventorySlots = getBackpackStack().get(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS);

		if (inventorySlots != null) {
			return inventorySlots;
		}

		int itemInventorySlots = ((BackpackItem) getBackpackStack().getItem()).getNumberOfSlots();
		setNumberOfInventorySlots(itemInventorySlots);
		return itemInventorySlots;
	}

	@Override
	public int getNumberOfSlotRows() {
		int itemInventorySlots = getNumberOfInventorySlots();
		return (int) Math.ceil(itemInventorySlots <= 81 ? (double) itemInventorySlots / 9 : (double) itemInventorySlots / 12);
	}

	private void setNumberOfInventorySlots(int itemInventorySlots) {
		getBackpackStack().set(ModCoreDataComponents.NUMBER_OF_INVENTORY_SLOTS, itemInventorySlots);
	}

	private CompoundTag getBackpackContentsNbt() {
		return BackpackStorage.get().getOrCreateBackpackContents(getOrCreateContentsUuid());
	}

	private void markBackpackContentsDirty() {
		BackpackStorage.get().setDirty();
	}

	@Override
	public ITrackedContentsItemHandler getInventoryForInputOutput() {
		if (inventoryIOHandler == null) {
			inventoryIOHandler = new InventoryIOHandler(this);
			if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER && ServerLifecycleHooks.getCurrentServer() != null) {
				fillWithLoot(ServerLifecycleHooks.getCurrentServer().overworld(), BlockPos.ZERO);
			}
		}
		return inventoryIOHandler.getFilteredItemHandler();
	}

	@Override
	public Optional<IStorageFluidHandler> getFluidHandler() {
		if (!fluidHandlerInitialized) {
			IStorageFluidHandler wrappedHandler = getUpgradeHandler().getTypeWrappers(TankUpgradeItem.TYPE).isEmpty() ? null : new BackpackFluidHandler(this);
			List<IFluidHandlerWrapperUpgrade> fluidHandlerWrapperUpgrades = getUpgradeHandler().getWrappersThatImplement(IFluidHandlerWrapperUpgrade.class);

			for (IFluidHandlerWrapperUpgrade fluidHandlerWrapperUpgrade : fluidHandlerWrapperUpgrades) {
				wrappedHandler = fluidHandlerWrapperUpgrade.wrapHandler(wrappedHandler, getBackpackStack());
			}

			fluidHandler = wrappedHandler;
		}

		return Optional.ofNullable(fluidHandler);
	}

	@Override
	public Optional<IFluidHandlerItem> getItemFluidHandler() {
		return getFluidHandler().map(fh -> new FluidHandlerItemWrapper(getBackpackStack(), fh));
	}

	@Override
	public Optional<IEnergyStorage> getEnergyStorage() {
		if (!energyStorageInitialized) {
			IEnergyStorage wrappedStorage = getUpgradeHandler().getWrappersThatImplement(IEnergyStorage.class).stream().findFirst().orElse(null);

			for (IEnergyStorageUpgradeWrapper energyStorageWrapperUpgrade : getUpgradeHandler().getWrappersThatImplement(IEnergyStorageUpgradeWrapper.class)) {
				wrappedStorage = energyStorageWrapperUpgrade.wrapStorage(wrappedStorage);
			}

			energyStorage = wrappedStorage;
		}

		return energyStorage == null || energyStorage.getMaxEnergyStored() == 0 ? Optional.empty() : Optional.of(energyStorage);
	}

	@Override
	public void copyDataTo(IStorageWrapper otherStorageWrapper) {
		getContentsUuid().ifPresent(originalUuid -> {
			getInventoryHandler().copyStacksTo(otherStorageWrapper.getInventoryHandler());
			getUpgradeHandler().copyTo(otherStorageWrapper.getUpgradeHandler());
			getSettingsHandler().copyTo(otherStorageWrapper.getSettingsHandler());
		});
	}

	@Override
	public IBackpackWrapper setBackpackStack(ItemStack backpack) {
		this.backpack = backpack;
		if (renderInfo == null) {
			renderInfo = new BackpackRenderInfo(backpack, () -> backpackSaveHandler);
		}
		return this;
	}

	@Override
	public BackpackSettingsHandler getSettingsHandler() {
		if (settingsHandler == null) {
			if (getContentsUuid().isPresent()) {
				settingsHandler = new BackpackSettingsHandler(this, getBackpackContentsNbt(), this::markBackpackContentsDirty);
			} else {
				settingsHandler = Noop.INSTANCE.getSettingsHandler();
			}
		}
		return settingsHandler;
	}

	@Override
	public UpgradeHandler getUpgradeHandler() {
		if (upgradeHandler == null) {
			if (getContentsUuid().isPresent()) {
				upgradeHandler = new UpgradeHandler(getNumberOfUpgradeSlots(), this, getBackpackContentsNbt(), this::markBackpackContentsDirty, () -> {
					if (handler != null) {
						handler.clearListeners();
						handler.setBaseSlotLimit(StackUpgradeItem.getInventorySlotLimit(this));
					}
					getInventoryHandler().clearListeners();
					handler.addListener(getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class)::itemChanged);
					inventoryIOHandler = null;
					inventoryModificationHandler = null;
					fluidHandlerInitialized = false;
					fluidHandler = null;
					energyStorageInitialized = false;
					energyStorage = null;
					upgradeCachesInvalidatedHandler.run();
				}) {
					@Override
					public boolean isItemValid(int slot, ItemStack stack) {
						return super.isItemValid(slot, stack) && (stack.isEmpty() || SophisticatedBackpacks.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()) || stack.is(ModItems.BACKPACK_UPGRADE_TAG));
					}
				};
			} else {
				upgradeHandler = Noop.INSTANCE.getUpgradeHandler();
			}
		}
		return upgradeHandler;
	}

	@Override
	public void setUpgradeCachesInvalidatedHandler(Runnable handler) {
		upgradeCachesInvalidatedHandler = handler;
	}

	private int getNumberOfUpgradeSlots() {
		Integer upgradeSlots = getBackpackStack().get(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS);

		if (upgradeSlots != null) {
			return upgradeSlots;
		}

		int itemUpgradeSlots = ((BackpackItem) getBackpackStack().getItem()).getNumberOfUpgradeSlots();
		setNumberOfUpgradeSlots(itemUpgradeSlots);
		return itemUpgradeSlots;
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.ofNullable(getBackpackStack().get(ModCoreDataComponents.STORAGE_UUID));
	}

	private UUID getOrCreateContentsUuid() {
		Optional<UUID> contentsUuid = getContentsUuid();
		if (contentsUuid.isPresent()) {
			return contentsUuid.get();
		}
		clearDummyHandlers();
		UUID newUuid = UUID.randomUUID();
		setContentsUuid(newUuid);
		return newUuid;
	}

	private void clearDummyHandlers() {
		if (upgradeHandler == Noop.INSTANCE.getUpgradeHandler()) {
			upgradeHandler = null;
		}
		if (settingsHandler == Noop.INSTANCE.getSettingsHandler()) {
			settingsHandler = null;
		}
	}

	@Override
	public int getMainColor() {
		return getBackpackStack().getOrDefault(ModCoreDataComponents.MAIN_COLOR, DEFAULT_MAIN_COLOR);
	}

	@Override
	public int getAccentColor() {
		return getBackpackStack().getOrDefault(ModCoreDataComponents.ACCENT_COLOR, DEFAULT_ACCENT_COLOR);
	}

	@Override
	public Optional<Integer> getOpenTabId() {
		return Optional.ofNullable(getBackpackStack().get(ModCoreDataComponents.OPEN_TAB_ID));
	}

	@Override
	public void setOpenTabId(int openTabId) {
		getBackpackStack().set(ModCoreDataComponents.OPEN_TAB_ID, openTabId);
		backpackSaveHandler.run();
	}

	@Override
	public void removeOpenTabId() {
		getBackpackStack().remove(ModCoreDataComponents.OPEN_TAB_ID);
		backpackSaveHandler.run();
	}

	@Override
	public void setColors(int mainColor, int accentColor) {
		ItemStack backpackStack = getBackpackStack();
		BackpackItem.setColors(backpackStack, mainColor, accentColor);
		backpackSaveHandler.run();
	}

	@Override
	public void setSortBy(SortBy sortBy) {
		getBackpackStack().set(ModCoreDataComponents.SORT_BY, sortBy);
		backpackSaveHandler.run();
	}

	@Override
	public SortBy getSortBy() {
		return getBackpackStack().getOrDefault(ModCoreDataComponents.SORT_BY, SortBy.NAME);
	}

	@Override
	public void sort() {
		Set<Integer> slotIndexesExcludedFromSort = new HashSet<>();
		slotIndexesExcludedFromSort.addAll(getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class).getNoSortSlots());
		slotIndexesExcludedFromSort.addAll(getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotIndexes());
		InventorySorter.sortHandler(getInventoryHandler(), getComparator(), slotIndexesExcludedFromSort);
	}

	private Comparator<Map.Entry<ItemStackKey, Integer>> getComparator() {
		return switch (getSortBy()) {
			case COUNT -> InventorySorter.BY_COUNT;
			case TAGS -> InventorySorter.BY_TAGS;
			case NAME -> InventorySorter.BY_NAME;
			case MOD -> InventorySorter.BY_MOD;
		};
	}

	public ItemStack getBackpack() {
		return getBackpackStack();
	}

	@Override
	public ItemStack cloneBackpack() {
		ItemStack clonedBackpack = cloneBackpack(this);
		cloneSubbackpacks(BackpackWrapper.fromStack(clonedBackpack));
		return clonedBackpack;
	}

	private void cloneSubbackpacks(IStorageWrapper wrapperCloned) {
		InventoryHandler inventoryHandler = wrapperCloned.getInventoryHandler();
		InventoryHelper.iterate(inventoryHandler, (slot, stack) -> {
			if (!(stack.getItem() instanceof BackpackItem)) {
				return;
			}
			inventoryHandler.setStackInSlot(slot, cloneBackpack(BackpackWrapper.fromStack(stack)));
		});
	}

	private ItemStack cloneBackpack(IBackpackWrapper originalWrapper) {
		ItemStack backpackCopy = originalWrapper.getBackpack().copy();
		backpackCopy.remove(ModCoreDataComponents.STORAGE_UUID);
		IBackpackWrapper wrapperCopy = BackpackWrapper.fromStack(backpackCopy);
		originalWrapper.copyDataTo(wrapperCopy);
		return wrapperCopy.getBackpack();
	}

	@Override
	public void refreshInventoryForInputOutput() {
		inventoryIOHandler = null;
		upgradeCachesInvalidatedHandler.run();
	}

	@Override
	public void setPersistent(boolean persistent) {
		getInventoryHandler().setPersistent(persistent);
		getUpgradeHandler().setPersistent(persistent);
	}

	@Override
	public void setSlotNumbers(int numberOfInventorySlots, int numberOfUpgradeSlots) {
		setNumberOfInventorySlots(numberOfInventorySlots);
		setNumberOfUpgradeSlots(numberOfUpgradeSlots);
	}

	@Override
	public void setLoot(ResourceLocation lootTableName, float lootFactor) {
		getBackpackStack().set(ModDataComponents.LOOT_TABLE, lootTableName);
		getBackpackStack().set(ModDataComponents.LOOT_FACTOR, lootFactor);
		backpackSaveHandler.run();
	}

	@Override
	public void fillWithLoot(Player playerEntity) {
		Level level = playerEntity.level();
		if (level.isClientSide) {
			return;
		}
		BlockPos pos = playerEntity.blockPosition();
		fillWithLoot(level, pos);
	}

	private void fillWithLoot(Level level, BlockPos pos) {
		ResourceLocation lootTable = getBackpackStack().get(ModDataComponents.LOOT_TABLE);
		if (lootTable == null) {
			return;
		}
		fillWithLootFromTable(level, pos, lootTable);
	}

	@Override
	public void setContentsUuid(UUID storageUuid) {
		getBackpackStack().set(ModCoreDataComponents.STORAGE_UUID, storageUuid);
/* TODO add in the future
		StorageWrapperRepository.migrateToUuid(this, backpack, storageUuid);
*/
	}

	@Override
	public void removeContentsUuid() {
		getContentsUuid().ifPresent(BackpackStorage.get()::removeBackpackContents);
		removeContentsUUIDTag();
	}

	@Override
	public void removeContentsUUIDTag() {
		getBackpackStack().remove(ModCoreDataComponents.STORAGE_UUID);
	}

	private ItemStack getBackpackStack() {
		if (backpack == null) {
			throw new IllegalStateException("Backpack stack not set");
		}
		return backpack;
	}

	@Override
	public BackpackRenderInfo getRenderInfo() {
		return renderInfo;
	}

	@Override
	public void setColumnsTaken(int columnsTaken, boolean hasChanged) {
		int originalColumnsTaken = getColumnsTaken();
		getBackpackStack().set(ModDataComponents.COLUMNS_TAKEN, columnsTaken);
		if (hasChanged) {
			int diff = (columnsTaken - originalColumnsTaken) * getNumberOfSlotRows();
			onSlotsChange.accept(diff);
		}
		backpackSaveHandler.run();
	}

	@Override
	public void registerOnSlotsChangeListener(IntConsumer onSlotsChange) {
		this.onSlotsChange = onSlotsChange;
	}

	@Override
	public void unregisterOnSlotsChangeListener() {
		onSlotsChange = diff -> {
		};
	}

	@Override
	public int getColumnsTaken() {
		return getBackpackStack().getOrDefault(ModDataComponents.COLUMNS_TAKEN, 0);
	}

	private void fillWithLootFromTable(Level level, BlockPos pos, ResourceLocation lootTable) {
		MinecraftServer server = level.getServer();
		if (server == null || !(level instanceof ServerLevel serverLevel)) {
			return;
		}

		float lootFactor = getBackpackStack().getOrDefault(ModDataComponents.LOOT_FACTOR, 0f);

		getBackpackStack().remove(ModDataComponents.LOOT_TABLE);
		getBackpackStack().remove(ModDataComponents.LOOT_FACTOR);

		List<ItemStack> loot = LootHelper.getLoot(lootTable, server, serverLevel, pos);
		loot.removeIf(stack -> stack.getItem() instanceof BackpackItem);
		loot = RandHelper.getNRandomElements(loot, (int) (loot.size() * lootFactor));
		LootHelper.fillWithLoot(serverLevel.random, loot, getInventoryHandler());
	}

	private void setNumberOfUpgradeSlots(int numberOfUpgradeSlots) {
		getBackpackStack().set(ModCoreDataComponents.NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
	}

	@Override
	public void refreshInventoryForUpgradeProcessing() {
		inventoryModificationHandler = null;
		fluidHandler = null;
		fluidHandlerInitialized = false;
		energyStorage = null;
		energyStorageInitialized = false;
		refreshInventoryForInputOutput();
	}

	@Override
	public void onContentsNbtUpdated() {
		handler = null;
		upgradeHandler = null;
		refreshInventoryForUpgradeProcessing();
		onInventoryHandlerRefresh.run();
	}

	@Override
	public void registerOnInventoryHandlerRefreshListener(Runnable onInventoryHandlerRefresh) {
		this.onInventoryHandlerRefresh = onInventoryHandlerRefresh;
	}

	@Override
	public void unregisterOnInventoryHandlerRefreshListener() {
		onInventoryHandlerRefresh = () -> {
		};
	}

	@Override
	public ItemStack getWrappedStorageStack() {
		return getBackpack();
	}

	@Override
	public String getStorageType() {
		return "backpack";
	}

	@Override
	public Component getDisplayName() {
		return getBackpack().getHoverName();
	}

	private static class FluidHandlerItemWrapper implements IFluidHandlerItem {
		private final IFluidHandler delegate;
		private final ItemStack container;

		public FluidHandlerItemWrapper(ItemStack container, IFluidHandler delegate) {
			this.container = container;
			this.delegate = delegate;
		}


		@Override
		public ItemStack getContainer() {
			return container;
		}

		@Override
		public int getTanks() {
			return delegate.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return delegate.getFluidInTank(tank);
		}

		@Override
		public int getTankCapacity(int tank) {
			return delegate.getTankCapacity(tank);
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return delegate.isFluidValid(tank, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return delegate.fill(resource, action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return delegate.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return delegate.drain(maxDrain, action);
		}
	}
}
