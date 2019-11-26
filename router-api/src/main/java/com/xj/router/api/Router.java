package com.xj.router.api;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;

import com.xj.router.impl.RouterInit;

import java.util.HashMap;
import java.util.Map;

public class Router {

    private static final String TAG = "ARouter";

    private static final Router instance = new Router();

    private Map<String, Class<? extends Activity>> routeMap = new HashMap<>();
    private boolean loaded;

    private Router() {
    }

    public static Router getInstance() {
        return instance;
    }

    public void init() {
        if (loaded) {
            return;
        }
        RouterInit.init();
        loaded = true;
    }

    public void add(String path, Class<? extends Activity> clz) {
        routeMap.put(path, clz);
        Log.i(TAG, "add: routeMap=" + routeMap);
    }

    public void add(Map<String, Class<? extends Activity>> routeMap) {
        if (routeMap != null) {
            this.routeMap.putAll(routeMap);
        }
    }


    public Map<String, Class<? extends Activity>> getMap() {
        return routeMap;
    }

    public Postcard build(String path) {
        return build(path, null);
    }

    public Postcard build(String path, RouterCallback routerCallback) {
        Class<? extends Activity> aClass = routeMap.get(path);
        if (aClass == null) {
            return new Postcard(Uri.parse(path), null, routerCallback);
        }
        return new Postcard(Uri.parse(path), aClass.getName(), routerCallback);
    }


}
