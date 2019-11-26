package com.xj.arounterdemo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.xj.router.annotation.Route;
import com.xj.router.api.Callback;
import com.xj.router.api.Router;

import androidx.appcompat.app.AppCompatActivity;

@Route(path = "activity/one")
public class OneActivity extends AppCompatActivity {

    private static final String TAG = "OneActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one);
    }

    public void onButtonClick(View view) {
        Router.getInstance().build("activity/two").navigation(this, new Callback() {
            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                Log.i(TAG, "onActivityResult: requestCode=" + requestCode + ";resultCode=" + resultCode + ";data=" + data);
            }
        });
    }
}
