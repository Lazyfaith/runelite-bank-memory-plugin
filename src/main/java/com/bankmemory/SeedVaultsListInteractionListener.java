package com.bankmemory;

public interface SeedVaultsListInteractionListener {
    void selectedToOpen(BanksListEntry save);

    void selectedToDelete(BanksListEntry save);

    void saveSeedVaultAs(BanksListEntry save, String saveName);

    void copySeedVaultSaveItemDataToClipboard(BanksListEntry save);

    void openSeedVaultsDiffPanel();
}
