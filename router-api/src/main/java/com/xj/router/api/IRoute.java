package com.xj.router.api;

import android.app.Activity;

import java.util.Map;


public interface IRoute {

    void initRouter(Map<String, Class<? extends Activity>> routers);

}
