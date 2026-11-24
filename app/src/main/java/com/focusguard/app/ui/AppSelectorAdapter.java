package com.focusguard.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.focusguard.app.data.TrackedApp;
import com.focusguard.app.databinding.ItemAppCardBinding;

import java.util.function.BiConsumer;

public class AppSelectorAdapter extends ListAdapter<TrackedApp, AppSelectorAdapter.ViewHolder> {

    private final BiConsumer<String, Boolean> onMonitoredToggled;

    public AppSelectorAdapter(BiConsumer<String, Boolean> onMonitoredToggled) {
        super(DIFF_CALLBACK);
        this.onMonitoredToggled = onMonitoredToggled;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppCardBinding binding = ItemAppCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppCardBinding binding;

        ViewHolder(ItemAppCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TrackedApp app) {
            binding.tvAppName.setText(app.appName);
            binding.switchMonitored.setOnCheckedChangeListener(null);
            binding.switchMonitored.setChecked(app.isMonitored);
            binding.chipRiskLevel.setVisibility(android.view.View.GONE);
            try {
                binding.imgAppIcon.setImageDrawable(binding.getRoot().getContext().getPackageManager()
                        .getApplicationIcon(app.packageName));
            } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
                binding.imgAppIcon.setImageResource(com.focusguard.app.R.drawable.ic_app_placeholder);
            }
            
            binding.switchMonitored.setOnCheckedChangeListener((buttonView, isChecked) -> {
                onMonitoredToggled.accept(app.packageName, isChecked);
            });
        }
    }

    private static final DiffUtil.ItemCallback<TrackedApp> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TrackedApp>() {
                @Override
                public boolean areItemsTheSame(@NonNull TrackedApp oldItem, @NonNull TrackedApp newItem) {
                    return oldItem.packageName.equals(newItem.packageName);
                }

                @Override
                public boolean areContentsTheSame(@NonNull TrackedApp oldItem, @NonNull TrackedApp newItem) {
                    return oldItem.isMonitored == newItem.isMonitored &&
                            java.util.Objects.equals(oldItem.appName, newItem.appName);
                }
            };
}
