package dev.osrsmcp;

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
    private boolean isMemberRegistered = false;
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
    private final DepositedItems deposited = new DepositedItems();

    public void submitToApi() {
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null || isBadWorldType()) return;
        if (skipNextNAttempts-- > 0) return;

        String playerName = client.getLocalPlayer().getName();
        String token = config.authorizationToken().trim();

        if (token.length() > 0) {
            // NOTE: We do this check so characters who are not authorized won't waste time serializing and sending
            // their data. It is OK if the user switches characters or is removed from the login since the update call
            // below will return a 401 where we set isMemberRegistered = false again.
            if (!isMemberRegistered) {
                boolean isMember = checkIfPlayerIsRegistered(token, playerName);

                if (!isMember) {
                    // NOTE: We don't really need to check this everytime I don't think.
                    // Waiting for a game state event is not what we really want either
                    // since membership can change at anytime from the website.
                    skipNextNAttempts = 10;
                    return;
                }
                isMemberRegistered = true;
            }

            String url = getUpdateMemberUrl();
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
            collectionLogV2Manager.consumeClogItems(updates);

            if (updates.size() > 1) {
                HttpRequestService.HttpResponse response = httpRequestService.post(url, token, updates);

                if (!response.isSuccessful()) {
                    skipNextNAttempts = 10;
                    if (response.getCode() == 401) {
                        isMemberRegistered = false;
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

    private boolean checkIfPlayerIsRegistered(String token, String playerName) {
        String url = amIMemberUrl(playerName);
        if (url == null) return false;

        HttpRequestService.HttpResponse response = httpRequestService.get(url, token);

        return response.isSuccessful();
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
        String loginName = loginName();
        if (token.isEmpty() || baseUrl == null || loginName == null) return;

        String url = String.format("%s/api/login/%s/%s", baseUrl, loginName, path);
        httpRequestService.post(url, token, body);
    }

    private String baseUrl() {
        return httpRequestService.getBaseUrl();
    }

    private String loginName() {
        String loginName = config.loginName().trim();
        if (loginName.length() == 0) {
            return null;
        }

        return loginName;
    }

    private String getUpdateMemberUrl() {
        String baseUrl = baseUrl();
        String loginName = loginName();

        if (baseUrl == null || loginName == null) return null;

        return String.format("%s/api/login/%s/update-member", baseUrl, loginName);
    }

    private String amIMemberUrl(String playerName) {
        String baseUrl = baseUrl();
        String loginName = loginName();

        if (baseUrl == null || loginName == null) return null;

        return String.format("%s/api/login/%s/am-i-a-member?member_name=%s", baseUrl, loginName, playerName);
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
