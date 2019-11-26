package com.xj.router.api;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

public class Postcard {

    private final RouterCallback mRouterCallback;
    private final Uri mUri;
    private String mActivityName;
    private Bundle mBundle;

    public Postcard(Uri uri, String activityName, RouterCallback routerCallback) {
        mUri = uri;
        this.mActivityName = activityName;
        mRouterCallback = routerCallback;
        mBundle = new Bundle();
    }

    public Postcard withString(String key, String value) {
        mBundle.putString(key, value);
        return this;
    }

    public Postcard withInt(String key, int value) {
        mBundle.putInt(key, value);
        return this;
    }

    public Postcard withFloat(String key, float value) {
        mBundle.putFloat(key, value);
        return this;
    }

    public Postcard withLong(String key, long value) {
        mBundle.putLong(key, value);
        return this;
    }

    public Postcard withParcelable(String key, Parcelable value) {
        mBundle.putParcelable(key, value);
        return this;
    }

    public Postcard with(Bundle bundle) {
        if (null != bundle) {
            mBundle = bundle;
        }
        return this;
    }

    public void navigation(Context context) {
        beforeOpen(context);
        boolean isFind = false;
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(context.getPackageName(), mActivityName));
            intent.putExtras(mBundle);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            isFind = true;
        } catch (Exception e) {
            errorOpen(context, e);
            tryToCallNotFind(e, context);
            isFind = false;
        }

        if (isFind) {
            afterOpen(context);
        }
    }

    private void tryToCallNotFind(Exception e, Context context) {
        if (e instanceof ClassNotFoundException && mRouterCallback != null) {
            mRouterCallback.notFind(context, mUri);
        }
    }


    private void afterOpen(Context context) {
        if (mRouterCallback != null) {
            mRouterCallback.afterOpen(context, mUri);
        }
    }

    private void errorOpen(Context context, Exception e) {
        if (mRouterCallback != null) {
            mRouterCallback.error(context, mUri, e);
        }
    }

    private void beforeOpen(Context context) {
        if (mRouterCallback != null) {
            mRouterCallback.beforeOpen(context, mUri);
        }
    }

    public void navigation(Activity context, int requestCode) {
        beforeOpen(context);
        boolean isFind = false;
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(context.getPackageName(), mActivityName));
            intent.putExtras(mBundle);
            context.startActivityForResult(intent, requestCode);
            isFind = true;
        } catch (Exception e) {
            errorOpen(context, e);
            tryToCallNotFind(e, context);
            isFind = false;
        }

        if (isFind) {
            afterOpen(context);
        }

    }

    public void navigation(Activity context, int requestCode, Callback callback) {
        beforeOpen(context);
        boolean isFind = false;
        try {
            Activity activity = (Activity) context;
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(context.getPackageName(), mActivityName));
            intent.putExtras(mBundle);
            getFragment(activity)
                    .setCallback(callback)
                    .startActivityForResult(intent, requestCode);
            isFind = true;
        } catch (Exception e) {
            errorOpen(context, e);
            tryToCallNotFind(e, context);
        }

        if (isFind) {
            afterOpen(context);
        }

    }

    public void navigation(Activity context, Callback callback) {
        navigation(context, 10, callback);
    }

    private static final String TAG = "com.xj.router.api.Postcard";

    private CallbackFragment getFragment(Activity activity) {
        CallbackFragment fragment = (CallbackFragment) activity.getFragmentManager().findFragmentByTag(TAG);
        if (fragment == null) {
            fragment = new CallbackFragment();
            FragmentManager fm = activity.getFragmentManager();
            fm.beginTransaction().add(fragment, TAG)
                    .commitAllowingStateLoss();
            fm.executePendingTransactions();
        }
        return fragment;
    }


}
