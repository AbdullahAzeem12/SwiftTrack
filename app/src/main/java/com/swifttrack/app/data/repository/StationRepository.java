package com.swifttrack.app.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.swifttrack.app.data.local.StationDao;
import com.swifttrack.app.data.local.StationEntity;
import com.swifttrack.app.data.local.SwiftTrackDatabase;
import com.swifttrack.app.data.remote.ApiClient;
import com.swifttrack.app.data.remote.SwiftTrackApi;
import com.swifttrack.app.data.remote.dto.StationDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class StationRepository {

    private final SwiftTrackApi api;
    private final StationDao stationDao;

    public StationRepository(Context context) {
        this.api = ApiClient.getInstance(context);
        this.stationDao = SwiftTrackDatabase.getInstance(context).stationDao();
    }

    public LiveData<List<StationEntity>> getLocalStationsLiveData() {
        return stationDao.getAllStationsLiveData();
    }

    public void refreshStations() {
        api.getAllStations().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<StationDto>> call, Response<List<StationDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<StationEntity> entities = new ArrayList<>();
                    for (StationDto dto : response.body()) {
                        entities.add(new StationEntity(
                                dto.getId().toString(),
                                dto.getCode(),
                                dto.getName(),
                                dto.getCity(),
                                dto.isAirportTerminal(),
                                dto.getTerminalCode(),
                                dto.getLatitude(),
                                dto.getLongitude()
                        ));
                    }
                    Executors.newSingleThreadExecutor().execute(() -> stationDao.insertAll(entities));
                }
            }

            @Override
            public void onFailure(Call<List<StationDto>> call, Throwable t) {
                // Keep offline cached stations
            }
        });
    }
}
