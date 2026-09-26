package com.swifttrack.app.presentation.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.swifttrack.app.data.repository.LiveTransitRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimetableViewModel extends AndroidViewModel {

    private final LiveTransitRepository repository;

    private final MutableLiveData<String> fromStationName = new MutableLiveData<>("Heathrow Airport");
    private final MutableLiveData<String> fromStationCode = new MutableLiveData<>("LHR");
    private final MutableLiveData<String> toStationName = new MutableLiveData<>("London Paddington");
    private final MutableLiveData<String> toStationCode = new MutableLiveData<>("PAD");

    private final MutableLiveData<String> selectedDateStr = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isNoticeBannerVisible = new MutableLiveData<>(true);

    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private Runnable autoRefreshRunnable;
    private static final long AUTO_REFRESH_INTERVAL_MS = 30000; // 30 seconds auto-refresh

    public TimetableViewModel(@NonNull Application application) {
        super(application);
        this.repository = LiveTransitRepository.getInstance(application);

        SimpleDateFormat sdf = new SimpleDateFormat("d MMM ''yy '(Today)'", Locale.getDefault());
        this.selectedDateStr.setValue(sdf.format(new Date()));

        fetchLiveTimetable(true);
        startAutoRefresh();
    }

    public LiveData<String> getFromStationName() { return fromStationName; }
    public LiveData<String> getToStationName() { return toStationName; }
    public LiveData<String> getSelectedDateStr() { return selectedDateStr; }
    public LiveData<List<LiveTransitRepository.LiveTimetableEntry>> getTimetableList() { return repository.getSharedTimetableList(); }
    public LiveData<Boolean> getIsLoading() { return repository.getIsSharedLoading(); }
    public LiveData<Boolean> getIsNoticeBannerVisible() { return isNoticeBannerVisible; }
    public LiveData<String> getLastUpdatedText() { return repository.getLastUpdatedText(); }

    public void setNoticeBannerVisible(boolean visible) {
        isNoticeBannerVisible.setValue(visible);
    }

    public void setSelectedDateStr(String dateStr) {
        selectedDateStr.setValue(dateStr);
        fetchLiveTimetable(true);
    }

    public void swapStations() {
        String curFrom = fromStationName.getValue();
        String curFromCode = fromStationCode.getValue();
        String curTo = toStationName.getValue();
        String curToCode = toStationCode.getValue();

        fromStationName.setValue(curTo);
        fromStationCode.setValue(curToCode);
        toStationName.setValue(curFrom);
        toStationCode.setValue(curFromCode);

        fetchLiveTimetable(true);
    }

    public void fetchLiveTimetable(boolean showLoading) {
        String fromCode = fromStationCode.getValue();
        String toCode = toStationCode.getValue();
        String dateIso = selectedDateStr.getValue();

        repository.refreshSharedLiveRailData(fromCode, toCode, dateIso, showLoading, null);
    }

    private void startAutoRefresh() {
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchLiveTimetable(false);
                autoRefreshHandler.postDelayed(this, AUTO_REFRESH_INTERVAL_MS);
            }
        };
        autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL_MS);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }
}