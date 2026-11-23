package dev.osrsmcp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class AccountTypeStateTest {

    @Test
    public void mapsEveryKnownVarbitValue() {
        assertEquals("regular", AccountTypeState.fromVarbitValue(0));
        assertEquals("ironman", AccountTypeState.fromVarbitValue(1));
        assertEquals("ultimate", AccountTypeState.fromVarbitValue(2));
        assertEquals("hardcore_ironman", AccountTypeState.fromVarbitValue(3));
        assertEquals("group_ironman", AccountTypeState.fromVarbitValue(4));
        assertEquals("hardcore_group_ironman", AccountTypeState.fromVarbitValue(5));
        assertEquals("unranked_group_ironman", AccountTypeState.fromVarbitValue(6));
    }

    @Test
    public void unknownVarbitValueIsNull() {
        assertNull(AccountTypeState.fromVarbitValue(7));
        assertNull(AccountTypeState.fromVarbitValue(-1));
    }

    @Test
    public void getReturnsTheAccountType() {
        assertEquals("group_ironman", new AccountTypeState("Player", "group_ironman").get());
    }

    @Test
    public void equalityCoversTypeAndOwner() {
        AccountTypeState a = new AccountTypeState("Player", "group_ironman");
        assertEquals(a, new AccountTypeState("Player", "group_ironman"));
        assertEquals(a.hashCode(), new AccountTypeState("Player", "group_ironman").hashCode());
        assertNotEquals(a, new AccountTypeState("Player", "ironman"));
        assertNotEquals(a, new AccountTypeState("Other", "group_ironman"));
    }
}
