package com.bankmemory;

import com.bankmemory.util.OnAnyChangeDocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import lombok.Value;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.AsyncBufferedImage;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

class BankMemoryPluginPanel extends PluginPanel {

    private enum DisplayState {
        RESET, SHOWING_NO_DATA, SHOWING_ITEM_LIST
    }

    private static final int PAD = 8;

    private DisplayState state;

    private final JLabel syncTimeLabel;
    private final IconTextField filterField;
    private final JPanel listingsPanel;
    private final JScrollPane itemsScrollPane;
    private final PluginErrorPanel errorPanel;

    private final List<ListingEntry> listEntriesWithLcItemName = new ArrayList<>();

    protected BankMemoryPluginPanel() {
        super(false);
        setLayout(new BorderLayout(0, PAD));
        setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

        filterField = new IconTextField();

        listingsPanel = new JPanel();
        listingsPanel.setLayout(new BoxLayout(listingsPanel, BoxLayout.Y_AXIS));
        itemsScrollPane = new JScrollPane(listingsPanel);

        filterField.setIcon(IconTextField.Icon.SEARCH);
        filterField.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        filterField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        filterField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        filterField.getDocument().addDocumentListener(new OnAnyChangeDocumentListener() {
            @Override
            public void onChange(DocumentEvent e) {
                itemsScrollPane.getViewport().setViewPosition(new Point(0, 0));
                updateFiltering();
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
        clearItemList();
        resetScrolling();
        removeAll();
        state = DisplayState.RESET;
    }

    private void resetScrolling() {
        itemsScrollPane.getViewport().setViewPosition(new Point(0, 0));
    }

    private void clearItemList() {
        listingsPanel.removeAll();
        listEntriesWithLcItemName.clear();
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
        clearItemList();

        for (int i = 0; i < names.size(); i++) {
            JLabel label = new JLabel(names.get(i));
            icons.get(i).addTo(label);
            listEntriesWithLcItemName.add(new ListingEntry(names.get(i).toLowerCase(), label));
            listingsPanel.add(label);
        }

        updateFiltering();
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

    private void updateFiltering() {
        assert SwingUtilities.isEventDispatchThread();

        String lowerCaseFilter = filterField.getText().toLowerCase();
        for (ListingEntry entry : listEntriesWithLcItemName) {
            boolean visible = lowerCaseFilter.isEmpty() || entry.getLcName().contains(lowerCaseFilter);
            entry.getListComponent().setVisible(visible);
        }
    }

    @Value
    private static class ListingEntry {
        private final String lcName;
        private final JComponent listComponent;

        ListingEntry(String lcName, JComponent listComponent) {
            assert lcName.toLowerCase().equals(lcName);
            this.lcName = lcName;
            this.listComponent = listComponent;
        }
    }
}
