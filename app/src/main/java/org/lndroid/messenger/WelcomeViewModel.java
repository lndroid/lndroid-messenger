package org.lndroid.messenger;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;

public class WelcomeViewModel extends AndroidViewModel {

    private static final String TAG = "WelcomeViewModel";

    private WalletServiceDao walletServiceDao_;
    private MutableLiveData<WalletService> walletService_ = new MutableLiveData<>();

    public WelcomeViewModel(Application app) {
        super(app);

        Database db = Database.open(app);
        walletServiceDao_ = db.walletServiceDao();

        db.execute(new Runnable() {
            @Override
            public void run() {
                WalletService ws = walletServiceDao_.getWalletService();
                walletService_.postValue(ws);
            }
        });
    }

    LiveData<WalletService> walletService() {
        return walletService_;
    }

}
