package com.bankmemory;

import com.bankmemory.bankview.BankViewPanel;
import com.bankmemory.bankview.ItemListEntry;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

public class SeedVaultSavesTopPanel extends JPanel {
    static {
        BufferedImage backIcon = ImageUtil.getResourceStreamFromClass(SeedVaultSavesTopPanel.class, "back_icon.png");
        BACK_ICON = new ImageIcon(backIcon);
        BACK_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(backIcon, -100));
    }

    private static final Icon BACK_ICON;
    private static final Icon BACK_ICON_HOVER;

    private final SeedVaultsListPanel vaultsListPanel = new SeedVaultsListPanel();
    private final BankViewPanel vaultViewPanel = new BankViewPanel();
    private final BankDiffPanel vaultDiffPanel = new BankDiffPanel();
    private final JPanel backButtonAndTitle = new JPanel();
    private final JLabel subUiTitle = new JLabel();

    public SeedVaultSavesTopPanel() {
        super();
        setLayout(new BorderLayout());

        backButtonAndTitle.setLayout(new BoxLayout(backButtonAndTitle, BoxLayout.LINE_AXIS));
        JButton backButton = new JButton(BACK_ICON);
        SwingUtil.removeButtonDecorations(backButton);
        backButton.setRolloverIcon(BACK_ICON_HOVER);
        backButton.addActionListener(e -> displayVaultsListPanel());
        backButtonAndTitle.add(backButton);
        backButtonAndTitle.add(subUiTitle);
    }

    void setVaultsListInteractionListener(SeedVaultsListInteractionListener listener) {
        vaultsListPanel.setInteractionListener(listener);
    }

    void displayVaultsListPanel() {
        vaultViewPanel.reset();
        removeAll();
        add(vaultsListPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    void updateVaultsList(List<BanksListEntry> entries) {
        vaultsListPanel.updateVaultsList(entries);
    }

    void displaySavedVaultData(String saveName, List<ItemListEntry> items, String timeString) {
        removeAll();
        subUiTitle.setText(saveName);
        add(backButtonAndTitle, BorderLayout.NORTH);
        add(vaultViewPanel, BorderLayout.CENTER);
        vaultViewPanel.updateTimeDisplay(timeString);
        vaultViewPanel.displayItemListings(items, false);
        revalidate();
        repaint();
    }

    public BankViewPanel getVaultViewPanel() {
        return vaultViewPanel;
    }

    public BankDiffPanel getDiffPanel() {
        return vaultDiffPanel;
    }

    void showVaultDiffPanel() {
        removeAll();
        subUiTitle.setText("Seed vault comparison");
        add(backButtonAndTitle, BorderLayout.NORTH);
        add(vaultDiffPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
