package org.lndroid.messenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.usecases.IRequestFactory;

public class ContactsPrivilegeActivity extends AppCompatActivity {

    private static final String TAG = "ContactsPrivActivity";
    private ContactsPrivilegeViewModel model_;
    private TextView state_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts_privilege);

        state_ = findViewById(R.id.state);

        Button b = findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addListContactsPrivilege();
            }
        });

        model_ = ViewModelProviders.of(this).get(ContactsPrivilegeViewModel.class);

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
        model_.addListContactsPrivilege().setRequestFactory(this, new IRequestFactory<WalletData.ListContactsPrivilege>() {
            @Override
            public WalletData.ListContactsPrivilege create() {
                return WalletData.ListContactsPrivilege.builder().build();
            }
        });
        model_.addListContactsPrivilege().setCallback(this, new IResponseCallback<WalletData.ListContactsPrivilege>() {
            @Override
            public void onResponse(WalletData.ListContactsPrivilege r) {
                Log.i(TAG, "ListContactsPrivilege "+r);
                state_.setText("Added privilege");
                finish();
            }

            @Override
            public void onError(String code, String e) {
                Log.i(TAG, "ListContactsPrivilege error "+code+" e "+e);
                state_.setText("Error: "+e);
            }
        });
        model_.addListContactsPrivilege().setAuthCallback(this, new IResponseCallback<WalletData.AuthRequest>() {
            @Override
            public void onResponse(WalletData.AuthRequest r) {
                startAuthActivity(r);
            }

            @Override
            public void onError(String code, String e) {
                Log.i(TAG, "ListContactsPrivilege auth error "+code+" e "+e);
                state_.setText("Error: "+e);
            }
        });
    }


    private void recoverUseCases() {
        if (model_.addListContactsPrivilege().isExecuting())
            addListContactsPrivilege();
    }

    private void addListContactsPrivilege() {
        state_.setText("Adding privilege...");
        if (model_.addListContactsPrivilege().isExecuting())
            model_.addListContactsPrivilege().recover();
        else
            model_.addListContactsPrivilege().execute("");
    }

    private void startAuthActivity(WalletData.AuthRequest r) {
        ComponentName comp = new ComponentName(r.componentPackageName(), r.componentClassName());
        Intent intent = new Intent();
        intent.setComponent(comp);
        intent.putExtra(Constants.EXTRA_AUTH_REQUEST_ID, r.id());
        startActivity(intent);
    }

}
