package com.vnd.superbible7;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class GLESActivity extends AppCompatActivity {
    private Object glesView = null;
    private String className = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set sensor landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        // Retrieve the className passed from the Intent
        className = getIntent().getStringExtra("className");

        if (className != null) {
            // Create a new class instance using reflection
            try {
                glesView = Class.forName(className).getDeclaredConstructor(Context.class).newInstance(this);
            } catch (Exception e) {
                Log.e("VND:", "Error during uninitialize", e);
            }

            // Set the x1_SimpleClear as the content view of the activity
            setContentView((View) glesView);
        }
    }

    /**
     * @noinspection EmptyMethod
     */
    @Override
    protected void onPause() {
        super.onPause();
        // glesView.onPause();
    }

    /**
     * @noinspection EmptyMethod
     */
    @Override
    protected void onResume() {
        super.onResume();
        // glesView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            // Call uninitialize() using reflection
            Class.forName(className).getMethod("uninitialize").invoke(glesView);
        } catch (Exception e) {
            Log.e("VND:", "Error during uninitialize", e);
        }
    }
}
