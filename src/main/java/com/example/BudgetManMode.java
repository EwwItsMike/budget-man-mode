package com.example;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

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
	public void onGameTick(GameTick event) {
		setValues();
	}

	public long getRemainingAllowedValue(){
		return maxAllowedValue - wornItemsValue;
	}
}