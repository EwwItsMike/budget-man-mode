package com.example;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.QuantityFormatter;

class ItemPricesOverlay extends Overlay
{
    private static final int INVENTORY_ITEM_WIDGETID = ComponentID.INVENTORY_CONTAINER;
    private static final int BANK_INVENTORY_ITEM_WIDGETID = ComponentID.BANK_INVENTORY_ITEM_CONTAINER;
    private static final int BANK_ITEM_WIDGETID = ComponentID.BANK_ITEM_CONTAINER;
    private static final int POH_TREASURE_CHEST_INVENTORY_ITEM_WIDGETID = ComponentID.POH_TREASURE_CHEST_INV_CONTAINER;

    private final Client client;
    private final BudgetManConfig config;
    private final TooltipManager tooltipManager;
    private final StringBuilder itemStringBuilder = new StringBuilder();

    @Inject
    ItemManager itemManager;

    @Inject
    ItemPricesOverlay(Client client, BudgetManConfig config, TooltipManager tooltipManager)
    {
        setPosition(OverlayPosition.DYNAMIC);
        this.client = client;
        this.config = config;
        this.tooltipManager = tooltipManager;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.isMenuOpen())
        {
            return null;
        }

        final MenuEntry[] menuEntries = client.getMenuEntries();
        final int last = menuEntries.length - 1;

        if (last < 0)
        {
            return null;
        }

        final MenuEntry menuEntry = menuEntries[last];
        final MenuAction action = menuEntry.getType();
        final int widgetId = menuEntry.getParam1();
        final int groupId = WidgetUtil.componentToInterface(widgetId);
        final boolean isAlching = menuEntry.getOption().equals("Cast") && menuEntry.getTarget().contains("High Level Alchemy");

        // Tooltip action type handling
        switch (action)
        {
            case WIDGET_TARGET_ON_WIDGET:
                // Check target widget is the inventory
                if (menuEntry.getWidget().getId() != ComponentID.INVENTORY_CONTAINER)
                {
                    break;
                }
                // FALLTHROUGH
            case CC_OP:
            case ITEM_USE:
            case ITEM_FIRST_OPTION:
            case ITEM_SECOND_OPTION:
            case ITEM_THIRD_OPTION:
            case ITEM_FOURTH_OPTION:
            case ITEM_FIFTH_OPTION:
                addTooltip(menuEntry, isAlching, groupId);
                break;
            case WIDGET_TARGET:
                // Check that this is the inventory
                if (menuEntry.getWidget().getId() == ComponentID.INVENTORY_CONTAINER)
                {
                    addTooltip(menuEntry, isAlching, groupId);
                }
        }

        return null;
    }

    private void addTooltip(MenuEntry menuEntry, boolean isAlching, int groupId)
    {
        // Item tooltip values
        switch (groupId)
        {
            case InterfaceID.INVENTORY:
            case InterfaceID.POH_TREASURE_CHEST_INV:
            case InterfaceID.BANK:
            case InterfaceID.BANK_INVENTORY:
                // Make tooltip
                final String text = makeValueTooltip(menuEntry);
                if (text != null)
                {
                    tooltipManager.add(new Tooltip(ColorUtil.prependColorTag(text, new Color(238, 238, 238))));
                }
        }
    }

    private String makeValueTooltip(MenuEntry menuEntry)
    {
        // Disabling both disables all value tooltips

        final int widgetId = menuEntry.getParam1();
        ItemContainer container = null;

        // Inventory item
        if (widgetId == INVENTORY_ITEM_WIDGETID ||
            widgetId == BANK_INVENTORY_ITEM_WIDGETID ||
            widgetId == POH_TREASURE_CHEST_INVENTORY_ITEM_WIDGETID)
        {
            container = client.getItemContainer(InventoryID.INVENTORY);
        }

        if (container == null)
        {
            return null;
        }

        // Find the item in the container to get stack size
        final int index = menuEntry.getParam0();
        final Item item = container.getItem(index);
        if (item != null)
        {
            return getItemStackValueText(item);
        }

        return null;
    }

    private String getItemStackValueText(Item item)
    {
        int id = itemManager.canonicalize(item.getId());
        int qty = item.getQuantity();

        // Special case for coins and platinum tokens
        if (id == ItemID.COINS_995)
        {
            return QuantityFormatter.formatNumber(qty) + " gp";
        }
        else if (id == ItemID.PLATINUM_TOKEN)
        {
            return QuantityFormatter.formatNumber(qty * 1000L) + " gp";
        }

        ItemComposition itemDef = itemManager.getItemComposition(id);

        // Only check prices for things with store prices
        if (itemDef.getPrice() <= 0)
        {
            return null;
        }

        int gePrice = 0;
        gePrice = itemManager.getItemPrice(id);

        if (gePrice > 0)
        {
            return stackValueText(qty, gePrice);
        }

        //Set hovered item cost to the ge price of the item we are hovering over
//        BudgetManMode.hoveredItemCost = gePrice;
//        System.out.println(BudgetManMode.hoveredItemCost);

        return null;
    }

    private String stackValueText(int qty, int gePrice)
    {
        if (gePrice > 0)
        {
            itemStringBuilder.append("GE: ")
                    .append(QuantityFormatter.quantityToStackSize((long) gePrice * qty))
                    .append(" gp");

        }

        // Build string and reset builder
        final String text = itemStringBuilder.toString();
        itemStringBuilder.setLength(0);
        return text;
    }
}
