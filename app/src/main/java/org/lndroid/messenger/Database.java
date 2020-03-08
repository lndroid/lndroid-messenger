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

    private static Database instance_ = null;

    static void open(Context ctx) {
        instance_ = Room.databaseBuilder(ctx, Database.class, Constants.DB_NAME)
                // FIXME implement migrations after the first release
                .fallbackToDestructiveMigration()
                .build();
    }

    static Database getInstance() { return instance_; }

    abstract WalletServiceDao walletServiceDao();

    void execute(Runnable r) {
        AsyncTask.execute(r);
    }
}
