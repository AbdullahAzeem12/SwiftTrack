package com.swifttrack.app.presentation.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.swifttrack.app.R;
import com.swifttrack.app.core.security.BiometricAuthManager;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.core.util.ButterClickEffect;
import com.swifttrack.app.databinding.FragmentAccountBinding;
import com.swifttrack.app.presentation.dialogs.AboutSwiftTrackBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.AccountSecurityBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.ContactSupportBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.FaqsBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.LiveTravelBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.MySupportRequestsBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.MyTravelBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.NotificationsBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.ReportProblemBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.RewardsBottomSheetDialogFragment;
import com.swifttrack.app.presentation.dialogs.SubmitFeedbackBottomSheetDialogFragment;
import com.swifttrack.app.presentation.viewmodels.AuthViewModel;
import com.swifttrack.app.presentation.viewmodels.MoreAccountViewModel;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private KeystoreManager keystoreManager;
    private MoreAccountViewModel moreAccountViewModel;
    private com.google.firebase.firestore.ListenerRegistration userSnapshotListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        keystoreManager = new KeystoreManager(requireContext());
        moreAccountViewModel = new ViewModelProvider(this).get(MoreAccountViewModel.class);

        applyMicroAnimations();
        setupHubDataObservation();
        setupExpandableTiles();
        setupQuickActions();
        setupBiometricControl();
        setupSettingsControls();
        setupSupportAndFeedbackActions();
        setupNavigationAndLogout();
    }

    private void applyMicroAnimations() {
        if (binding == null) return;
        ButterClickEffect.applyAll(
                binding.cardHeaderGreeting,
                binding.btnQuickTickets,
                binding.btnQuickTimetable,
                binding.btnQuickAlerts,
                binding.headerAccountSecurity,
                binding.headerMyTravel,
                binding.headerLiveTravel,
                binding.headerRewards,
                binding.headerNotifications,
                binding.headerSettings,
                binding.headerHelpSupport,
                binding.headerAbout,
                binding.btnSubProfile,
                binding.btnSubActiveSession,
                binding.btnSubUpcomingJourneys,
                binding.btnSubPastJourneys,
                binding.btnSubSavedRoutes,
                binding.btnSubLiveStatus,
                binding.btnSubServiceAlerts,
                binding.btnSubRewardsBalance,
                binding.btnSubPromoCodes,
                binding.btnSubViewNotifications,
                binding.btnSubLanguage,
                binding.btnSubClearCache,
                binding.btnHelpSupport,
                binding.btnSubContactSupport,
                binding.btnSubMySupportRequests,
                binding.btnSubFeedback,
                binding.btnSubAboutApp,
                binding.btnSubTermsPrivacy,
                binding.btnLogout
        );
    }

    private void setupHubDataObservation() {
        moreAccountViewModel.getHubState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || binding == null) return;

            binding.tvUserName.setText(state.userName);
            binding.tvUserEmail.setText(state.userEmail != null && !state.userEmail.isEmpty() ? state.userEmail : "user@swifttrack.com");

            binding.tvSubAccountSecurity.setText("Signed in as " + state.userName);
            binding.tvSubMyTravel.setText(state.upcomingJourneyCount + (state.upcomingJourneyCount == 1 ? " upcoming journey" : " upcoming journeys"));
            binding.tvBadgeMyTravel.setVisibility(state.upcomingJourneyCount > 0 ? View.VISIBLE : View.GONE);
            binding.tvBadgeMyTravel.setText(state.upcomingJourneyCount + " UPCOMING");

            binding.tvSubLiveTravel.setText(state.liveStatusText);
            binding.tvLiveTimestamp.setText(state.lastApiTimestamp);

            binding.tvSubRewards.setText(state.rewardPoints + " points • " + state.availableOffersCount + " available offers");
            binding.tvSubNotifications.setText(state.unreadNotificationCount + (state.unreadNotificationCount == 1 ? " unread alert" : " unread alerts"));
            binding.tvBadgeNotifications.setVisibility(state.unreadNotificationCount > 0 ? View.VISIBLE : View.GONE);
            binding.tvBadgeNotifications.setText(state.unreadNotificationCount + " NEW");

            binding.tvSubAbout.setText("Version " + state.appVersion + " • App Information");

            binding.tvChipJourneys.setText("🎫 " + state.upcomingJourneyCount + " Journeys");
            binding.tvChipAlerts.setText("🔔 " + state.unreadNotificationCount + " Alerts");
            binding.tvChipRewards.setText("⭐ " + state.rewardPoints + " Pts");

            binding.switchBiometric.setChecked(state.isBiometricEnabled);
            if (binding.switchDarkMode.isChecked() != state.isDarkMode) {
                binding.switchDarkMode.setChecked(state.isDarkMode);
            }
        });

        // Observe Room DB Local Tickets for live ticket counts
        moreAccountViewModel.getLocalTicketsLiveData().observe(getViewLifecycleOwner(), tickets -> {
            if (tickets != null) {
                int upcoming = 0;
                for (var t : tickets) {
                    if ("ACTIVE".equalsIgnoreCase(t.status) || "VALID".equalsIgnoreCase(t.status)) {
                        upcoming++;
                    }
                }
                moreAccountViewModel.updateUpcomingCount(upcoming);
            }
        });
    }

    private void setupExpandableTiles() {
        binding.headerAccountSecurity.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            toggleTileExpansion(binding.layoutChildrenAccountSecurity, binding.ivChevronAccountSecurity);
        });

        binding.headerMyTravel.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            toggleTileExpansion(binding.layoutChildrenMyTravel, binding.ivChevronMyTravel);
        });

        binding.headerLiveTravel.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            toggleTileExpansion(binding.layoutChildrenLiveTravel, binding.ivChevronLiveTravel);
        });

        binding.headerRewards.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            toggleTileExpansion(binding.layoutChildrenRewards, binding.ivChevronRewards);
        });

        binding.headerNotifications.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            toggleTileExpansion(binding.layoutChildrenNotifications, binding.ivChevronNotifications);
        });

        binding.headerSettings.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            toggleTileExpansion(binding.layoutChildrenSettings, binding.ivChevronSettings);
        });

        binding.headerHelpSupport.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            toggleTileExpansion(binding.layoutChildrenHelp, binding.ivChevronHelp);
        });

        binding.headerAbout.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            toggleTileExpansion(binding.layoutChildrenAbout, binding.ivChevronAbout);
        });
    }

    private void toggleTileExpansion(LinearLayout childrenContainer, ImageView chevronImageView) {
        boolean isExpanded = childrenContainer.getVisibility() == View.VISIBLE;
        if (isExpanded) {
            chevronImageView.animate().rotation(0f).setDuration(200).setInterpolator(new OvershootInterpolator(1.5f)).start();
            childrenContainer.animate()
                    .alpha(0.0f)
                    .setDuration(160)
                    .withEndAction(() -> childrenContainer.setVisibility(View.GONE))
                    .start();
        } else {
            childrenContainer.setAlpha(0.0f);
            childrenContainer.setVisibility(View.VISIBLE);
            childrenContainer.animate()
                    .alpha(1.0f)
                    .setDuration(200)
                    .start();
            chevronImageView.animate().rotation(180f).setDuration(220).setInterpolator(new OvershootInterpolator(1.8f)).start();
        }
    }

    private void setupQuickActions() {
        binding.btnQuickTickets.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            MyTravelBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "my_travel");
        });
        binding.btnQuickTimetable.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            LiveTravelBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "live_travel");
        });
        binding.btnQuickAlerts.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            NotificationsBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "notifications");
        });
    }

    private void setupBiometricControl() {
        binding.switchBiometric.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            boolean isChecked = binding.switchBiometric.isChecked();
            if (isChecked) {
                BiometricAuthManager.BiometricStatus status = BiometricAuthManager.getBiometricStatus(requireContext());
                if (status != BiometricAuthManager.BiometricStatus.AVAILABLE) {
                    binding.switchBiometric.setChecked(false);
                    String errorMsg;
                    switch (status) {
                        case NO_HARDWARE:
                        case UNSUPPORTED:
                            errorMsg = "⚠️ Device does not support biometric authentication";
                            break;
                        case HARDWARE_UNAVAILABLE:
                            errorMsg = "⚠️ Biometric sensor is temporarily unavailable";
                            break;
                        case NONE_ENROLLED:
                            errorMsg = "⚠️ No fingerprint or face enrolled. Please enable in Android Settings";
                            break;
                        default:
                            errorMsg = "⚠️ Biometric authentication unavailable";
                            break;
                    }
                    if (getView() != null) {
                        AlertUtil.showErrorSnackbar(getView(), errorMsg);
                    }
                    return;
                }

                BiometricAuthManager.showBiometricPrompt(
                        AccountFragment.this,
                        "Enable Biometric Re-entry",
                        "Scan your fingerprint or face to verify your identity",
                        "Cancel",
                        new BiometricAuthManager.BiometricAuthCallback() {
                            @Override
                            public void onSuccess(@NonNull BiometricPrompt.AuthenticationResult result) {
                                if (!isAdded() || binding == null) return;
                                BiometricAuthManager.generateBiometricKey();
                                keystoreManager.setBiometricEnabled(true);
                                binding.switchBiometric.setChecked(true);
                                if (getView() != null) {
                                    AlertUtil.showSuccessSnackbar(getView(), "🔒 Biometric Re-entry Enabled");
                                }
                            }

                            @Override
                            public void onError(int errorCode, @NonNull CharSequence errString) {
                                if (!isAdded() || binding == null) return;
                                binding.switchBiometric.setChecked(false);
                                keystoreManager.setBiometricEnabled(false);
                                if (getView() != null && errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                    AlertUtil.showErrorSnackbar(getView(), "Biometric setup failed: " + errString);
                                }
                            }

                            @Override
                            public void onFailed() {
                                if (!isAdded() || binding == null) return;
                                binding.switchBiometric.setChecked(false);
                                keystoreManager.setBiometricEnabled(false);
                                if (getView() != null) {
                                    AlertUtil.showErrorSnackbar(getView(), "Biometric authentication failed");
                                }
                            }
                        }
                );
            } else {
                BiometricAuthManager.removeBiometricKey();
                keystoreManager.setBiometricEnabled(false);
                binding.switchBiometric.setChecked(false);
                if (getView() != null) {
                    AlertUtil.showInfoSnackbar(getView(), "🔓 Biometric Re-entry Disabled");
                }
            }
        });
    }

    private void setupSettingsControls() {
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (getView() != null) {
                String msg = isChecked ? "🔔 Service Disruption Alerts Enabled" : "🔕 Service Disruption Alerts Disabled";
                AlertUtil.showInfoSnackbar(getView(), msg);
            }
        });

        boolean isDarkModeActive = keystoreManager.isDarkMode(requireContext());
        binding.switchDarkMode.setChecked(isDarkModeActive);
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            int currentMode = keystoreManager.getNightMode();
            int newMode = isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;

            if (currentMode == newMode && androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() == newMode) {
                return;
            }

            keystoreManager.setNightMode(newMode);
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(newMode);
            if (getView() != null) {
                String msg = isChecked ? "🌙 Dark Mode Theme Activated" : "☀️ Light Mode Theme Activated";
                AlertUtil.showSuccessSnackbar(getView(), msg);
            }
        });

        binding.btnSubClearCache.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            try {
                Context ctx = requireContext();
                ctx.getCacheDir().delete();
                if (getView() != null) AlertUtil.showSuccessSnackbar(getView(), "🧹 App cache cleared successfully.");
            } catch (Exception e) {
                if (getView() != null) AlertUtil.showInfoSnackbar(getView(), "Cache cleared.");
            }
        });

        binding.btnSubLanguage.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (getView() != null) {
                AlertUtil.showInfoSnackbar(getView(), "🌐 Language set to English (United Kingdom)");
            }
        });
    }

    private void setupSupportAndFeedbackActions() {
        binding.btnHelpSupport.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            FaqsBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "faqs");
        });

        binding.btnSubContactSupport.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            ContactSupportBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "contact_support");
        });

        binding.btnSubMySupportRequests.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            MySupportRequestsBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "my_support_requests");
        });

        binding.btnSubReportProblem.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            ReportProblemBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "report_problem");
        });

        binding.btnSubFeedback.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            SubmitFeedbackBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "submit_feedback");
        });

        binding.btnSubProfile.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            AccountSecurityBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "account_security");
        });

        binding.btnSubActiveSession.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            AccountSecurityBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "account_security");
        });

        binding.btnSubRewardsBalance.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            RewardsBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "rewards");
        });

        binding.btnSubPromoCodes.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            RewardsBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "rewards");
        });

        binding.btnSubAboutApp.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            AboutSwiftTrackBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "about_swifttrack");
        });

        binding.btnSubTermsPrivacy.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            AboutSwiftTrackBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "about_swifttrack");
        });
    }

    private void setupNavigationAndLogout() {
        binding.btnSubUpcomingJourneys.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            MyTravelBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "my_travel");
        });

        binding.btnSubPastJourneys.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            MyTravelBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "my_travel");
        });

        binding.btnSubSavedRoutes.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            MyTravelBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "my_travel");
        });

        binding.btnSubLiveStatus.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            LiveTravelBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "live_travel");
        });

        binding.btnSubServiceAlerts.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            LiveTravelBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "live_travel");
        });

        binding.btnSubViewNotifications.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            NotificationsBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "notifications");
        });

        AuthViewModel authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding.btnLogout.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

            String userEmail = keystoreManager.getUserEmail();
            FirebaseUser fUserLogout = FirebaseAuth.getInstance().getCurrentUser();
            if ((userEmail == null || userEmail.isEmpty()) && fUserLogout != null && fUserLogout.getEmail() != null) {
                userEmail = fUserLogout.getEmail();
            }

            authViewModel.logout();
            keystoreManager.clearAuth();

            Bundle args = new Bundle();
            if (userEmail != null && !userEmail.isEmpty()) {
                args.putString("email", userEmail);
            }

            if (getActivity() != null && getActivity().findViewById(android.R.id.content) != null) {
                AlertUtil.showSuccessSnackbar(getActivity().findViewById(android.R.id.content), "🚪 Logged out successfully");
            }

            try {
                androidx.navigation.fragment.NavHostFragment.findNavController(AccountFragment.this)
                        .navigate(R.id.action_account_to_login, args);
            } catch (Exception e) {
                try {
                    Navigation.findNavController(v).navigate(R.id.action_account_to_login, args);
                } catch (Exception ignored) {}
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (moreAccountViewModel != null) {
            moreAccountViewModel.loadStateData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userSnapshotListener != null) {
            userSnapshotListener.remove();
            userSnapshotListener = null;
        }
        binding = null;
    }
}
