package com.bankmemory;

import java.awt.BorderLayout;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;


import static com.bankmemory.util.Constants.PAD;

@Singleton
class BankMemoryPluginPanel extends PluginPanel {

    private final BankViewPanel currentBankViewPanel = new BankViewPanel();
    private final BankSavesTopPanel savedBanksTopPanel = new BankSavesTopPanel();

    private final JPanel displayPanel = new JPanel();
    private final MaterialTabGroup tabGroup = new MaterialTabGroup(displayPanel);
    private final MaterialTab currentBankTab;
    private final MaterialTab savesListTab;

    protected BankMemoryPluginPanel() {
        super(false);
        setBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

        currentBankTab = new MaterialTab("Current bank", tabGroup, currentBankViewPanel);
        savesListTab = new MaterialTab("Saved banks", tabGroup, savedBanksTopPanel);
        tabGroup.addTab(currentBankTab);
        tabGroup.addTab(savesListTab);
        tabGroup.select(currentBankTab);

        setLayout(new BorderLayout());
        add(tabGroup, BorderLayout.NORTH);
        add(displayPanel, BorderLayout.CENTER);
    }

    BankViewPanel getCurrentBankViewPanel() {
        return currentBankViewPanel;
    }

    BankSavesTopPanel getSavedBanksTopPanel() {
        return savedBanksTopPanel;
    }
}
