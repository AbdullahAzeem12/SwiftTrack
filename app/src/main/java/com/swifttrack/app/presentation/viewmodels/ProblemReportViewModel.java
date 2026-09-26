package com.swifttrack.app.presentation.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.swifttrack.app.core.util.OperationErrorType;
import com.swifttrack.app.data.model.ProblemReport;
import com.swifttrack.app.data.repository.ProblemReportRepository;

public class ProblemReportViewModel extends AndroidViewModel {

    public enum State { IDLE, VALIDATING, SUBMITTING, SUCCESS, ERROR }

    public static class ProblemReportUiState {
        public final State state;
        public final String reportId;
        public final OperationErrorType errorType;
        public final String errorMessage;

        private ProblemReportUiState(State state, String reportId, OperationErrorType errorType, String errorMessage) {
            this.state = state;
            this.reportId = reportId;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }

        public static ProblemReportUiState idle() { return new ProblemReportUiState(State.IDLE, null, null, null); }
        public static ProblemReportUiState validating() { return new ProblemReportUiState(State.VALIDATING, null, null, null); }
        public static ProblemReportUiState submitting() { return new ProblemReportUiState(State.SUBMITTING, null, null, null); }
        public static ProblemReportUiState success(String reportId) { return new ProblemReportUiState(State.SUCCESS, reportId, null, null); }
        public static ProblemReportUiState error(OperationErrorType errorType, String msg) { return new ProblemReportUiState(State.ERROR, null, errorType, msg); }
    }

    private final MutableLiveData<ProblemReportUiState> uiState = new MutableLiveData<>(ProblemReportUiState.idle());
    private final ProblemReportRepository repository;

    public ProblemReportViewModel(@NonNull Application application) {
        super(application);
        this.repository = ProblemReportRepository.getInstance();
    }

    public LiveData<ProblemReportUiState> getUiState() {
        return uiState;
    }

    public void submitProblemReport(String category, String subject, String description, String screenName, String bookingRef) {
        if (uiState.getValue() != null && uiState.getValue().state == State.SUBMITTING) {
            return; // Prevent duplicate rapid taps
        }

        uiState.setValue(ProblemReportUiState.validating());

        if (category == null || category.trim().isEmpty()) {
            uiState.setValue(ProblemReportUiState.error(OperationErrorType.VALIDATION_ERROR, "Please select a problem category."));
            return;
        }

        if (subject == null || subject.trim().isEmpty()) {
            uiState.setValue(ProblemReportUiState.error(OperationErrorType.VALIDATION_ERROR, "Please enter a subject summary."));
            return;
        }

        if (description == null || description.trim().isEmpty()) {
            uiState.setValue(ProblemReportUiState.error(OperationErrorType.VALIDATION_ERROR, "Please describe the problem you encountered."));
            return;
        }

        uiState.setValue(ProblemReportUiState.submitting());

        ProblemReport report = new ProblemReport(
                null, null, null, null,
                category, subject, description, screenName,
                bookingRef, "1.0", null, null, System.currentTimeMillis()
        );

        repository.submitProblemReport(getApplication(), report, new ProblemReportRepository.ProblemReportCallback() {
            @Override
            public void onSuccess(String reportId) {
                uiState.postValue(ProblemReportUiState.success(reportId));
            }

            @Override
            public void onError(OperationErrorType errorType, String userFriendlyMessage) {
                uiState.postValue(ProblemReportUiState.error(errorType, userFriendlyMessage));
            }
        });
    }

    public void resetState() {
        uiState.setValue(ProblemReportUiState.idle());
    }
}
