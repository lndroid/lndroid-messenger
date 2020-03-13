package org.lndroid.messenger;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.client.PluginClientBuilder;
import org.lndroid.framework.common.IResponseCallback;

public class WalletViewModelBase extends AndroidViewModel {

    private static final String TAG = "WalletViewModelBase";

    private Context ctx_;
    private Database db_;
    private WalletServiceDao walletServiceDao_;
    private MutableLiveData<WalletService> walletService_ = new MutableLiveData<>();
    private MutableLiveData<Boolean> ready_ = new MutableLiveData<>();
    private MutableLiveData<WalletData.Error> clientError_ = new MutableLiveData<>();
    private IPluginClient pluginClient_;

    protected Database db() {
        return db_;
    }

    protected WalletServiceDao walletServiceDao() {
        return walletServiceDao_;
    }

    protected IPluginClient pluginClient() {
        return pluginClient_;
    }

    protected void onConnect() {
        // init use cases in subclasses
    }

    public WalletViewModelBase(Application app) {
        super(app);

        ctx_ = app;

        db_ = Database.getInstance();

        walletServiceDao_ = db_.walletServiceDao();

        walletService_.observeForever(new Observer<WalletService>() {
            @Override
            public void onChanged(WalletService walletService) {
                if (ready_.getValue() == null) {
                    if (walletService != null)
                        connect();
                    ready_.setValue(walletService != null);
                }
            }
        });

        db_.execute(new Runnable() {
            @Override
            public void run() {
                WalletService ws = walletServiceDao_.getWalletService();
                walletService_.postValue(ws);
            }
        });
    }

    @Override
    protected void onCleared() {
        pluginClient_.disconnect(ctx_);
        super.onCleared();
    }

    private void connect() {
        WalletService ws = walletService_.getValue();

        pluginClient_ = new PluginClientBuilder()
                .setIpc(true)
                .setUserIdentity(WalletData.UserIdentity.builder()
                        .setAppPubkey(WalletKeyStore.getInstance().getAppPubkey())
                        .setAppPackageName(Constants.APP_PACKAGE_NAME)
                        .build())
                .setServicePackageName(ws.packageName)
                .setServiceClassName(ws.className)
                .setServicePubkey(ws.pubkey)
                .setSigner(WalletKeyStore.getInstance().getSigner())
                .build();
        pluginClient_.setOnConnection(new IResponseCallback<Boolean>() {
            @Override
            public void onResponse(Boolean connected) {
                Log.i(TAG, "connection state "+connected);
                if (!connected) {
                    Log.i(TAG, "reconnecting");
                    Toast.makeText(ctx_, "Reconnecting to wallet", Toast.LENGTH_SHORT).show();
                    pluginClient_.connect(ctx_);
                }
            }

            @Override
            public void onError(String s, String s1) {
                Log.e(TAG, "plugin client connection error "+s+" err "+s1);
                Toast.makeText(ctx_, "Client error: "+s1, Toast.LENGTH_LONG).show();
            }
        });
        pluginClient_.setOnError(new IResponseCallback<WalletData.Error>() {
            @Override
            public void onResponse(WalletData.Error error) {
                clientError_.setValue(error);
            }

            @Override
            public void onError(String s, String s1) {
                clientError_.setValue(WalletData.Error.builder().setCode(s).setMessage(s1).build());
            }
        });

        pluginClient_.connect(ctx_);
        onConnect();
    }

    LiveData<Boolean> ready() {
        return ready_;
    }
    LiveData<WalletData.Error> clientError() { return clientError_; }
}



