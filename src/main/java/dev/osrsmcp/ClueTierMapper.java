package dev.osrsmcp;

import net.runelite.api.gameval.ItemID;

import javax.annotation.Nullable;

/**
 * Maps a Reward Casket item ID to its clue tier. Item IDs are Jagex-assigned and effectively
 * permanent (unlike varbit meanings, which occasionally get restructured); resolved via
 * RuneLite's own generated {@link ItemID} constants rather than bare literals so a future
 * rename/renumber upstream would be caught at compile time.
 */
public final class ClueTierMapper {
    private ClueTierMapper() {
    }

    @Nullable
    public static String tierForCasketItemId(int itemId) {
        switch (itemId) {
            case ItemID.TRAIL_REWARD_CASKET_BEGINNER:
                return "beginner";
            case ItemID.TRAIL_REWARD_CASKET_EASY:
                return "easy";
            case ItemID.TRAIL_REWARD_CASKET_MEDIUM:
                return "medium";
            case ItemID.TRAIL_REWARD_CASKET_HARD:
                return "hard";
            case ItemID.TRAIL_REWARD_CASKET_ELITE:
                return "elite";
            case ItemID.TRAIL_REWARD_CASKET_MASTER:
                return "master";
            default:
                return null;
        }
    }
}
