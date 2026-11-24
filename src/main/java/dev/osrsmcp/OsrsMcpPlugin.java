package dev.osrsmcp;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@PluginDescriptor(
        name = "OSRS MCP Tracker"
)
public class OsrsMcpPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private DataManager dataManager;
    @Inject
    private ItemManager itemManager;
    @Inject
    private CollectionLogWidgetSubscriber collectionLogWidgetSubscriber;
    private int itemsDeposited = 0;
    private boolean cachePotions = false;
    private Set<Integer> potionStoreVars;
    private static final int SECONDS_BETWEEN_UPLOADS = 1;
    private static final int SECONDS_BETWEEN_INFREQUENT_DATA_CHANGES = 60;
    private static final int DEPOSIT_ITEM = 12582914;
    private static final int DEPOSIT_INVENTORY = 12582916;
    private static final int DEPOSIT_EQUIPMENT = 12582918;
    private static final int CHATBOX_ENTERED = 681;
    private static final int GROUP_STORAGE_LOADER = 293;

    @Override
    protected void startUp() throws Exception {
        collectionLogWidgetSubscriber.startUp();
        log.info("OSRS MCP Tracker v{} started!", PluginVersion.get());
    }

    @Override
    protected void shutDown() throws Exception {
        collectionLogWidgetSubscriber.shutDown();
        cachePotions = false;
        potionStoreVars = null;
        log.info("OSRS MCP Tracker stopped!");
    }

    @Schedule(
            period = SECONDS_BETWEEN_UPLOADS,
            unit = ChronoUnit.SECONDS,
            asynchronous = true
    )
    public void submitToApi() {
        dataManager.submitToApi();
    }

    @Schedule(
            period = SECONDS_BETWEEN_UPLOADS,
            unit = ChronoUnit.SECONDS
    )
    public void updateThingsThatDoChangeOften() {
        if (doNotUseThisData())
            return;
        Player player = client.getLocalPlayer();
        String playerName = player.getName();
        dataManager.getResources().update(new ResourcesState(playerName, client));
        dataManager.getRunePouch().update(new RunePouchState(playerName, client));
        dataManager.getQuiver().update(new QuiverState(playerName, client, itemManager));
    }

    @Schedule(
            period = SECONDS_BETWEEN_INFREQUENT_DATA_CHANGES,
            unit = ChronoUnit.SECONDS
    )
    public void updateThingsThatDoNotChangeOften() {
        if (doNotUseThisData())
            return;
        String playerName = client.getLocalPlayer().getName();
        dataManager.getQuests().update(new QuestState(playerName, client));
        dataManager.getAchievementDiary().update(new AchievementDiaryState(playerName, client));
        dataManager.getCombatAchievements().update(new CombatAchievementsState(playerName, client));
        dataManager.getFarmingPatches().update(FarmingPatchState.fromClient(playerName, client));
        refreshSlayerTask(playerName);
        refreshAccountType(playerName);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (doNotUseThisData()) return;

        if (event.getVarbitId() == VarbitID.IRONMAN) {
            refreshAccountType(client.getLocalPlayer().getName());
        }

        final int varpId = event.getVarpId();
        if (varpId == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO || varpId == VarPlayerID.DIZANAS_QUIVER_TEMP_AMMO_AMOUNT) {
            String playerName = client.getLocalPlayer().getName();
            dataManager.getQuiver().update(new QuiverState(playerName, client, itemManager));
        }

        if (varpId == VarPlayerID.SLAYER_TARGET) {
            handleSlayerTaskChange(client.getLocalPlayer().getName());
        } else if (varpId == VarPlayerID.SLAYER_COUNT) {
            refreshSlayerTask(client.getLocalPlayer().getName());
        }

        if (potionStoreVars != null && potionStoreVars.contains(varpId)) {
            cachePotions = true;
        }
    }

    @Subscribe
    public void onClientTick(ClientTick event) {
        if (cachePotions) {
            cachePotions = false;
            updatePotionStorage();

            Widget w = client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
            if (w != null && potionStoreVars == null) {
                int[] trigger = w.getVarTransmitTrigger();
                potionStoreVars = new HashSet<>();
                for (int varId : trigger) {
                    potionStoreVars.add(varId);
                }
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        --itemsDeposited;
        updateInteracting();

        Widget groupStorageLoaderText = client.getWidget(GROUP_STORAGE_LOADER, 1);
        if (groupStorageLoaderText != null) {
            if (groupStorageLoaderText.getText().equalsIgnoreCase("saving...")) {
                dataManager.getSharedBank().commitTransaction();
            }
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        if (doNotUseThisData())
            return;
        String playerName = client.getLocalPlayer().getName();
        dataManager.getSkills().update(new SkillState(playerName, client));
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (doNotUseThisData())
            return;
        String playerName = client.getLocalPlayer().getName();
        final int id = event.getContainerId();
        ItemContainer container = event.getItemContainer();

        if (id == InventoryID.BANK) {
            dataManager.getDeposited().reset();
            dataManager.getBank().update(new ItemContainerState(playerName, container, itemManager));
        } else if (id == InventoryID.SEED_VAULT) {
            dataManager.getSeedVault().update(new ItemContainerState(playerName, container, itemManager));
        } else if (id == InventoryID.INV) {
            ItemContainerState previousInventoryState = (ItemContainerState) dataManager.getInventory().mostRecentState();
            ItemContainerState newInventoryState = new ItemContainerState(playerName, container, itemManager, 28);
            if (itemsDeposited > 0) {
                updateDeposited(newInventoryState, previousInventoryState);
            }
            if (previousInventoryState != null) {
                detectClueCompletions(playerName, previousInventoryState, newInventoryState);
            }

            dataManager.getInventory().update(newInventoryState);
        } else if (id == InventoryID.WORN) {
            ItemContainerState newEquipmentState = new ItemContainerState(playerName, container, itemManager, 14);
            if (itemsDeposited > 0) {
                updateDeposited(newEquipmentState, (ItemContainerState) dataManager.getEquipment().mostRecentState());
            }

            dataManager.getEquipment().update(newEquipmentState);
        } else if (id == InventoryID.INV_GROUP_TEMP) {
            dataManager.getSharedBank().update(new ItemContainerState(playerName, container, itemManager));
        }
    }

    @Subscribe
    private void onScriptPreFired(ScriptPreFired event) {
        if (doNotUseThisData()) return;
        if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING) {
            cachePotions = true;
        }
    }

    @Subscribe
    private void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() == CHATBOX_ENTERED && client.getWidget(InterfaceID.BankDepositbox.INVENTORY) != null) {
            itemsMayHaveBeenDeposited();
        }
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event) {
        final int param1 = event.getParam1();
        final MenuAction menuAction = event.getMenuAction();
        if (menuAction == MenuAction.CC_OP) {
            if (param1 == DEPOSIT_ITEM || param1 == DEPOSIT_INVENTORY || param1 == DEPOSIT_EQUIPMENT) {
                itemsMayHaveBeenDeposited();
            }
        }
    }

    @Subscribe
    private void onInteractingChanged(InteractingChanged event) {
        if (event.getSource() != client.getLocalPlayer()) return;
        updateInteracting();
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        if (doNotUseThisData()) return;
        Player player = client.getLocalPlayer();
        if (event.getActor() != player) return;

        dataManager.reportDeath(new DeathEvent(player.getName(), resolveKiller()));
    }

    @Nullable
    private String resolveKiller() {
        InteractingState interacting = (InteractingState) dataManager.getInteracting().mostRecentState();
        return interacting != null ? interacting.getName() : null;
    }

    private void itemsMayHaveBeenDeposited() {
        // NOTE: In order to determine if an item has gone through the deposit box we first detect if any of the menu
        // actions were performed OR a custom amount was entered while the deposit box inventory widget was opened.
        // Then we allow up to two game ticks were an inventory changed event can occur and at that point we assume
        // it must have been caused by the action detected just before. We can't check the inventory at the time of
        // either interaction since the inventory may have not been updated yet. We also cannot just check that the deposit
        // box window is open in the item container event since it is possible for a player to close the widget before
        // the event handler is called.
        itemsDeposited = 2;
    }

    private void updateInteracting() {
        Player player = client.getLocalPlayer();

        if (player != null) {
            Actor actor = player.getInteracting();

            if (actor != null) {
                String playerName = player.getName();
                dataManager.getInteracting().update(new InteractingState(playerName, actor));
            }
        }
    }

    private void handleSlayerTaskChange(String playerName) {
        SlayerTaskState previousTask = (SlayerTaskState) dataManager.getSlayerTask().mostRecentState();
        int newTaskId = client.getVarpValue(VarPlayerID.SLAYER_TARGET);

        if (previousTask != null && previousTask.taskId != newTaskId) {
            int amountCompleted = previousTask.getInitialAmount() - previousTask.getAmountRemaining();
            String outcome = SlayerTaskState.inferOutcome(previousTask.getAmountRemaining());
            dataManager.reportSlayerTaskCompletion(new SlayerTaskCompletion(
                    playerName, previousTask.getTask(), previousTask.getMaster(), amountCompleted, outcome));
        }

        refreshSlayerTask(playerName);
    }

    private void refreshSlayerTask(String playerName) {
        SlayerTaskState newTask = SlayerTaskState.fromClient(playerName, client);
        if (newTask != null) {
            dataManager.getSlayerTask().update(newTask);
        }
    }

    private void refreshAccountType(String playerName) {
        AccountTypeState accountType = AccountTypeState.fromClient(playerName, client);
        if (accountType != null) {
            dataManager.getAccountType().update(accountType);
        }
    }

    private void detectClueCompletions(String playerName, ItemContainerState previous, ItemContainerState current) {
        ItemContainerState added = previous.whatGotRemoved(current);
        if (added == null) return;

        for (Integer itemId : added.getItemMap().keySet()) {
            String tier = ClueTierMapper.tierForCasketItemId(itemId);
            if (tier != null) {
                dataManager.reportClueCompletion(new ClueCompletion(playerName, tier));
            }
        }
    }

    private void updateDeposited(ItemContainerState newState, ItemContainerState previousState) {
        ItemContainerState deposited = newState.whatGotRemoved(previousState);
        dataManager.getDeposited().update(deposited);
    }

    private void updatePotionStorage() {
        Player player = client.getLocalPlayer();

        if (player != null) {
            PotionStorageState potionStorageState = PotionStorageState.fromClient(player.getName(), client, itemManager);
            dataManager.getPotionStorage().update(potionStorageState);
        }
    }

    private boolean doNotUseThisData() {
        return client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null;
    }

    @Provides
    OsrsMcpConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(OsrsMcpConfig.class);
    }
}
