package com.bankmemory;

import com.bankmemory.bankview.BankViewPanel;
import com.bankmemory.data.StorageType;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;


import static com.bankmemory.util.Constants.PAD;

class BankMemoryPluginPanel extends PluginPanel {

    private final BankViewPanel currentBankViewPanel = new BankViewPanel();
    private final BankSavesTopPanel savedBanksTopPanel = new BankSavesTopPanel();

    private final BankViewPanel currentSeedVaultViewPanel = new BankViewPanel();
    private final SeedVaultSavesTopPanel savedSeedVaultTopPanel = new SeedVaultSavesTopPanel();

    private final JPanel contentCards = new JPanel(new CardLayout());
    private static final String CARD_BANK = "bank";
    private static final String CARD_SEED = "seed";

    protected BankMemoryPluginPanel() {
        super(false);
        setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

        // Bank group
        JPanel bankDisplayPanel = new JPanel();
        MaterialTabGroup bankTabGroup = new MaterialTabGroup(bankDisplayPanel);
        MaterialTab currentBankTab = new MaterialTab("Current", bankTabGroup, currentBankViewPanel);
        MaterialTab savesBankTab = new MaterialTab("Saved", bankTabGroup, savedBanksTopPanel);
        bankTabGroup.addTab(currentBankTab);
        bankTabGroup.addTab(savesBankTab);
        bankTabGroup.select(currentBankTab);
        JPanel bankGroupPanel = new JPanel(new BorderLayout());
        bankGroupPanel.add(bankTabGroup, BorderLayout.NORTH);
        bankGroupPanel.add(bankDisplayPanel, BorderLayout.CENTER);

        // Seed vault group
        JPanel seedDisplayPanel = new JPanel();
        MaterialTabGroup seedTabGroup = new MaterialTabGroup(seedDisplayPanel);
        MaterialTab currentSeedTab = new MaterialTab("Current", seedTabGroup, currentSeedVaultViewPanel);
        MaterialTab savedSeedTab = new MaterialTab("Saved", seedTabGroup, savedSeedVaultTopPanel);
        seedTabGroup.addTab(currentSeedTab);
        seedTabGroup.addTab(savedSeedTab);
        seedTabGroup.select(currentSeedTab);
        JPanel seedGroupPanel = new JPanel(new BorderLayout());
        seedGroupPanel.add(seedTabGroup, BorderLayout.NORTH);
        seedGroupPanel.add(seedDisplayPanel, BorderLayout.CENTER);

        contentCards.add(bankGroupPanel, CARD_BANK);
        contentCards.add(seedGroupPanel, CARD_SEED);

        JComboBox<StorageType> dropdown = new JComboBox<>(StorageType.values());
        dropdown.addActionListener(e -> {
            StorageType selected = (StorageType) dropdown.getSelectedItem();
            CardLayout cl = (CardLayout) contentCards.getLayout();
            if (selected == StorageType.SEED_VAULT) {
                cl.show(contentCards, CARD_SEED);
            } else {
                cl.show(contentCards, CARD_BANK);
            }
        });
        dropdown.setSelectedItem(StorageType.BANK);

        setLayout(new BorderLayout());
        add(dropdown, BorderLayout.NORTH);
        add(contentCards, BorderLayout.CENTER);
    }

    BankViewPanel getCurrentBankViewPanel() {
        return currentBankViewPanel;
    }

    BankSavesTopPanel getSavedBanksTopPanel() {
        return savedBanksTopPanel;
    }

    BankViewPanel getCurrentSeedVaultViewPanel() {
        return currentSeedVaultViewPanel;
    }

    SeedVaultSavesTopPanel getSavedSeedVaultTopPanel() {
        return savedSeedVaultTopPanel;
    }
}
