package com.swifttrack.app.presentation.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.swifttrack.app.R;
import com.swifttrack.app.data.repository.LiveTransitRepository;
import com.swifttrack.app.databinding.ItemTimetableRowBinding;

import java.util.ArrayList;
import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder> {

    private final List<LiveTransitRepository.LiveTimetableEntry> items = new ArrayList<>();

    public void setItems(List<LiveTransitRepository.LiveTimetableEntry> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimetableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTimetableRowBinding binding = ItemTimetableRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new TimetableViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TimetableViewHolder holder, int position) {
        holder.bind(items.get(position));

        // Pop in & Fade in Entrance Animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setScaleX(0.92f);
        holder.itemView.setScaleY(0.92f);
        holder.itemView.setTranslationY(40f);

        long startDelay = (position % 8) * 45L;
        holder.itemView.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .translationY(0f)
                .setDuration(320)
                .setStartDelay(startDelay)
                .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                .start();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TimetableViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimetableRowBinding binding;

        public TimetableViewHolder(@NonNull ItemTimetableRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(LiveTransitRepository.LiveTimetableEntry item) {
            binding.tvStop1Time.setText(item.stop1Time);
            binding.tvStop2Time.setText(item.stop2Time);
            binding.tvStop3Time.setText(item.stop3Time);

            binding.tvPlatformBadge.setText(item.platform);
            binding.tvOperatorName.setText(item.operatorName + " • " + item.duration);
            binding.tvStatusBadge.setText(item.status);

            Context context = itemView.getContext();

            if (item.isCancelled) {
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.chip_cancelled_text));
                binding.tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.chip_cancelled_bg)));
                binding.tvStop1Time.setPaintFlags(binding.tvStop1Time.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvStop2Time.setPaintFlags(binding.tvStop2Time.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvStop3Time.setPaintFlags(binding.tvStop3Time.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (item.status != null && item.status.contains("DELAYED")) {
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.chip_delayed_text));
                binding.tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.chip_delayed_bg)));
                binding.tvStop1Time.setPaintFlags(binding.tvStop1Time.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                binding.tvStop2Time.setPaintFlags(binding.tvStop2Time.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                binding.tvStop3Time.setPaintFlags(binding.tvStop3Time.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            } else {
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.chip_on_time_text));
                binding.tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.chip_on_time_bg)));
                binding.tvStop1Time.setPaintFlags(binding.tvStop1Time.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                binding.tvStop2Time.setPaintFlags(binding.tvStop2Time.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                binding.tvStop3Time.setPaintFlags(binding.tvStop3Time.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }
        }
    }
}
