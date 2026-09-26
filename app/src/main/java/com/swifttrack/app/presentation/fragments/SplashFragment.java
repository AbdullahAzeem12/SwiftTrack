package com.swifttrack.app.presentation.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.swifttrack.app.R;
import com.swifttrack.app.databinding.FragmentSplashBinding;

public class SplashFragment extends Fragment {

    private FragmentSplashBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ValueAnimator progressAnimator;
    private boolean hasNavigated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSplashBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Enforce Cinematic Dark Mode Night Sky Experience Exclusively
        binding.auroraNightSkyView.setVisibility(View.VISIBLE);
        binding.auroraNightSkyView.startAnimation();
        binding.daySkyView.setVisibility(View.GONE);

        binding.tvLogo.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_amber_gold));
        binding.tvSubtitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary_white));
        binding.progressBar.setProgressDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_splash_progress_bar));

        // 10-Second (10,000ms) Smooth Progress Bar Loading Animation
        progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(10000); // Exactly 10 seconds
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            if (binding != null && binding.progressBar != null) {
                binding.progressBar.setProgress((int) animation.getAnimatedValue());
            }
        });
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                performNavigation();
            }
        });
        progressAnimator.start();
    }

    private void performNavigation() {
        if (hasNavigated || !isAdded() || getView() == null || getContext() == null) return;
        hasNavigated = true;

        // Stop animation loops cleanly before page transition
        if (binding != null) {
            if (binding.auroraNightSkyView != null) binding.auroraNightSkyView.stopAnimation();
            if (binding.daySkyView != null) binding.daySkyView.stopAnimation();
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("swifttrack_prefs", Context.MODE_PRIVATE);
        boolean onboardingSeen = prefs.getBoolean("onboarding_seen", false);
        com.swifttrack.app.core.security.KeystoreManager keystoreManager = new com.swifttrack.app.core.security.KeystoreManager(requireContext());
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        try {
            if (!onboardingSeen) {
                NavHostFragment.findNavController(SplashFragment.this).navigate(R.id.action_splash_to_onboarding);
            } else if (currentUser != null && keystoreManager.isBiometricEnabled() && com.swifttrack.app.core.security.BiometricAuthManager.isBiometricAvailable(requireContext())) {
                Bundle args = new Bundle();
                args.putBoolean("trigger_biometric", true);
                if (currentUser.getEmail() != null) {
                    args.putString("email", currentUser.getEmail());
                }
                NavHostFragment.findNavController(SplashFragment.this).navigate(R.id.action_splash_to_login, args);
            } else {
                NavHostFragment.findNavController(SplashFragment.this).navigate(R.id.action_splash_to_home);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        if (binding != null) {
            if (binding.auroraNightSkyView != null) binding.auroraNightSkyView.stopAnimation();
            if (binding.daySkyView != null) binding.daySkyView.stopAnimation();
        }
        binding = null;
    }
}
