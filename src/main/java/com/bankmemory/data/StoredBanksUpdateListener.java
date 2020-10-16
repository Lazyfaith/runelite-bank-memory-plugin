package com.bankmemory.data;

public interface StoredBanksUpdateListener {
    void currentBanksListChanged();

    void namedBanksListChanged();

    void displayNameMapUpdated();
}
