package org.lndroid.messenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.usecases.IRequestFactory;

public class PaymentsPrivilegeActivity extends AppCompatActivity {

    private static final String TAG = "PaymentsPrivActivity";
    private PaymentsPrivilegeViewModel model_;
    private TextView state_;
    private long contactId_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments_privilege);

        state_ = findViewById(R.id.state);

        Intent intent = getIntent();
        contactId_ = intent.getLongExtra(Constants.EXTRA_CONTACT_ID, 0);
        if (contactId_ == 0) {
            state_.setText("Contact id not specified");
        }

        Button b = findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addContactPaymentsPrivilege();
            }
        });

        model_ = ViewModelProviders.of(this).get(PaymentsPrivilegeViewModel.class);

        model_.ready().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean ready) {
                if (ready == null)
                    return;

                if (ready) {
                    initUseCases();
                    recoverUseCases();
                } else {
                    state_.setText("Please connect app to the wallet");
                }
            }
        });
    }

    private void initUseCases() {
        if (contactId_ == 0)
            return;

        model_.addContactPaymentsPrivilege().setRequestFactory(this, new IRequestFactory<WalletData.ContactPaymentsPrivilege>() {
            @Override
            public WalletData.ContactPaymentsPrivilege create() {
                return WalletData.ContactPaymentsPrivilege.builder()
                        .setContactId(contactId_)
                        .build();
            }
        });
        model_.addContactPaymentsPrivilege().setCallback(this, new IResponseCallback<WalletData.ContactPaymentsPrivilege>() {
            @Override
            public void onResponse(WalletData.ContactPaymentsPrivilege r) {
                Log.i(TAG, "ContactPaymentsPrivilege "+r);
                state_.setText("Added privilege "+r);
                showMessages();
                finish();
            }

            @Override
            public void onError(String code, String e) {
                Log.i(TAG, "ContactPaymentsPrivilege error "+code+" e "+e);
                state_.setText("Error: "+e);
            }
        });
        model_.addContactPaymentsPrivilege().setAuthCallback(this, new IResponseCallback<WalletData.AuthRequest>() {
            @Override
            public void onResponse(WalletData.AuthRequest r) {
                startAuthActivity(r);
            }

            @Override
            public void onError(String code, String e) {
                Log.i(TAG, "ContactPaymentsPrivilege auth error "+code+" e "+e);
                state_.setText("Error: "+e);
            }
        });
    }


    private void recoverUseCases() {
        if (model_.addContactPaymentsPrivilege().isExecuting())
            addContactPaymentsPrivilege();
    }

    private void addContactPaymentsPrivilege() {
        state_.setText("Adding privilege...");
        if (model_.addContactPaymentsPrivilege().isExecuting())
            model_.addContactPaymentsPrivilege().recover();
        else
            model_.addContactPaymentsPrivilege().execute("");
    }

    private void startAuthActivity(WalletData.AuthRequest r) {
        ComponentName comp = new ComponentName(r.componentPackageName(), r.componentClassName());
        Intent intent = new Intent();
        intent.setComponent(comp);
        intent.putExtra(Constants.EXTRA_AUTH_REQUEST_ID, r.id());
        startActivity(intent);
    }

    private void showMessages() {
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra(Constants.EXTRA_CONTACT_ID, contactId_);
        startActivity(intent);
    }

}
