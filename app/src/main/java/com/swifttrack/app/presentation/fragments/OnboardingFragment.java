package com.swifttrack.app.presentation.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import com.swifttrack.app.R;
import com.swifttrack.app.databinding.FragmentOnboardingBinding;

public class OnboardingFragment extends Fragment {

    private FragmentOnboardingBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String[] titles = {
                "Express Airport Booking",
                "Instant Mobile Tickets",
                "Live Rail Alerts"
        };
        String[] descs = {
                "Book non-stop trains between London terminals and airport hubs in seconds.",
                "Access secure QR barcodes offline directly on your phone without network dependency.",
                "Stay informed with real-time platform updates, live delays, and service notifications."
        };
        int[] icons = {
                android.R.drawable.ic_dialog_info,
                android.R.drawable.ic_menu_agenda,
                android.R.drawable.ic_dialog_alert
        };

        binding.viewPager.setAdapter(new RecyclerView.Adapter<OnboardingViewHolder>() {
            @NonNull
            @Override
            public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View slide = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_slide, parent, false);
                return new OnboardingViewHolder(slide);
            }

            @Override
            public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
                holder.tvTitle.setText(titles[position]);
                holder.tvDescription.setText(descs[position]);
                holder.ivIcon.setImageResource(icons[position]);
            }

            @Override
            public int getItemCount() { return 3; }
        });

        binding.btnSkip.setOnClickListener(v -> finishOnboarding());
        binding.btnNext.setOnClickListener(v -> {
            int current = binding.viewPager.getCurrentItem();
            if (current < 2) {
                binding.viewPager.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });
    }

    private void finishOnboarding() {
        if (!isAdded() || getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("swifttrack_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_seen", true).apply();
        try {
            NavHostFragment.findNavController(this).navigate(R.id.action_onboarding_to_home);
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription;
        ImageView ivIcon;

        OnboardingViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_slide_title);
            tvDescription = itemView.findViewById(R.id.tv_slide_description);
            ivIcon = itemView.findViewById(R.id.iv_slide_icon);
        }
    }
}
