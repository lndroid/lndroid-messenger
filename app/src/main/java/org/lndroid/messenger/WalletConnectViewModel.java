package org.lndroid.messenger;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class WalletConnectViewModel extends AndroidViewModel {
    private static final String TAG = "WalletConnectViewModel";

    private static final String SERVICE_CONNECT_ACTION_BITCOIN_TESTNET = "org.lndroid.actions.bitcoin.testnet.APP_CONNECT";

    private static final String EXTRA_SERVICE_CLASSNAME = "org.lndroid.extra.SERVICE_CLASSNAME";
    private static final String EXTRA_SERVICE_PACKAGENAME = "org.lndroid.extra.SERVICE_PACKAGENAME";
    private static final String EXTRA_SERVICE_PUBKEY = "org.lndroid.extra.SERVICE_PUBKEY";
    private static final String EXTRA_APP_PUBKEY = "org.lndroid.extra.APP_PUBKEY";

    private Context ctx_;
    private Database db_;
    private WalletServiceDao walletServiceDao_;
    private MutableLiveData<WalletService> walletService_ = new MutableLiveData<>();

    public WalletConnectViewModel(Application app) {
        super(app);

        ctx_ = app;

        db_ = Database.getInstance();

        walletServiceDao_ = db_.walletServiceDao();
    }

    Intent createGetWalletServiceIntent() {
        Intent intent = new Intent(SERVICE_CONNECT_ACTION_BITCOIN_TESTNET);
        intent.putExtra(EXTRA_APP_PUBKEY, WalletKeyStore.getInstance().getAppPubkey());
        return intent;
    }

    void setWalletService(Intent data) {

        String serviceClassName = data.getStringExtra(EXTRA_SERVICE_CLASSNAME);
        String servicePackageName = data.getStringExtra(EXTRA_SERVICE_PACKAGENAME);
        String servicePubkey = data.getStringExtra(EXTRA_SERVICE_PUBKEY);
        Log.i(TAG, "app connect ok class "+serviceClassName+" pk "+servicePubkey);

        WalletService walletService = new WalletService();
        walletService.pubkey = servicePubkey;
        walletService.className = serviceClassName;
        walletService.packageName = servicePackageName;

        // connect will be called, as it's observing this live data
        walletService_.setValue(walletService);

        // write to db
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                walletServiceDao_.setWalletService(walletService_.getValue());
            }
        });
    }
}

