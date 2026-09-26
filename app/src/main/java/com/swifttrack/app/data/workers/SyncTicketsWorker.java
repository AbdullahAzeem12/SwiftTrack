package com.swifttrack.app.data.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.swifttrack.app.data.repository.TicketRepository;

public class SyncTicketsWorker extends Worker {

    public SyncTicketsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            TicketRepository repository = new TicketRepository(getApplicationContext());
            repository.refreshTickets();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
