package org.lndroid.messenger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.common.collect.ImmutableList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.client.IPluginClient;
import org.lndroid.framework.client.IPluginTransaction;
import org.lndroid.framework.client.IPluginTransactionCallback;
import org.lndroid.framework.client.PluginClientBuilder;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.IPluginData;
import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.common.PluginData;
import org.lndroid.framework.defaults.DefaultPlugins;
import org.lndroid.framework.usecases.GetPaymentPeerContact;

import java.io.IOException;
import java.util.Stack;

public class Notifier {

    private static final String TAG = "Notifier";

    private static Notifier instance_;

    private Context ctx_;
    private Stack<Long> currentContacts_ = new Stack<>();
    private IPluginClient pluginClient_;
    private MutableLiveData<WalletService> walletService_ = new MutableLiveData<>();

    public static Notifier getInstance() { return instance_; }

    private Notifier(Context ctx) {
        ctx_ = ctx;

        walletService_.observeForever(new Observer<WalletService>() {
            @Override
            public void onChanged(WalletService walletService) {
                if (walletService != null)
                    connect();
                else
                    getWalletService(true);
            }
        });

        getWalletService(false);
    }

    private void getWalletService(final boolean later) {
        final Database db = Database.getInstance();
        db.execute(new Runnable() {
            @Override
            public void run() {
                if (later) {
                    try {
                        Thread.currentThread().sleep(1000);
                    } catch (InterruptedException e) {}
                }
                WalletService ws = db.walletServiceDao().getWalletService();
                walletService_.postValue(ws);
            }
        });
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
                Log.i(TAG, "client connection state "+connected);
                if (!connected) {
                    Log.i(TAG, "reconnecting");
                    pluginClient_.connect(ctx_);
                    start();
                }
            }

            @Override
            public void onError(String s, String s1) {
                Log.e(TAG, "client connection error "+s+" err "+s1);
            }
        });
        pluginClient_.connect(ctx_);

        start();
    }

    private void start() {

        IPluginTransaction tx = pluginClient_.createTransaction(
                DefaultPlugins.SUBSCRIBE_NEW_PAID_INVOICES, "notifier", new IPluginTransactionCallback() {
                    @Override
                    public void onResponse(IPluginData in) {
                        in.assignDataType(WalletData.Payment.class);
                        WalletData.Payment p = null;
                        try {
                            p = in.getData();
                        } catch (IOException e) {
                            // FIXME server version incompatible?
                            throw new RuntimeException(e);
                        }

                        Log.i(TAG, "new message "+p);
                        onNewMessage(p);
                    }

                    @Override
                    public void onAuth(WalletData.AuthRequest authRequest) {
                        throw new RuntimeException("Unexpected auth");
                    }

                    @Override
                    public void onAuthed(WalletData.AuthResponse authResponse) {
                        throw new RuntimeException("Unexpected auth response");
                    }

                    @Override
                    public void onError(String s, String s1) {
                        Log.e(TAG, "subscribe paid invoices error code "+s+" msg "+s1);
                        if (Errors.TX_TIMEOUT.equals(s)
                                || Errors.TX_INVALIDATE.equals(s)
                                || Errors.TX_DONE.equals(s))
                        {
                            start();
                        }
                    }
                });
        WalletData.SubscribeNewPaidInvoicesRequest req = WalletData.SubscribeNewPaidInvoicesRequest.builder()
                .setComponentPackageName(BuildConfig.APPLICATION_ID)
                .setComponentClassName(EventBroadcastReceiver.class.getName())
                .setNoAuth(true)
                .setProtocolExtension(WalletData.PROTOCOL_MESSAGES)
                .build();
        tx.start(req, WalletData.SubscribeNewPaidInvoicesRequest.class);
        tx.detach();
    }

    private void notifyMessage(final WalletData.Payment p, WalletData.Contact contact) {

        // this contact is current and thus user will see this message in the current dialog,
        // so no need to create a notification, but still need to mark message as notified
        if (currentContacts_.isEmpty() || !currentContacts_.peek().equals(contact.id())) {

            NotificationManager notificationManager = (NotificationManager)
                    ctx_.getSystemService(Context.NOTIFICATION_SERVICE);

            Intent intent = new Intent(ctx_, MessagesActivity.class);
            intent.putExtra(Constants.EXTRA_CONTACT_ID, contact.id());

            PendingIntent pendingIntent = PendingIntent.getActivity(ctx_, (int) contact.id(), intent, 0);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    ctx_, MessengerApplication.NEW_MESSAGE_CHANNEL_ID)
                    .setContentTitle(contact.name())
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    // NOTE: this is required!
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setContentText(p.message())
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            notificationManager.notify(0, builder.build());

            Log.i(TAG, "notification for ctx " + ctx_);
        }

        // mark as notified
        IPluginTransaction tx = pluginClient_.createTransaction(
                DefaultPlugins.SET_NOTIFIED_INVOICES, "", new IPluginTransactionCallback() {

            @Override
            public void onResponse(IPluginData r) {
                Log.i(TAG, "set notified invoices done for "+p.sourceId());
            }

            @Override
            public void onAuth(WalletData.AuthRequest r) {
                throw new RuntimeException("Unexpected auth");
            }

            @Override
            public void onAuthed(WalletData.AuthResponse r) {
                throw new RuntimeException("Unexpected authed");
            }

            @Override
            public void onError(String code, String message) {
                Log.e(TAG, "set notified invoices failed for "+p.sourceId()+" code "+code+" message "+message);
            }
        });

        // start
        ImmutableList.Builder<Long> ib = ImmutableList.builder();
        ib.add(p.sourceId());
        WalletData.NotifiedInvoicesRequest r = WalletData.NotifiedInvoicesRequest.builder()
                .setInvoiceIds(ib.build())
                .build();
        tx.start(r, WalletData.NotifiedInvoicesRequest.class);
        tx.detach();
    }

    private void onNewMessage(final WalletData.Payment p) {
        GetPaymentPeerContact getContact = new GetPaymentPeerContact(pluginClient_);

        getContact.setCallback(new IResponseCallback<WalletData.Contact>() {
            @Override
            public void onResponse(WalletData.Contact contact) {
                // FIXME anonym sending a message?
                if (contact == null)
                    return;

                Log.e(TAG, "got message from contact "+contact);
                notifyMessage(p, contact);
            }

            @Override
            public void onError(String s, String s1) {
                Log.e(TAG, "failed to get payment contact "+s+" msg "+s1);
            }
        });

        getContact.setRequest(WalletData.GetRequestLong.builder()
                .setNoAuth(true)
                .setId(p.id())
                .build());
        getContact.start();
        getContact.detach();
    }

    public static void start(Context ctx) {
        instance_ = new Notifier(ctx);
    }

    public void pushCurrentContact(long currentContact) {
        currentContacts_.push(currentContact);
    }

    public void popCurrentContact() {
        currentContacts_.pop();
    }

    public static class EventBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String pluginId = intent.getStringExtra(PluginData.BROADCAST_PLUGIN);
            String txId = intent.getStringExtra(PluginData.BROADCAST_PLUGIN);
            Log.i(TAG, "received broadcasted notification "+pluginId+" tx "+txId+" instance_ "+instance_);
            if (instance_ == null) {
                final PendingResult pendingResult = goAsync();
                Task asyncTask = new Task(pendingResult, context);
                asyncTask.execute();
            }
        }

        private static class Task extends AsyncTask<String, Integer, String> {

            private final PendingResult pendingResult_;
            private final Context ctx_;

            private Task(PendingResult pendingResult, Context ctx) {
                pendingResult_ = pendingResult;
                ctx_ = ctx;
            }

            @Override
            protected String doInBackground(String... strings) {
                Log.d(TAG, "Starting in goAsync");
                start(ctx_);
                return "";
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                // Must call finish() so the BroadcastReceiver can be recycled.
                pendingResult_.finish();
            }
        }
    }
}
