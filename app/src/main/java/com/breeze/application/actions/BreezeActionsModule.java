package com.breeze.application.actions;

import android.util.Log;

import com.breeze.application.BreezeAPI;
import com.breeze.application.BreezeModule;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class BreezeActionsModule extends BreezeModule {

    private HashMap<String, List<BreezeAction>> actionMap = new HashMap<>();
    private HashMap<String, Consumer<Object>> moduleListeners = new HashMap<>();

    public BreezeActionsModule(BreezeAPI api) {
        super(api);
        loadPendingActions();
    }

    private void loadPendingActions() {
        List<String> actionStrings = api.db.getActions();
        if (actionStrings == null) return;
        for (String as : actionStrings) {
            try {
                String actionType = new JSONObject(as).getString("actionType");

                if (actionType.equalsIgnoreCase(BreezeAction.ACTION_TYPE.SAVE_NODE.name()))
                    this.addAction(new BreezeActionSaveNode(as, true));

            } catch (Exception e) {
                Log.e("ACTIONS", "Failed to deserialize action in DB", e);
            }
        }
    }

    public void addSaveNodeAction(String nodeId) {
        BreezeActionSaveNode a = new BreezeActionSaveNode(nodeId);
        addAction(a);
    }

    private void addAction(BreezeAction a) {

        // Try the action first, if it doesn't succeed, add it to the buffer
        if (a.doAction()) return;
        api.db.addAction(a);

        // Get a list of actions waiting for that same event
        String actionName = a.getModule() + a.getEventName();
        List<BreezeAction> actionList = actionMap.get(actionName);
        if (actionList == null) {
            actionList = new LinkedList<>();
            actionMap.put(actionName, actionList);
        }

        // Add the action to the list of pending actions
        actionList.add(a);
        addModuleListener(a);
    }

    private void addModuleListener(BreezeAction a) {
        String actionName = a.getModule() + a.getEventName();
        Consumer<Object> moduleListener = moduleListeners.get(actionName);

        // If we have a listener for that module.event set up already
        if (moduleListener != null) return;

        // Otherwise, set up a new module listener
        moduleListener = o -> checkActions(actionName);

        // Add and register the listener
        moduleListeners.put(actionName, moduleListener);

        if (a.getModule() == BreezeAction.BREEZE_MODULE.GRAPH) {
            this.api.router.graph.on(a.getEventName(), moduleListener);
        } else if (a.getModule() == BreezeAction.BREEZE_MODULE.ROUTER) {
            this.api.router.on(a.getEventName(), moduleListener);
        } else if (a.getModule() == BreezeAction.BREEZE_MODULE.STATE) {
            this.api.state.on(a.getEventName(), moduleListener);
        }
    }

    private void checkActions(String actionName) {
        List<BreezeAction> actions = actionMap.get(actionName);
        if (actions == null || actions.size() == 0) return;

        List<BreezeAction> toRemove = new LinkedList<>();

        for (BreezeAction act : actions)
            if (act.doAction()) toRemove.add(act);

        actions.removeAll(toRemove);
        for (BreezeAction a : toRemove) api.db.deleteAction(a.getId());
    }
}
