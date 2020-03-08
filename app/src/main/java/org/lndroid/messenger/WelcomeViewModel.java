package org.lndroid.messenger;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class WelcomeViewModel extends ViewModel {

    private static final String TAG = "WelcomeViewModel";

    private WalletServiceDao walletServiceDao_;
    private MutableLiveData<WalletService> walletService_ = new MutableLiveData<>();

    public WelcomeViewModel() {
        super();

        Database db = Database.getInstance();
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
