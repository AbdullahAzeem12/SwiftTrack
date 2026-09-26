package com.swifttrack.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AlertDao {

    @Query("SELECT * FROM alerts WHERE isActive = 1")
    LiveData<List<AlertEntity>> getActiveAlertsLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AlertEntity> alerts);
}
