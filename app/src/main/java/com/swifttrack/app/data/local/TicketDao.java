package com.swifttrack.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TicketDao {

    @Query("SELECT * FROM tickets ORDER BY cachedAt DESC")
    LiveData<List<TicketEntity>> getAllTicketsLiveData();

    @Query("SELECT * FROM tickets ORDER BY cachedAt DESC")
    List<TicketEntity> getAllTickets();

    @Query("SELECT * FROM tickets WHERE ticketCode = :code LIMIT 1")
    TicketEntity getTicketByCode(String code);

    @Query("SELECT * FROM tickets WHERE ticketId = :id LIMIT 1")
    TicketEntity getTicketById(String id);

    @Query("SELECT * FROM tickets WHERE ticketId = :id LIMIT 1")
    LiveData<TicketEntity> getTicketByIdLiveData(String id);

    @Query("UPDATE tickets SET status = :status WHERE ticketId = :id")
    void updateTicketStatus(String id, String status);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TicketEntity> tickets);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TicketEntity ticket);
}
