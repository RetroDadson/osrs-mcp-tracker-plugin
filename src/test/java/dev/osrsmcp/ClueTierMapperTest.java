package dev.osrsmcp;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClueTierMapperTest {

    @Test
    public void mapsAllSixCasketTiers() {
        assertEquals("beginner", ClueTierMapper.tierForCasketItemId(ItemID.TRAIL_REWARD_CASKET_BEGINNER));
        assertEquals("easy", ClueTierMapper.tierForCasketItemId(ItemID.TRAIL_REWARD_CASKET_EASY));
        assertEquals("medium", ClueTierMapper.tierForCasketItemId(ItemID.TRAIL_REWARD_CASKET_MEDIUM));
        assertEquals("hard", ClueTierMapper.tierForCasketItemId(ItemID.TRAIL_REWARD_CASKET_HARD));
        assertEquals("elite", ClueTierMapper.tierForCasketItemId(ItemID.TRAIL_REWARD_CASKET_ELITE));
        assertEquals("master", ClueTierMapper.tierForCasketItemId(ItemID.TRAIL_REWARD_CASKET_MASTER));
    }

    @Test
    public void nonCasketItemReturnsNull() {
        assertNull(ClueTierMapper.tierForCasketItemId(995)); // coins
    }
}
