package com.swifttrack.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface JourneyDao {

    @Query("SELECT * FROM journeys")
    LiveData<List<JourneyEntity>> getAllJourneysLiveData();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<JourneyEntity> journeys);
}
