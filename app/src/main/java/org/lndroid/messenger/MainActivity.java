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
import android.widget.Toast;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.usecases.IRequestFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private MainViewModel model_;
    private TextView state_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        model_ = ViewModelProviders.of(this).get(MainViewModel.class);

        state_ = findViewById(R.id.state);

        Button b = findViewById(R.id.addContact);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addContact();
            }
        });
        b = findViewById(R.id.shareContact);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareContact();
            }
        });

        model_.ready().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean ready) {
                if (ready == null)
                    return;

                if (ready) {
                    state_.setText("");
                    initUseCases();
                    recoverUseCases();
                } else {
                    state_.setText("Please connect app to the wallet");
                }
            }
        });
    }

    private void initUseCases() {
        model_.addAppContact().setRequestFactory(this, new IRequestFactory<WalletData.AddAppContactRequest>() {
            @Override
            public WalletData.AddAppContactRequest create() {
                return WalletData.AddAppContactRequest.builder().build();
            }
        });
        model_.addAppContact().setCallback(this, new IResponseCallback<WalletData.Contact>() {
            @Override
            public void onResponse(WalletData.Contact r) {
                Log.i(TAG, "contact "+r);
                Toast.makeText(MainActivity.this, "Added contact", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String code, String e) {
                Log.i(TAG, "contact error "+code+" e "+e);
                state_.setText("Error "+e);
            }
        });
        model_.addAppContact().setAuthCallback(this, new IResponseCallback<WalletData.AuthRequest>() {
            @Override
            public void onResponse(WalletData.AuthRequest r) {
                startAuthActivity(r);
            }

            @Override
            public void onError(String code, String e) {
                throw new RuntimeException("Unexpected auth request error");
            }
        });

        model_.shareContact().setRequestFactory(this, new IRequestFactory<WalletData.ShareContactRequest>() {
            @Override
            public WalletData.ShareContactRequest create() {
                return WalletData.ShareContactRequest.builder().build();
            }
        });
        model_.shareContact().setCallback(this, new IResponseCallback<WalletData.ShareContactResponse>() {
            @Override
            public void onResponse(WalletData.ShareContactResponse r) {
                Log.i(TAG, "shared "+r);
                Toast.makeText(MainActivity.this, "Shared contact", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String code, String e) {
                Log.i(TAG, "contact error "+code+" e "+e);
                state_.setText("Error "+e);
            }
        });
        model_.shareContact().setAuthCallback(this, new IResponseCallback<WalletData.AuthRequest>() {
            @Override
            public void onResponse(WalletData.AuthRequest r) {
                startAuthActivity(r);
            }

            @Override
            public void onError(String code, String e) {
                throw new RuntimeException("Unexpected auth request error");
            }
        });

        // set contact list request
        WalletData.ListContactsRequest listContactsRequest = WalletData.ListContactsRequest.builder()
                .setPage(WalletData.ListPage.builder().setCount(10).build())
                .setSort("name")
                .setEnablePaging(true)
                .build();
        model_.setListContactsRequest(listContactsRequest);

        // create list view adapter
        final ContactListView.Adapter adapter = new ContactListView.Adapter();
        adapter.setEmptyView(findViewById(R.id.notFound));

        // subscribe adapter to model list updates
        model_.contactList().observe(this, new Observer<PagedList<WalletData.Contact>>() {
            @Override
            public void onChanged(PagedList<WalletData.Contact> contacts) {
                adapter.submitList(contacts);
            }
        });

        // set adapter to list view, init list layout
        final RecyclerView listView = findViewById(R.id.contacts);
        listView.setAdapter(adapter);
        listView.setLayoutManager(new LinearLayoutManager(this));

        // set click listener to open contact details
        adapter.setItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContactListView.Holder viewHolder =
                        (ContactListView.Holder)listView.findContainingViewHolder(view);
                WalletData.Contact c = viewHolder.contact();
                Log.i(TAG, "click on c "+c);
                if (c == null)
                    return;

                showMessages(c.id());
            }
        });

        model_.contactListError().observe(this, new Observer<WalletData.Error>() {
            @Override
            public void onChanged(WalletData.Error error) {
                if (error.code().equals(Errors.FORBIDDEN))
                    showGetContactsPrivilege();
                else if (error.code().equals(Errors.UNKNOWN_CALLER))
                    showWalletConnect();
                else if (!error.code().equals(Errors.TX_INVALIDATE))
                    state_.setText("Error: "+error.message());
            }
        });

        model_.clientError().observe(this, new Observer<WalletData.Error>() {
            @Override
            public void onChanged(WalletData.Error error) {
                if (error != null && Errors.IPC_IDENTITY_ERROR.equals(error.code())) {
                    showConnect();
                    finish();
                }
            }
        });
    }

    private void recoverUseCases() {
        if (model_.addAppContact().isExecuting())
            addContact();
        if (model_.shareContact().isExecuting())
            shareContact();
    }

    private void addContact() {
        state_.setText("Adding contact...");
        if (model_.addAppContact().isExecuting())
            model_.addAppContact().recover();
        else
            model_.addAppContact().execute("");
    }

    private void shareContact() {
        state_.setText("Sharing contact...");
        if (model_.shareContact().isExecuting())
            model_.shareContact().recover();
        else
            model_.shareContact().execute("");
    }

    private void startAuthActivity(WalletData.AuthRequest r) {
        ComponentName comp = new ComponentName(r.componentPackageName(), r.componentClassName());
        Intent intent = new Intent();
        intent.setComponent(comp);
        intent.putExtra(Constants.EXTRA_AUTH_REQUEST_ID, r.id());
        startActivity(intent);
    }

    private void showGetContactsPrivilege() {
        Intent intent = new Intent(this, ContactsPrivilegeActivity.class);
        startActivity(intent);
    }

    private void showWalletConnect() {
        Intent intent = new Intent(this, WalletConnectActivity.class);
        startActivity(intent);
    }

    private void showMessages(long contactId) {
        Intent intent = new Intent(this, MessagesActivity.class);
        intent.putExtra(Constants.EXTRA_CONTACT_ID, contactId);
        startActivity(intent);
    }

    private void showConnect() {
        Intent intent = new Intent(this, WalletConnectActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Boolean.TRUE.equals(model_.ready().getValue()) && model_.contactListError().getValue() != null) {
            model_.contactListRefresh();
        }
    }

}
