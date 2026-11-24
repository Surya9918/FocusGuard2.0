package com.focusguard.app.ui;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.focusguard.app.R;
import com.focusguard.app.data.AppDatabase;
import com.focusguard.app.data.TrackedApp;
import com.focusguard.app.databinding.ActivityAppSelectorBinding;
import com.focusguard.app.ml.AppIdMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppSelectorActivity extends AppCompatActivity {

    private ActivityAppSelectorBinding binding;
    private AppSelectorAdapter adapter;
    private final java.util.Set<String> selected = new java.util.HashSet<>();
    private boolean loadingPicker;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppSelectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowPadding.apply(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(view -> finish());

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
            selected.clear();
            if (apps != null) {
                for (TrackedApp app : apps) selected.add(app.packageName);
            }
            adapter.submitList(apps);
            if (apps == null || apps.isEmpty()) {
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerTrackedApps.setVisibility(View.GONE);
                binding.chipMonitoredCount.setVisibility(View.GONE);
            } else {
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.recyclerTrackedApps.setVisibility(View.VISIBLE);
                binding.chipMonitoredCount.setVisibility(View.VISIBLE);
                int count = (int) apps.stream().filter(app -> app.isMonitored).count();
                String countText = getResources().getQuantityString(R.plurals.focus_breakers_monitored_count_plural, count, count);
                binding.chipMonitoredCount.setText(countText);
            }
        });
    }

    private void showAppPicker() {
        if (loadingPicker) return;
        loadingPicker = true;
        java.util.Set<String> excluded = new java.util.HashSet<>(selected);
        excluded.add(getPackageName());
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<TrackedApp> available = new ArrayList<>();
            Intent launchable = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo match : pm.queryIntentActivities(launchable, 0)) {
                String name = match.activityInfo.packageName;
                if (excluded.add(name)) {
                    available.add(new TrackedApp(name, match.loadLabel(pm).toString(),
                            AppIdMapper.categorize(name)));
                }
            }
            var sorter = java.text.Collator.getInstance();
            available.sort((first, second) -> sorter.compare(first.appName, second.appName));
            runOnUiThread(() -> {
                loadingPicker = false;
                if (isFinishing() || isDestroyed()) return;
                AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.picker_title)
                        .setNegativeButton(android.R.string.cancel, null);
                if (available.isEmpty()) dialog.setMessage(R.string.tracking_select_empty);
                else dialog.setItems(available.stream().map(app -> app.appName).toArray(String[]::new),
                        (view, index) -> executor.execute(() -> AppDatabase.getInstance(this)
                                .trackedAppDao().insertOrUpdate(available.get(index))));
                dialog.show();
            });
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}
