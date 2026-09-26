package com.swifttrack.app.presentation.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.core.util.OperationErrorType;
import com.swifttrack.app.data.model.SupportTicket;
import com.swifttrack.app.data.repository.SupportRepository;

import java.util.ArrayList;
import java.util.List;

public class SupportViewModel extends AndroidViewModel {

    public enum State { IDLE, VALIDATING, SUBMITTING, SUCCESS, ERROR }

    public static class SupportUiState {
        public final State state;
        public final String ticketReference;
        public final OperationErrorType errorType;
        public final String errorMessage;

        private SupportUiState(State state, String ticketReference, OperationErrorType errorType, String errorMessage) {
            this.state = state;
            this.ticketReference = ticketReference;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }

        public static SupportUiState idle() { return new SupportUiState(State.IDLE, null, null, null); }
        public static SupportUiState validating() { return new SupportUiState(State.VALIDATING, null, null, null); }
        public static SupportUiState submitting() { return new SupportUiState(State.SUBMITTING, null, null, null); }
        public static SupportUiState success(String ticketRef) { return new SupportUiState(State.SUCCESS, ticketRef, null, null); }
        public static SupportUiState error(OperationErrorType errorType, String msg) { return new SupportUiState(State.ERROR, null, errorType, msg); }
    }

    private final MutableLiveData<SupportUiState> uiState = new MutableLiveData<>(SupportUiState.idle());
    private final MutableLiveData<List<SupportTicket>> userTickets = new MutableLiveData<>(new ArrayList<>());
    private final SupportRepository repository;
    private ListenerRegistration ticketsListenerRegistration;

    public SupportViewModel(@NonNull Application application) {
        super(application);
        this.repository = SupportRepository.getInstance();
    }

    public LiveData<SupportUiState> getUiState() {
        return uiState;
    }

    public LiveData<List<SupportTicket>> getUserTickets() {
        return userTickets;
    }

    public void submitSupportTicket(String category, String subject, String description, String bookingRef) {
        if (uiState.getValue() != null && uiState.getValue().state == State.SUBMITTING) {
            return; // Prevent duplicate rapid taps
        }

        uiState.setValue(SupportUiState.validating());

        if (category == null || category.trim().isEmpty()) {
            uiState.setValue(SupportUiState.error(OperationErrorType.VALIDATION_ERROR, "Please select a support category."));
            return;
        }

        if (subject == null || subject.trim().isEmpty()) {
            uiState.setValue(SupportUiState.error(OperationErrorType.VALIDATION_ERROR, "Please enter a subject for your enquiry."));
            return;
        }

        if (description == null || description.trim().isEmpty()) {
            uiState.setValue(SupportUiState.error(OperationErrorType.VALIDATION_ERROR, "Please provide a description of your issue."));
            return;
        }

        uiState.setValue(SupportUiState.submitting());

        SupportTicket ticket = new SupportTicket(
                null, null, null, null,
                category, subject, description, bookingRef,
                "1.0", "Android", System.currentTimeMillis()
        );

        repository.createSupportTicket(getApplication(), ticket, new SupportRepository.SupportTicketCallback() {
            @Override
            public void onSuccess(String ticketReference) {
                uiState.postValue(SupportUiState.success(ticketReference));
            }

            @Override
            public void onError(OperationErrorType errorType, String userFriendlyMessage) {
                uiState.postValue(SupportUiState.error(errorType, userFriendlyMessage));
            }
        });
    }

    public void startObservingUserTickets() {
        if (ticketsListenerRegistration != null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        KeystoreManager keystoreManager = new KeystoreManager(getApplication());
        String uid = user != null ? user.getUid() : keystoreManager.getUserUid();

        ticketsListenerRegistration = repository.observeUserTickets(uid, new SupportRepository.TicketsListener() {
            @Override
            public void onTicketsUpdated(List<SupportTicket> tickets) {
                userTickets.postValue(tickets);
            }

            @Override
            public void onError(OperationErrorType errorType, String userFriendlyMessage) {
                // Keep existing tickets on error
            }
        });
    }

    public void resetState() {
        uiState.setValue(SupportUiState.idle());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (ticketsListenerRegistration != null) {
            ticketsListenerRegistration.remove();
            ticketsListenerRegistration = null;
        }
    }
}
