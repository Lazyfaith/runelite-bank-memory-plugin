package com.bankmemory;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

public class BankSavesTopPanel extends JPanel {
    static {
        BufferedImage backIcon = ImageUtil.getResourceStreamFromClass(BankSavesTopPanel.class, "back_icon.png");
        BACK_ICON = new ImageIcon(backIcon);
        BACK_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(backIcon, -100));
    }

    private static final Icon BACK_ICON;
    private static final Icon BACK_ICON_HOVER;

    private final BanksListPanel banksListPanel = new BanksListPanel();
    private final BankViewPanel bankViewPanel = new BankViewPanel();
    private final JPanel backButtonAndBankName = new JPanel();
    private final JLabel bankName = new JLabel();

    public BankSavesTopPanel() {
        super();
        setLayout(new BorderLayout());

        backButtonAndBankName.setLayout(new BoxLayout(backButtonAndBankName, BoxLayout.LINE_AXIS));
        JButton backButton = new JButton(BACK_ICON);
        SwingUtil.removeButtonDecorations(backButton);
        backButton.setRolloverIcon(BACK_ICON_HOVER);
        backButton.addActionListener(e -> displayBanksListPanel());
        backButtonAndBankName.add(backButton);
        backButtonAndBankName.add(bankName);
    }

    void setBanksListInteractionListener(BanksListInteractionListener listener) {
        banksListPanel.setInteractionListener(listener);
    }

    void displayBanksListPanel() {
        bankViewPanel.reset();
        removeAll();
        add(banksListPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    void updateBanksList(List<BanksListEntry> entries) {
        banksListPanel.updateBanksList(entries);
    }

    void displaySavedBankData(String saveName, List<String> itemNames, List<AsyncBufferedImage> itemIcons, String timeString) {
        removeAll();
        bankName.setText(saveName);
        add(backButtonAndBankName, BorderLayout.NORTH);
        add(bankViewPanel, BorderLayout.CENTER);
        bankViewPanel.updateTimeDisplay(timeString);
        bankViewPanel.displayItemListings(itemNames, itemIcons);
        revalidate();
        repaint();
    }
}
