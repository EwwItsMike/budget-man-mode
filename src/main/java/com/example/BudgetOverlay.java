package com.example;

import net.runelite.api.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;


import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

// Base code sourced from Item Prices plugin
public class BudgetOverlay extends Overlay {

    @Inject
    ItemManager itemManager;

    @Inject
    Client client;

    @Inject
    BudgetManConfig config;

    @Inject
    TooltipManager tooltipManager;

    @Inject
    BudgetManMode plugin;

    @Inject
    BudgetOverlay() {
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics2D) {
        if (client.isMenuOpen()) return null;

        final MenuEntry[] menuEntries = client.getMenuEntries();
        final int last = menuEntries.length - 1;

        if (last < 0) return null;

        final MenuEntry menuEntry = menuEntries[last];
        final MenuAction action = menuEntry.getType();
        final int groupId = WidgetUtil.componentToInterface(menuEntry.getParam1());

        switch (action){
            case WIDGET_TARGET_ON_WIDGET:
                if (menuEntry.getWidget().getId() != ComponentID.INVENTORY_CONTAINER) break;
            case CC_OP:
            case ITEM_USE:
            case ITEM_FIRST_OPTION:
            case ITEM_SECOND_OPTION:
            case ITEM_THIRD_OPTION:
            case ITEM_FOURTH_OPTION:
            case ITEM_FIFTH_OPTION:
                addTooltip(menuEntry, groupId);
                break;
            case WIDGET_TARGET:
                // Check that this is the inventory
                if (menuEntry.getWidget().getId() == ComponentID.INVENTORY_CONTAINER)
                {
                    addTooltip(menuEntry, groupId);
                }
        }
        return null;
    }

    private void addTooltip(MenuEntry entry, int groupID) {
        switch (groupID) {
            case InterfaceID.INVENTORY:
            case InterfaceID.BANK:
            case InterfaceID.BANK_INVENTORY:
                // Make tooltip
                final String text = makeTooltip(entry);
                if (text != null)
                {
                    String cleanValue = text.replaceAll(",", "").replace(" GP", "");
                    if (Long.parseLong(cleanValue) < plugin.getRemainingAllowedValue()){
                        tooltipManager.add(new Tooltip(ColorUtil.prependColorTag(text, new Color(0, 190, 0))));
                    } else {
                        tooltipManager.add(new Tooltip(ColorUtil.prependColorTag(text, new Color(190, 0 , 0))));
                        // TODO: remove equip/wield option on the item (or set first option to USE)
                        removeEquipOption(entry);
                    }
                }
        }
    }

    private String makeTooltip(MenuEntry entry) {
        final int widgetId = entry.getParam1();
        ItemContainer container = null;

        if (widgetId == ComponentID.INVENTORY_CONTAINER || widgetId == ComponentID.BANK_INVENTORY_ITEM_CONTAINER) {
            container = client.getItemContainer(InventoryID.INVENTORY);
        }

        if (container == null) return null;

        final int index = entry.getParam0();
        final Item item = container.getItem(index);
        if (item != null) {

            String formattedNumber = NumberFormat.getNumberInstance(Locale.US).format(getHoveredPriceDifference(item));
            return  formattedNumber + " GP";
        }

        return null;
    }

    public Integer getHoveredPriceDifference(Item item) {

        int currentlyEquippedPrice = 0;

        final ItemStats stats = itemManager.getItemStats(item.getId(), false);
        final ItemEquipmentStats current = stats.getEquipment();

        ItemContainer c = client.getItemContainer(InventoryID.EQUIPMENT);
        if (stats.isEquipable() && current != null && c != null){
            final int slot = current.getSlot();
            currentlyEquippedPrice = getItemPriceFromContainer(c, slot);
        }

        return (itemManager.getItemPrice(item.getId()) * item.getQuantity()) - currentlyEquippedPrice;
    }

    private int getItemPriceFromContainer(ItemContainer container, int slotID) {
        final Item item = container.getItem(slotID);
        return item != null ? itemManager.getItemPrice(item.getId()) * item.getQuantity() : 0;
    }

    private void removeEquipOption(MenuEntry entry) {
        MenuEntry[] entries = client.getMenuEntries();
        ArrayList<MenuEntry> cleaned = new ArrayList<>();

        for (MenuEntry e : entries){
            if (e.getIdentifier() != entry.getIdentifier()){
                cleaned.add(e);
            }
        }
        client.setMenuEntries(cleaned.toArray(new MenuEntry[0]));

    }
}
