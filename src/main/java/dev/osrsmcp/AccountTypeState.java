package dev.osrsmcp;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

/**
 * The account's game mode, read straight from the in-game {@link VarbitID#IRONMAN} varbit. This is
 * the only reliable source for Group Ironman variants - the OSRS Hiscores rank GIM players on the
 * regular board, so the server's hiscores probe alone would mislabel them as "regular".
 */
public class AccountTypeState implements ConsumableState {
    private final String accountType;
    private final transient String playerName;

    AccountTypeState(String playerName, String accountType) {
        this.playerName = playerName;
        this.accountType = accountType;
    }

    /** Returns null when the varbit holds a value we don't recognise, so nothing bogus is sent. */
    public static AccountTypeState fromClient(String playerName, Client client) {
        String accountType = fromVarbitValue(client.getVarbitValue(VarbitID.IRONMAN));
        return accountType == null ? null : new AccountTypeState(playerName, accountType);
    }

    /** Maps the IRONMAN varbit to the server's account-type identifiers. */
    public static String fromVarbitValue(int value) {
        switch (value) {
            case 0:
                return "regular";
            case 1:
                return "ironman";
            case 2:
                return "ultimate";
            case 3:
                return "hardcore_ironman";
            case 4:
                return "group_ironman";
            case 5:
                return "hardcore_group_ironman";
            case 6:
                return "unranked_group_ironman";
            default:
                return null;
        }
    }

    @Override
    public Object get() {
        return accountType;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof AccountTypeState)) return false;

        AccountTypeState other = (AccountTypeState) o;
        return accountType.equals(other.accountType) && playerName.equals(other.playerName);
    }

    @Override
    public int hashCode() {
        return accountType.hashCode() * 31 + playerName.hashCode();
    }
}
