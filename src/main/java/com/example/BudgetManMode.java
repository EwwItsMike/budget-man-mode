package com.example;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

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
	private Integer wornItemsValue = 0;
	private boolean initialValueLoaded = false;

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
			initValue();
			initialValueLoaded = true;
		} else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN) {
			wornItemsValue = 0;
			initialValueLoaded = false;
		}
	}

	private void initValue() {
		wornItemsValue = 0;
		Item[] equipped = client.getItemContainer(InventoryID.EQUIPMENT).getItems();

		for (Item i : equipped){
			wornItemsValue += itemManager.getItemPrice(i.getId()) * i.getQuantity();
		}

		System.out.println("Initial equipped value loaded. Value set at: " + wornItemsValue);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
//		int groupId = WidgetInfo.TO_GROUP(event.getActionParam1());
//		if (WidgetID.INVENTORY_GROUP_ID != groupId)
//		{
//			return;
//		}
//
//		MenuEntry[] menuEntries = client.getMenuEntries();
//		List<MenuEntry> cleaned = new ArrayList<>();
//
//		for (MenuEntry entry : menuEntries)
//		{
//			String option = entry.getOption().toLowerCase();
//			String target = Text.removeTags(entry.getTarget());
//
//			// remove ability to wear or wield IF this item puts your total value of worn items above your total XP.
//			if ((wornItemsValue + getHoveredPriceDifference() > client.getOverallExperience()) && ("wear".equalsIgnoreCase(option) || "wield".equalsIgnoreCase(option)))
//			{
//				continue;
//			}
//			else
//			{
//				cleaned.add(entry);
//			}
//		}
//		client.setMenuEntries(cleaned.toArray(new MenuEntry[0]));
	}
}