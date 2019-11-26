package com.xj.router.compiler;

public class Constants {

    public static final String CLASSNAME_APPLICATION = "android.app.Application";
    public static final String CLASSNAME_ACTIVITY = "android.app.Activity";

    public static final String[] FILTER_PREFIX = new String[]{
            "com.android",
            "android",
            "java",
            "javax",
    };

    public static String ROUTE_METHOD_NAME = "initRouter";
    public static String ROUTE_CLASS_NAME = "AppRouter";
    public static String ROUTE_CLASS_PACKAGE = "com.xj.router.impl";
    public static String ROUTE_INTERFACE_NAME = "com.xj.router.api.IRoute";
}
