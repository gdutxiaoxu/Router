package com.xj.router.api;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

public class CallbackFragment extends Fragment {
    private Callback callback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public CallbackFragment setCallback(Callback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (callback != null) {
            callback.onActivityResult(requestCode, resultCode, data);
        }
    }

}
