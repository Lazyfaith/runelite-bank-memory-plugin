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
        if (worldsTypes.contains(WorldType.LEAGUE)) {
            return BankWorldType.LEAGUE;
        }
        if (worldsTypes.contains(WorldType.TOURNAMENT)) {
            return BankWorldType.TOURNAMENT;
        }
        if (worldsTypes.contains(WorldType.DEADMAN_TOURNAMENT)) {
            return BankWorldType.DEADMAN_TOURNAMENT;
        }
        if (worldsTypes.contains(WorldType.DEADMAN)) {
            return BankWorldType.DEADMAN;
        }
        return BankWorldType.DEFAULT;
    }
}
