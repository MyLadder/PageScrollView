package com.rexy.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by rexy on 17/6/7.
 */

public class ActivityLauncher extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this,ExampleActivity.class));
        finish();
    }
}
