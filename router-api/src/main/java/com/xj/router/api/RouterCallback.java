package com.xj.router.api;

import android.content.Context;
import android.net.Uri;

/**
 * 博客地址：http://blog.csdn.net/gdutxiaoxu
 *
 * @author xujun
 * 2019-11-13 20:36.
 */
public interface RouterCallback {

    /**
     * 在跳转 router 之前
     * @param context
     * @param uri
     * @return
     */
    boolean beforeOpen(Context context, Uri uri);

    /**
     * 在跳转 router 之后
     * @param context
     * @param uri
     */
    void afterOpen(Context context, Uri uri);

    /**
     * 没有找到改 router
     * @param context
     * @param uri
     */
    void notFind(Context context, Uri uri);

    /**
     * 跳转 router 错误
     * @param context
     * @param uri
     * @param e
     */
    void error(Context context, Uri uri, Throwable e);
}
