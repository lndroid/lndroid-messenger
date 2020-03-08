package org.lndroid.messenger;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

public class MessengerApplication extends Application {

    public static final String NEW_MESSAGE_CHANNEL_ID = "org.lndroid.messenger.notifications.NEW_MESSAGE";

    @Override
    public void onTerminate() {
        Log.i("MessengerApplication", "terminating");
        super.onTerminate();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("MessengerApplication", "starting");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel(
                    NEW_MESSAGE_CHANNEL_ID,
                    "New messages",
                    NotificationManager.IMPORTANCE_HIGH
            ));
        }

        // open the db
        Database.open(this);

        // start notifier
        Notifier.start(this);
    }

}
