package com.focusguard.app.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.TrackedApp;
import com.focusguard.app.databinding.ActivityAppSelectorBinding;
import com.focusguard.app.ml.AppIdMapper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.focusguard.app.databinding.LayoutAppPickerSheetBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppSelectorActivity extends AppCompatActivity {

    private ActivityAppSelectorBinding binding;
    private AppSelectorAdapter adapter;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppSelectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        observeTrackedApps();

        binding.fabAddApp.setOnClickListener(v -> showAppPicker());
    }

    private void setupRecyclerView() {
        adapter = new AppSelectorAdapter((packageName, isMonitored) -> {
            executor.execute(() -> {
                AppDatabase.getInstance(this).trackedAppDao().setMonitored(packageName, isMonitored);
            });
        });
        binding.recyclerTrackedApps.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTrackedApps.setAdapter(adapter);
    }

    private void observeTrackedApps() {
        AppDatabase.getInstance(this).trackedAppDao().getAllTrackedApps().observe(this, apps -> {
            if (apps == null || apps.isEmpty()) {
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerTrackedApps.setVisibility(View.GONE);
                binding.chipMonitoredCount.setVisibility(View.GONE);
            } else {
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.recyclerTrackedApps.setVisibility(View.VISIBLE);
                binding.chipMonitoredCount.setVisibility(View.VISIBLE);
                int count = apps.size();
                String countText = getResources().getQuantityString(R.plurals.focus_breakers_monitored_count_plural, count, count);
                binding.chipMonitoredCount.setText(countText);
                adapter.submitList(apps);
            }
        });
    }

    private void showAppPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        LayoutAppPickerSheetBinding pickerBinding = LayoutAppPickerSheetBinding.inflate(getLayoutInflater());
        dialog.setContentView(pickerBinding.getRoot());

        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<TrackedApp> installedApps = new ArrayList<>();
            for (ApplicationInfo appInfo : packages) {
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    installedApps.add(new TrackedApp(appInfo.packageName, 
                            appInfo.loadLabel(pm).toString(), 
                            AppIdMapper.categorize(appInfo.packageName)));
                }
            }

            runOnUiThread(() -> {
                AppSelectorAdapter pickerAdapter = new AppSelectorAdapter((packageName, isMonitored) -> {
                    executor.execute(() -> {
                        TrackedApp appToAdd = installedApps.stream()
                                .filter(a -> a.packageName.equals(packageName))
                                .findFirst()
                                .orElse(null);
                        if (appToAdd != null) {
                            AppDatabase.getInstance(this).trackedAppDao().insertOrUpdate(appToAdd);
                        }
                    });
                    dialog.dismiss();
                });
                pickerBinding.recyclerAppPicker.setLayoutManager(new LinearLayoutManager(this));
                pickerBinding.recyclerAppPicker.setAdapter(pickerAdapter);
                pickerAdapter.submitList(installedApps);
            });
        });

        dialog.show();
    }
}
