package com.focusguard.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.focusguard.app.R;

/**
 * BlockScreenActivity — THE MOST CRITICAL ACTIVITY.
 * ══════════════════════════════════════════════════════════════════
 * This is the full-screen overlay that covers distraction apps.
 */
public class BlockScreenActivity extends AppCompatActivity {

    private String mBlockedPackage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make it appear over the lock screen and turn the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        setContentView(R.layout.layout_block_screen);

        mBlockedPackage = getIntent().getStringExtra("package_name");

        TextView titleText = findViewById(R.id.tv_block_title);
        TextView subtitleText = findViewById(R.id.tv_block_subtitle);
        TextView appNameText = findViewById(R.id.tv_blocked_app_name);
        android.widget.ImageView appIcon = findViewById(R.id.img_blocked_app_icon);
        Button closeButton = findViewById(R.id.btn_close_app);
        Button snoozeButton = findViewById(R.id.btn_snooze);

        if (mBlockedPackage != null) {
            appNameText.setText(mBlockedPackage);
            // In a real app, we would load the app icon here
        }

        closeButton.setOnClickListener(v -> {
            // Navigate to home screen
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            finish();
        });

        snoozeButton.setOnClickListener(v -> {
            // Close the block screen
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Disable back button to prevent easy bypass
        // User must click one of the buttons
    }
}
