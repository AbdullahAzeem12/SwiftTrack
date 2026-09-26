package com.swifttrack.app.presentation.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.data.local.TicketEntity;
import com.swifttrack.app.data.repository.LiveTransitRepository;
import com.swifttrack.app.data.repository.NotificationRepository;
import com.swifttrack.app.data.repository.RewardsRepository;
import com.swifttrack.app.data.repository.TicketRepository;

import java.util.List;

public class MoreAccountViewModel extends AndroidViewModel {

    public static class AccountHubState {
        public String userName = "Guest User";
        public String userEmail = "";
        public int upcomingJourneyCount = 0;
        public String liveStatusText = "✓ Services running normally";
        public String lastApiTimestamp = "Updated live";
        public int rewardPoints = 250;
        public int availableOffersCount = 3;
        public int unreadNotificationCount = 0;
        public boolean isBiometricEnabled = false;
        public boolean isDarkMode = false;
        public String appVersion = "1.0";
    }

    private final MutableLiveData<AccountHubState> hubState = new MutableLiveData<>(new AccountHubState());
    private final KeystoreManager keystoreManager;
    private final RewardsRepository rewardsRepository;
    private final NotificationRepository notificationRepository;
    private final LiveTransitRepository liveTransitRepository;
    private final TicketRepository ticketRepository;

    public MoreAccountViewModel(@NonNull Application application) {
        super(application);
        this.keystoreManager = new KeystoreManager(application);
        this.rewardsRepository = RewardsRepository.getInstance(application);
        this.notificationRepository = NotificationRepository.getInstance();
        this.liveTransitRepository = new LiveTransitRepository(application);
        this.ticketRepository = new TicketRepository(application);

        loadStateData();
        setupRewardSummaryObservation();
    }

    public LiveData<AccountHubState> getHubState() {
        return hubState;
    }

    private void setupRewardSummaryObservation() {
        rewardsRepository.getRewardSummaryLiveData().observeForever(summary -> {
            if (summary != null) {
                AccountHubState curr = hubState.getValue();
                if (curr != null) {
                    curr.rewardPoints = summary.getPointsBalance();
                    hubState.postValue(curr);
                }
            }
        });
    }

    public void loadStateData() {
        AccountHubState state = hubState.getValue();
        if (state == null) state = new AccountHubState();

        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser != null) {
            state.userEmail = fUser.getEmail() != null ? fUser.getEmail() : keystoreManager.getUserEmail();
            state.userName = fUser.getDisplayName() != null && !fUser.getDisplayName().isEmpty() ?
                    fUser.getDisplayName() : (keystoreManager.getUserName() != null && !"Guest User".equals(keystoreManager.getUserName()) ?
                    keystoreManager.getUserName() : formatEmailToName(state.userEmail));
        } else {
            state.userName = keystoreManager.getUserName();
            state.userEmail = keystoreManager.getUserEmail();
        }

        String userId = rewardsRepository.resolveActiveUserId();
        state.rewardPoints = rewardsRepository.getAvailablePoints(userId);
        state.availableOffersCount = 3; // SWIFTTRACK20, Save 30%, Member Perk

        state.unreadNotificationCount = notificationRepository.getUnreadCount(getApplication());
        state.isBiometricEnabled = keystoreManager.isBiometricEnabled();
        state.isDarkMode = keystoreManager.isDarkMode(getApplication());

        try {
            state.appVersion = getApplication().getPackageManager()
                    .getPackageInfo(getApplication().getPackageName(), 0).versionName;
        } catch (Exception e) {
            state.appVersion = "1.0";
        }

        hubState.setValue(state);

        // Fetch Live Departure / Status async from live railway API
        liveTransitRepository.fetchLiveDepartureApiAsync("PAD", "HWV", new LiveTransitRepository.LiveApiCallback<>() {
            @Override
            public void onSuccess(LiveTransitRepository.LiveDepartureInfo result) {
                AccountHubState curr = hubState.getValue();
                if (curr != null && result != null) {
                    curr.liveStatusText = result.status != null ? result.status : "✓ Services running normally";
                    curr.lastApiTimestamp = result.lastUpdatedTimestamp != null ? result.lastUpdatedTimestamp : "Updated live";
                    hubState.postValue(curr);
                }
            }

            @Override
            public void onError(Exception e) {
                AccountHubState curr = hubState.getValue();
                if (curr != null) {
                    curr.liveStatusText = "Live travel info unavailable";
                    hubState.postValue(curr);
                }
            }
        });
    }

    public LiveData<List<TicketEntity>> getLocalTicketsLiveData() {
        return ticketRepository.getLocalTicketsLiveData();
    }

    public void updateUpcomingCount(int count) {
        AccountHubState curr = hubState.getValue();
        if (curr != null) {
            curr.upcomingJourneyCount = count;
            hubState.setValue(curr);
        }
    }

    private String formatEmailToName(String email) {
        if (email == null || !email.contains("@")) return "User";
        String namePart = email.split("@")[0].replaceAll("[0-9]", " ").trim();
        if (namePart.isEmpty()) return "User";
        String[] words = namePart.split("[._\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) sb.append(w.substring(1));
            }
        }
        return sb.length() > 0 ? sb.toString() : "User";
    }
}
