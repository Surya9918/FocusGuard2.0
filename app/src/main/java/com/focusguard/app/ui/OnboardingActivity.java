package com.focusguard.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.focusguard.app.databinding.ActivityOnboardingBinding;
import com.focusguard.app.service.TrackingState;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowPadding.apply(binding.getRoot());

        binding.btnGrantUsage.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        });

        binding.btnGrantOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        binding.btnGetStarted.setOnClickListener(v -> {
            TrackingState.preferences(this).edit().putBoolean("onboarding_complete", true).apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

}
