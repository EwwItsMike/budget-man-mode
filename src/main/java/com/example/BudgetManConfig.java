package com.example;

import java.awt.Color;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Budget Man Mode")
public interface BudgetManConfig extends Config
{
	@ConfigItem(
			keyName = "barColor",
			name = "Bar Colour",
			description = "The colour of the overlay bar"
	)
	default Color barColor() { return Color.YELLOW; }
}
