package com.bankmemory.data;

public enum StorageType {
    BANK,
    SEED_VAULT;

    @Override
    public String toString() {
        switch (this) {
            case BANK:
                return "Bank";
            case SEED_VAULT:
                return "Seed Vault";
            default:
                return name();
        }
    }
}
