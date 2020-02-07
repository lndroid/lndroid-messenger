package org.lndroid.messenger;

import android.content.Context;
import android.os.AsyncTask;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.Executor;

@androidx.room.Database(version = 2, exportSchema = true, entities = {
        WalletService.class,
})

abstract class Database extends RoomDatabase {

    abstract WalletServiceDao walletServiceDao();

    static Database open(Context ctx) {
        return Room.databaseBuilder(ctx, Database.class, Constants.DB_NAME)
                // FIXME implement migrations after the first release
                .fallbackToDestructiveMigration()
                .build();
    }

    void execute(Runnable r) {
        AsyncTask.execute(r);
    }
}
