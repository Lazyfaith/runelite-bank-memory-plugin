package com.bankmemory;

import com.bankmemory.util.OnAnyChangeDocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.AsyncBufferedImage;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

class BankMemoryPluginPanel extends PluginPanel {

    private enum DisplayState {
        RESET, SHOWING_NO_DATA, SHOWING_ITEM_LIST
    }

    private static final int PAD = 8;

    private DisplayState state;

    private final JLabel syncTimeLabel;
    private final IconTextField filterField;
    private final ItemList itemsList;
    private final JScrollPane itemsScrollPane;
    private final PluginErrorPanel errorPanel;

    protected BankMemoryPluginPanel() {
        super(false);
        setLayout(new BorderLayout(0, PAD));
        setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

        filterField = new IconTextField();

        itemsList = new ItemList();
        itemsList.setCellRenderer(new ItemListRenderer());
        itemsScrollPane = new JScrollPane(itemsList);

        filterField.setIcon(IconTextField.Icon.SEARCH);
        filterField.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        filterField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        filterField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        filterField.getDocument().addDocumentListener(new OnAnyChangeDocumentListener() {
            @Override
            public void onChange(DocumentEvent e) {
                itemsScrollPane.getViewport().setViewPosition(new Point(0, 0));
                itemsList.getModel().applyFilter(filterField.getText());
            }
        });

        syncTimeLabel = new JLabel();

        errorPanel = new PluginErrorPanel();
        errorPanel.setContent("Bank Memory",
                "Log in to a character and open a bank window to populate bank data for that character.");
        displayNoDataMessage();
    }

    void displayNoDataMessage() {
        checkState(SwingUtilities.isEventDispatchThread());
        reset();
        add(errorPanel, BorderLayout.NORTH);
        state = DisplayState.SHOWING_NO_DATA;
        repaint();
    }

    /**
     * Resets filter, resets list data, resets scroll position and removes UI components.
     * Indirectly: releases some held objects (by resetting the list).
     */
    void reset() {
        checkState(SwingUtilities.isEventDispatchThread());
        itemsList.getModel().clearList();
        itemsList.getModel().clearFilter();
        resetScrolling();
        removeAll();
        state = DisplayState.RESET;
    }

    private void resetScrolling() {
        itemsScrollPane.getViewport().setViewPosition(new Point(0, 0));
    }

    void updateTimeDisplay(String timeString) {
        checkState(SwingUtilities.isEventDispatchThread());
        syncTimeLabel.setText("Data from: " + timeString);
    }

    void displayItemListings(List<String> names, List<AsyncBufferedImage> icons) {
        checkState(SwingUtilities.isEventDispatchThread());
        checkArgument(names.size() == icons.size());

        ensureDisplayIsInItemListState();
        Point scrollPosition = itemsScrollPane.getViewport().getViewPosition();

        List<ItemListEntry> itemData = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            AsyncBufferedImage img = icons.get(i);
            int unfilteredRow = i;
            img.onLoaded(() -> repaintItemEntryIfRowVisible(unfilteredRow));
            itemData.add(new ItemListEntry(names.get(i), img));
        }
        FilterableItemListModel listModel = itemsList.getModel();
        listModel.setListContents(itemData);

        itemsScrollPane.getViewport().setViewPosition(scrollPosition);
        repaint();
    }

    private void ensureDisplayIsInItemListState() {
        if (state == DisplayState.SHOWING_ITEM_LIST) {
            return;
        }
        removeAll();
        add(filterField, BorderLayout.NORTH);
        add(itemsScrollPane, BorderLayout.CENTER);
        add(syncTimeLabel, BorderLayout.SOUTH);
        state = DisplayState.SHOWING_ITEM_LIST;
    }

    private void repaintItemEntryIfRowVisible(int unfilteredIndex) {
        int adjustedIndex = itemsList.getModel().getAdjustedIndex(unfilteredIndex);
        if (adjustedIndex < 0) {
            // Filtered out!
            return;
        }
        if (itemsList.getFirstVisibleIndex() <= unfilteredIndex && unfilteredIndex <= itemsList.getLastVisibleIndex()) {
            itemsList.repaint(itemsList.getCellBounds(adjustedIndex, adjustedIndex));
        }
    }

    private static class ItemList extends JList<ItemListEntry> {
        ItemList() {
            super(new FilterableItemListModel());
        }

        @Override
        public void setModel(ListModel<ItemListEntry> model) {
            checkNotNull(model);
            checkArgument(model instanceof FilterableItemListModel, "Incorrect class: " + model.getClass());
            super.setModel(model);
        }

        @Override
        public FilterableItemListModel getModel() {
            return (FilterableItemListModel) super.getModel();
        }
    }
}