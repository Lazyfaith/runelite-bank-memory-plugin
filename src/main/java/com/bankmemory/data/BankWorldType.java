package com.bankmemory.data;

import java.util.EnumSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.WorldType;

@AllArgsConstructor
@Getter
public enum BankWorldType {
    // NB: changing these name will break automatic JSON deserialisation of existing saves
    DEFAULT(""),
    LEAGUE("League"),
    TOURNAMENT("Tournament"),
    DEADMAN("DMM"),
    DEADMAN_TOURNAMENT("DMM Tournament");

    String displayString;

    public static BankWorldType forWorld(EnumSet<WorldType> worldsTypes) {
        if (worldsTypes.contains(WorldType.SEASONAL)) {
            return BankWorldType.LEAGUE;
        }
        if (worldsTypes.contains(WorldType.TOURNAMENT_WORLD)) {
            return BankWorldType.TOURNAMENT;
        }
        if (worldsTypes.contains(WorldType.DEADMAN)) {
            return BankWorldType.DEADMAN;
        }
        return BankWorldType.DEFAULT;
    }
}
