package com.bankmemory.data;

public interface DataStoreUpdateListener {
    void currentBanksListChanged();

    void currentBanksListOrderChanged();

    void snapshotBanksListChanged();

    void displayNameMapUpdated();

    // Seed vault specific notifications
    default void currentSeedVaultsListChanged() {}

    default void currentSeedVaultsListOrderChanged() {}

    default void snapshotSeedVaultsListChanged() {}
}
