package com.example.mylibrary;

import android.os.Bundle;

import com.xj.router.annotation.Module;
import com.xj.router.annotation.Route;

import androidx.appcompat.app.AppCompatActivity;

@Route(path = "my/activity/main")
@Module("moudle1")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
    }
}
