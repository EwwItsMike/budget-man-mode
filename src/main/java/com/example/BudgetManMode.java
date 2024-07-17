package com.example;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.ArrayList;

@Slf4j
@PluginDescriptor(
		name = "Budget Man Mode",
		description = "A game mode that restricts the value of your worn items to your total xp"
)
public class BudgetManMode extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;
	private long wornItemsValue = 0;
	private long maxAllowedValue = 0;
	private boolean initialValueLoaded = false;
//	private MenuE

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BudgetManConfig config;

	@Inject
	private BudgetOverlay overlay;

	@Provides
	BudgetManConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BudgetManConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		System.out.println("Budget Man Mode started!");
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		System.out.println("Budget Man Mode stopped!");
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN
			&& !initialValueLoaded)
		{
			setValues();
			initialValueLoaded = true;
		} else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
			wornItemsValue = 0;
			initialValueLoaded = false;
		}
	}

	private void setValues() {
		maxAllowedValue = client.getOverallExperience();

		wornItemsValue = 0;
		Item[] equipped = client.getItemContainer(InventoryID.EQUIPMENT).getItems();

		for (Item i : equipped){
			wornItemsValue += ((long) itemManager.getItemPrice(i.getId()) * i.getQuantity());
		}

	}


	@Subscribe
	public void onMenuOpened(MenuOpened event){
		MenuEntry[] entries = client.getMenuEntries();
		ArrayList<MenuEntry> cleaned = new ArrayList<>();
		ItemContainer container = null;
		MenuEntry entry = event.getFirstEntry();

		final int widgetID = entry.getParam1();
		if (widgetID == ComponentID.INVENTORY_CONTAINER || widgetID == ComponentID.BANK_INVENTORY_ITEM_CONTAINER) {
			container = client.getItemContainer(InventoryID.INVENTORY);
		}
		if (container == null) return;

		for (MenuEntry e : entries) {
			final int index = e.getParam0();
			final Item item = container.getItem(index);
			if (item == null) return;

			System.out.println("Checking entry: " + e.getOption());

			if ((wornItemsValue + overlay.getHoveredPriceDifference(item)) > maxAllowedValue && (e.getOption().equalsIgnoreCase("wear") || e.getOption().equalsIgnoreCase("wield"))){
				continue;
			} else {
				cleaned.add(e);
				System.out.println("Adding entry to cleaned");

			}

			System.out.println("---");
		}
		client.setMenuEntries(cleaned.toArray(new MenuEntry[0]));
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		setValues();
	}

	public long getRemainingAllowedValue(){
		return maxAllowedValue - wornItemsValue;
	}

	public long getWornItemsValue(){
		return wornItemsValue;
	}
}