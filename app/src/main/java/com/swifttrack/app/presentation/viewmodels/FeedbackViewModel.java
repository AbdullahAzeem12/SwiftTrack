package com.swifttrack.app.presentation.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.swifttrack.app.core.util.OperationErrorType;
import com.swifttrack.app.data.model.FeedbackItem;
import com.swifttrack.app.data.repository.FeedbackRepository;

public class FeedbackViewModel extends AndroidViewModel {

    public enum State { IDLE, VALIDATING, SUBMITTING, SUCCESS, ERROR }

    public static class FeedbackUiState {
        public final State state;
        public final String feedbackId;
        public final OperationErrorType errorType;
        public final String errorMessage;

        private FeedbackUiState(State state, String feedbackId, OperationErrorType errorType, String errorMessage) {
            this.state = state;
            this.feedbackId = feedbackId;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }

        public static FeedbackUiState idle() { return new FeedbackUiState(State.IDLE, null, null, null); }
        public static FeedbackUiState validating() { return new FeedbackUiState(State.VALIDATING, null, null, null); }
        public static FeedbackUiState submitting() { return new FeedbackUiState(State.SUBMITTING, null, null, null); }
        public static FeedbackUiState success(String feedbackId) { return new FeedbackUiState(State.SUCCESS, feedbackId, null, null); }
        public static FeedbackUiState error(OperationErrorType errorType, String msg) { return new FeedbackUiState(State.ERROR, null, errorType, msg); }
    }

    private final MutableLiveData<FeedbackUiState> uiState = new MutableLiveData<>(FeedbackUiState.idle());
    private final FeedbackRepository repository;

    public FeedbackViewModel(@NonNull Application application) {
        super(application);
        this.repository = FeedbackRepository.getInstance();
    }

    public LiveData<FeedbackUiState> getUiState() {
        return uiState;
    }

    public void submitFeedback(float rating, String category, String message, String recommend) {
        if (uiState.getValue() != null && uiState.getValue().state == State.SUBMITTING) {
            return; // Prevent duplicate rapid taps
        }

        uiState.setValue(FeedbackUiState.validating());

        if (rating <= 0) {
            uiState.setValue(FeedbackUiState.error(OperationErrorType.VALIDATION_ERROR, "Please select a rating star before submitting."));
            return;
        }

        if (category == null || category.trim().isEmpty()) {
            uiState.setValue(FeedbackUiState.error(OperationErrorType.VALIDATION_ERROR, "Please select a feedback category."));
            return;
        }

        uiState.setValue(FeedbackUiState.submitting());

        FeedbackItem item = new FeedbackItem(
                null, null, null, null,
                rating, category, message, recommend,
                "1.0", "Android", System.currentTimeMillis()
        );

        repository.submitFeedback(getApplication(), item, new FeedbackRepository.FeedbackCallback() {
            @Override
            public void onSuccess(String feedbackId) {
                uiState.postValue(FeedbackUiState.success(feedbackId));
            }

            @Override
            public void onError(OperationErrorType errorType, String userFriendlyMessage) {
                uiState.postValue(FeedbackUiState.error(errorType, userFriendlyMessage));
            }
        });
    }

    public void resetState() {
        uiState.setValue(FeedbackUiState.idle());
    }
}
