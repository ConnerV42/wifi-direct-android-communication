package com.breeze.state;

import java.util.ArrayList;
import java.util.HashMap;

public class BrzStateStore {

    private HashMap<String, HashMap<String, Object>> store = new HashMap<>();
    private HashMap<String, ArrayList<BrzStateObserver>> pathListeners = new HashMap<>();

    private static BrzStateStore instance = new BrzStateStore();
    public static BrzStateStore getStore() {
        return instance;
    }

    public void listen(BrzStateObserver listener, String path) {
        if(listener == null || path == null || path.compareTo("") == 0)
            throw new RuntimeException("Invalid params");

        ArrayList<BrzStateObserver> pathList = this.pathListeners.get(path);
        if(pathList == null) {
            pathList = new ArrayList<BrzStateObserver>();
            this.pathListeners.put(path, pathList);
        }

        pathList.add(listener);
        listener.stateChange(new BrzStateChangeEvent(path, this.getVal(path)));
    }

    public void setVal(String path, Object value) {
        String[] pathParts = path.split("/");
        if(pathParts.length != 2) throw new RuntimeException("Invalid path");

        HashMap<String, Object> map = this.store.get(pathParts[0]);
        if(this.store.get(pathParts[0]) == null) {
            map = new HashMap<String, Object>();
            this.store.put(pathParts[0], map);
        }

        map.put(pathParts[1], value);

        BrzStateChangeEvent e = new BrzStateChangeEvent(path, value);
        ArrayList<BrzStateObserver> pl = pathListeners.get(path);

        if(pl != null)
            for(BrzStateObserver o : pl) o.stateChange(e);
    }

    public Object getVal(String path) {
        String[] pathParts = path.split("/");

        if(pathParts.length != 2) return null;
        if(this.store.get(pathParts[0]) == null) return null;

        return this.store.get(pathParts[0]).get(pathParts[1]);
    }


}
