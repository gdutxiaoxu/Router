package com.xj.arounterdemo;

import android.app.Application;
import android.content.Context;

import com.xj.router.annotation.Module;
import com.xj.router.annotation.Modules;
import com.xj.router.api.Router;


/**
 * Created by jun xu on 2019-11-12.
 */
@Modules({"app", "moudle1"})
@Module("app")
public class RouterApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Router.getInstance().init();
    }
}
