package dev.osrsmcp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.WorldType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class DataManager {
    @Inject
    Client client;
    @Inject
    OsrsMcpConfig config;
    @Inject
    private CollectionLogV2Manager collectionLogV2Manager;
    @Inject
    private HttpRequestService httpRequestService;
    @Inject
    private Gson gson;
    private String lastVerifiedPlayerName = null;
    private int skipNextNAttempts = 0;

    @Getter
    private final DataState inventory = new DataState("inventory", false);
    @Getter
    private final DataState bank = new DataState("bank", false);
    @Getter
    private final DataState equipment = new DataState("equipment", false);
    @Getter
    private final DataState sharedBank = new DataState("shared_bank", true);
    @Getter
    private final DataState resources = new DataState("stats", false);
    @Getter
    private final DataState skills = new DataState("skills", false);
    @Getter
    private final DataState quests = new DataState("quests", false);
    @Getter
    private final DataState position = new DataState("coordinates", false);
    @Getter
    private final DataState runePouch = new DataState("rune_pouch", false);
    @Getter
    private final DataState quiver = new DataState("quiver", false);
    @Getter
    private final DataState interacting = new DataState("interacting", false);
    @Getter
    private final DataState seedVault = new DataState("seed_vault", false);
    @Getter
    private final DataState potionStorage = new DataState("potion_storage", false);
    @Getter
    private final DataState achievementDiary = new DataState("diary_vars", false);
    @Getter
    private final DataState slayerTask = new DataState("slayer_task", false);
    @Getter
    private final DataState combatAchievements = new DataState("combat_achievements", false);
    @Getter
    private final DataState farmingPatches = new DataState("farming_patches", false);
    @Getter
    private final DataState accountType = new DataState("account_type", false);
    @Getter
    private final DepositedItems deposited = new DepositedItems();

    public void submitToApi() {
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null || isBadWorldType()) return;
        if (skipNextNAttempts-- > 0) return;

        String playerName = client.getLocalPlayer().getName();
        String token = config.authorizationToken().trim();

        if (token.length() > 0) {
            // NOTE: We don't really need to check this everytime I don't think.
            // Waiting for a game state event is not what we really want either
            // since the token could be repointed at another login at any time from the website.
            if (needsVerification(playerName) && !verifyAndCacheLoginName(token, playerName)) {
                skipNextNAttempts = 10;
                return;
            }

            String url = getUpdateUrl();
            if (url == null) return;

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", playerName);
            inventory.consumeState(updates);
            bank.consumeState(updates);
            equipment.consumeState(updates);
            sharedBank.consumeState(updates);
            resources.consumeState(updates);
            skills.consumeState(updates);
            quests.consumeState(updates);
            position.consumeState(updates);
            runePouch.consumeState(updates);
            quiver.consumeState(updates);
            interacting.consumeState(updates);
            deposited.consumeState(updates);
            seedVault.consumeState(updates);
            potionStorage.consumeState(updates);
            achievementDiary.consumeState(updates);
            slayerTask.consumeState(updates);
            combatAchievements.consumeState(updates);
            farmingPatches.consumeState(updates);
            accountType.consumeState(updates);
            collectionLogV2Manager.consumeClogItems(updates);

            if (updates.size() > 1) {
                HttpRequestService.HttpResponse response = httpRequestService.post(url, token, updates);

                if (!response.isSuccessful()) {
                    skipNextNAttempts = 10;
                    if (response.getCode() == 401) {
                        lastVerifiedPlayerName = null;
                    }
                    restoreStateIfNothingUpdated();
                } else {
                    collectionLogV2Manager.clearClogItems();
                }
            } else {
                log.debug("Skip POST: no changes to send (fields={})", updates.size());
            }
        }
    }

    // NOTE: Re-verify whenever the actively logged-in character differs from the last one we
    // confirmed (covers both "never checked" and "character changed since last check"), not just
    // once per session.
    private boolean needsVerification(String playerName) {
        return !playerName.equalsIgnoreCase(lastVerifiedPlayerName);
    }

    // It is OK if the token is later revoked/rotated since the update call in submitToApi() will
    // return a 401 where we reset lastVerifiedPlayerName again.
    private boolean verifyAndCacheLoginName(String token, String playerName) {
        String verifiedName = verifyLoginName(token);
        if (verifiedName == null || !verifiedName.equalsIgnoreCase(playerName)) {
            return false;
        }
        lastVerifiedPlayerName = playerName;
        return true;
    }

    /** Returns the login_name the token resolves to server-side, or null if the token is invalid/unreachable. */
    private String verifyLoginName(String token) {
        String baseUrl = baseUrl();
        String url = baseUrl + "/api/am-i-logged-in";

        HttpRequestService.HttpResponse response = httpRequestService.get(url, token);
        if (!response.isSuccessful()) return null;

        try {
            JsonObject body = gson.fromJson(response.getBody(), JsonObject.class);
            return body != null && body.has("login_name") ? body.get("login_name").getAsString() : null;
        } catch (Exception e) {
            log.warn("Failed to parse am-i-logged-in response: {}", e.toString());
            return null;
        }
    }

    // NOTE: These states should only be restored if a new update did not come in at some point before calling this
    private void restoreStateIfNothingUpdated() {
        inventory.restoreState();
        bank.restoreState();
        equipment.restoreState();
        sharedBank.restoreState();
        resources.restoreState();
        skills.restoreState();
        quests.restoreState();
        position.restoreState();
        runePouch.restoreState();
        quiver.restoreState();
        interacting.restoreState();
        deposited.restoreState();
        seedVault.restoreState();
        potionStorage.restoreState();
        achievementDiary.restoreState();
        slayerTask.restoreState();
        combatAchievements.restoreState();
        farmingPatches.restoreState();
        accountType.restoreState();
    }

    public void reportSlayerTaskCompletion(SlayerTaskCompletion event) {
        postEvent("slayer-task-completed", event);
    }

    public void reportClueCompletion(ClueCompletion event) {
        postEvent("clue-completed", event);
    }

    public void reportDeath(DeathEvent event) {
        postEvent("death", event);
    }

    private void postEvent(String path, Object body) {
        String token = config.authorizationToken().trim();
        String baseUrl = baseUrl();
        if (token.isEmpty() || baseUrl == null) return;

        String url = String.format("%s/api/%s", baseUrl, path);
        httpRequestService.post(url, token, body);
    }

    private String baseUrl() {
        return httpRequestService.getBaseUrl();
    }

    private String getUpdateUrl() {
        String baseUrl = baseUrl();
        if (baseUrl == null) return null;

        return String.format("%s/api/update", baseUrl);
    }

    private boolean isBadWorldType() {
        EnumSet<WorldType> worldTypes = client.getWorldType();
        for (WorldType worldType : worldTypes) {
            if (worldType == WorldType.SEASONAL ||
                    worldType == WorldType.DEADMAN ||
                    worldType == WorldType.TOURNAMENT_WORLD ||
                    worldType == WorldType.PVP_ARENA ||
                    worldType == WorldType.BETA_WORLD ||
                    worldType == WorldType.QUEST_SPEEDRUNNING) {
                return true;
            }
        }

        return false;
    }
}
