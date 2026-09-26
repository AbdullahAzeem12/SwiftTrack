package com.swifttrack.app.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {StationEntity.class, TicketEntity.class, JourneyEntity.class, AlertEntity.class}, version = 3, exportSchema = false)
public abstract class SwiftTrackDatabase extends RoomDatabase {

    private static volatile SwiftTrackDatabase INSTANCE;

    public abstract StationDao stationDao();
    public abstract TicketDao ticketDao();
    public abstract JourneyDao journeyDao();
    public abstract AlertDao alertDao();

    public static SwiftTrackDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SwiftTrackDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SwiftTrackDatabase.class,
                            "swifttrack.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
