package com.swifttrack.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StationDao {

    @Query("SELECT * FROM stations ORDER BY name ASC")
    LiveData<List<StationEntity>> getAllStationsLiveData();

    @Query("SELECT * FROM stations ORDER BY name ASC")
    List<StationEntity> getAllStations();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<StationEntity> stations);
}
