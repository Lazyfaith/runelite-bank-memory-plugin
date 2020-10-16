package com.bankmemory;

import com.bankmemory.data.BankItem;
import com.bankmemory.data.BankSave;
import com.bankmemory.data.BankSavesDataStore;
import com.bankmemory.data.StoredBanksUpdateListener;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

@Slf4j
public class SavedBanksPanelController {

    static {
        BufferedImage backIcon = ImageUtil.getResourceStreamFromClass(SavedBanksPanelController.class, "back_icon.png");
        BACK_ICON = new ImageIcon(backIcon);
        BACK_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(backIcon, -100));
    }

    private static final Icon BACK_ICON;
    private static final Icon BACK_ICON_HOVER;

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ItemManager itemManager;
    @Inject
    private BankSavesDataStore dataStore;

    private JPanel contentPanel;
    private BanksListPanel banksListPanel;
    private ImageIcon casketIcon;
    private ImageIcon notedCasketIcon;
    private BankViewPanel bankViewPanel;
    private JPanel backButtonAndBankName;
    private JLabel bankName;
    private final AtomicBoolean workingToOpenBank = new AtomicBoolean();

    public void startUp(JPanel contentPanel) {
        assert SwingUtilities.isEventDispatchThread();

        this.contentPanel = contentPanel;
        contentPanel.setLayout(new BorderLayout());
        banksListPanel = new BanksListPanel(new BanksListInteractionListenerImpl());
        casketIcon = new ImageIcon(itemManager.getImage(405));
        notedCasketIcon = new ImageIcon(itemManager.getImage(406));

        bankViewPanel = new BankViewPanel();
        backButtonAndBankName = new JPanel();
        backButtonAndBankName.setLayout(new BoxLayout(backButtonAndBankName, BoxLayout.LINE_AXIS));
        JButton backButton = new JButton(BACK_ICON);
        SwingUtil.removeButtonDecorations(backButton);
        backButton.setRolloverIcon(BACK_ICON_HOVER);
        backButton.addActionListener(e -> displayBanksListPanel());
        bankName = new JLabel();
        backButtonAndBankName.add(backButton);
        backButtonAndBankName.add(bankName);

        displayBanksListPanel();
        updateCurrentBanksList();

        dataStore.addListener(new BanksUpdateListener());
    }

    private void displayBanksListPanel() {
        bankViewPanel.reset();
        contentPanel.removeAll();
        contentPanel.add(banksListPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // Gets called on EDT and on game client thread
    private void updateCurrentBanksList() {
        List<BanksListEntry> saves = new ArrayList<>();
        TreeMap<String, String> displayNameMap = dataStore.getCurrentDisplayNameMap();

        for (BankSave save : dataStore.getCurrentBanksList()) {
            String displayName = displayNameMap.getOrDefault(save.getUserName(), save.getUserName());
            saves.add(new BanksListEntry(
                    save.getId(), casketIcon, displayName, "Current bank", save.getDateTimeString()));
        }
        for (BankSave save : dataStore.getNamedBanksList()) {
            String displayName = displayNameMap.getOrDefault(save.getUserName(), save.getUserName());
            saves.add(new BanksListEntry(
                    save.getId(), notedCasketIcon, save.getSaveName(), displayName, save.getDateTimeString()));
        }

        Runnable updateList = () -> banksListPanel.updateBanksList(saves);
        if (SwingUtilities.isEventDispatchThread()) {
            updateList.run();
        } else {
            SwingUtilities.invokeLater(updateList);
        }
    }

    private void openSavedBank(BanksListEntry selected) {
        assert client.isClientThread();

        Optional<BankSave> save = dataStore.getBankSaveWithId(selected.getSaveId());
        if (!save.isPresent()) {
            log.error("Selected missing bank save: {}", selected);
            workingToOpenBank.set(false);
            return;
        }
        BankSave foundSave = save.get();

        List<String> itemNames = new ArrayList<>();
        List<AsyncBufferedImage> itemIcons = new ArrayList<>();

        for (BankItem i : foundSave.getItemData()) {
            itemNames.add(itemManager.getItemComposition(i.getItemId()).getName());
            itemIcons.add(itemManager.getImage(i.getItemId(), i.getQuantity(), i.getQuantity() > 1));
        }
        SwingUtilities.invokeLater(() -> {
            workingToOpenBank.set(false);
            displaySavedBankData(selected.getSaveName(), itemNames, itemIcons, foundSave.getDateTimeString());
        });
    }

    private void displaySavedBankData(String saveName, List<String> itemNames, List<AsyncBufferedImage> itemIcons, String timeString) {
        assert SwingUtilities.isEventDispatchThread();

        contentPanel.removeAll();
        bankName.setText(saveName);
        contentPanel.add(backButtonAndBankName, BorderLayout.NORTH);
        contentPanel.add(bankViewPanel, BorderLayout.CENTER);
        bankViewPanel.updateTimeDisplay(timeString);
        bankViewPanel.displayItemListings(itemNames, itemIcons);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private class BanksListInteractionListenerImpl implements BanksListInteractionListener {
        @Override
        public void selectedToOpen(BanksListEntry save) {
            if (workingToOpenBank.get()) {
                return;
            }
            workingToOpenBank.set(true);
            clientThread.invokeLater(() -> openSavedBank(save));
        }

        @Override
        public void selectedToDelete(BanksListEntry save) {
            dataStore.deleteBankSaveWithId(save.getSaveId());
        }

        @Override
        public void saveBankAs(BanksListEntry save, String saveName) {
            Optional<BankSave> existingSave = dataStore.getBankSaveWithId(save.getSaveId());
            if (existingSave.isPresent()) {
                dataStore.saveAsNamedBank(saveName, existingSave.get());
            } else {
                log.error("Tried to 'Save As' missing bank save: {}", save);
            }
        }
    }

    private class BanksUpdateListener implements StoredBanksUpdateListener {
        @Override
        public void currentBanksListChanged() {
            updateCurrentBanksList();
        }

        @Override
        public void namedBanksListChanged() {
            updateCurrentBanksList();
        }

        @Override
        public void displayNameMapUpdated() {
            updateCurrentBanksList();
        }
    }
}
