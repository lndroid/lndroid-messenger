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
import android.widget.EditText;
import android.widget.TextView;

import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.usecases.IRequestFactory;

public class MessagesActivity extends AppCompatActivity {

    private static final String TAG = "MessagesActivity";

    private MessagesViewModel model_;
    private RecyclerView messages_;
    private TextView state_;
    private EditText message_;
    private EditText amount_;
    private long contactId_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        model_ = ViewModelProviders.of(this).get(MessagesViewModel.class);

        messages_ = findViewById(R.id.messages);
        message_ = findViewById(R.id.message);
        amount_ = findViewById(R.id.amount);
        state_ = findViewById(R.id.state);
        state_.setText("Please wait...");

        Intent intent = getIntent();
        contactId_ = intent.getLongExtra(Constants.EXTRA_CONTACT_ID, 0);
        if (contactId_ == 0) {
            state_.setText("Contact id not specified");
        }

        Button b = findViewById(R.id.send);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
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
        model_.sendPayment().setRequestFactory(this, new IRequestFactory<WalletData.SendPaymentRequest>() {
            @Override
            public WalletData.SendPaymentRequest create() {
                String message = message_.getText().toString();
                if (message.length() > Constants.MAX_MESSAGE_LENGTH) {
                    state_.setText("Max message size: "+Constants.MAX_MESSAGE_LENGTH
                            +", your message: "+message.length());
                    message_.setEnabled(true);
                    return null;
                }
                if (message.isEmpty()) {
                    state_.setText("Specify a message");
                    message_.setEnabled(true);
                    return null;
                }

                long amount = 0;
                try {
                    amount = Long.parseLong(amount_.getText().toString());
                } catch (NumberFormatException e) {
                    state_.setText("Bad amount: "+e);
                    message_.setEnabled(true);
                    return null;
                }

                if (amount == 0) {
                    state_.setText("Empty amount");
                    message_.setEnabled(true);
                    return null;
                }

                if (amount > Constants.MAX_AMOUNT) {
                    state_.setText("Amount too high, max: "+Constants.MAX_AMOUNT);
                    message_.setEnabled(true);
                    return null;
                }

                return WalletData.SendPaymentRequest.builder()
                        .setContactId(contactId_)
                        .setMessage(message)
                        .setValueSat(amount)
                        .setIncludeSenderPubkey(true)
                        .setIsKeysend(true)
                        .setMaxTries(100)
                        .setExpiry(System.currentTimeMillis() + 3600 * 1000) // 1h
                        .setNoAuth(true)
                        .build();
            }
        });
        model_.sendPayment().setCallback(this, new IResponseCallback<WalletData.SendPayment>() {
            @Override
            public void onResponse(WalletData.SendPayment r) {
                Log.i(TAG, "sent "+r);
                state_.setText("");
                message_.setText("");
                message_.setEnabled(true);
                messages_.scrollToPosition(0);
            }

            @Override
            public void onError(String code, String e) {
                Log.i(TAG, "send error "+code+" e "+e);
                message_.setEnabled(true);
                if (code.equals(Errors.FORBIDDEN)) {
                    showGetPaymentsPrivilege();
                } else {
                    state_.setText("Error: " + e);
                }
            }
        });
        model_.sendPayment().setAuthCallback(this, new IResponseCallback<WalletData.AuthRequest>() {
            @Override
            public void onResponse(WalletData.AuthRequest r) {
//                startAuthActivity(r);
                // wtf? we shouldn't get to auth w/ noAuth=true
                model_.sendPayment().destroy();
                showGetPaymentsPrivilege();
            }

            @Override
            public void onError(String code, String e) {
                throw new RuntimeException("Unexpected auth request error");
            }
        });

        // set contact list request
        WalletData.ListPaymentsRequest listRequest = WalletData.ListPaymentsRequest.builder()
                .setPage(WalletData.ListPage.builder().setCount(50).build())
                .setSort("time") // order by time
                .setSortDesc(true) // reverse chrono order
                .setContactId(contactId_)
                .setEnablePaging(true)
                .build();
        model_.setListPaymentsRequest(listRequest);

        // create list view adapter
        final PaymentListView.Adapter adapter = new PaymentListView.Adapter();

        // subscribe adapter to model list updates
        model_.paymentList().observe(this, new Observer<PagedList<WalletData.Payment>>() {
            @Override
            public void onChanged(PagedList<WalletData.Payment> payments) {
                Log.i(TAG, "list "+payments.size());
                adapter.submitList(payments);
            }
        });

        // set adapter to list view
        messages_.setAdapter(adapter);

        // set message layout manager, make it reverse the order
        // of messages so that most recent are at the bottom
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setReverseLayout(true);
        messages_.setLayoutManager(lm);

        // set click listener to open contact details
        adapter.setItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PaymentListView.Holder viewHolder = (PaymentListView.Holder)messages_.findContainingViewHolder(view);
                WalletData.Payment p = viewHolder.payment();
                Log.i(TAG, "click on "+p);
                if (p == null)
                    return;

                // FIXME select,copy,etc
            }
        });

        model_.paymentListError().observe(this, new Observer<WalletData.Error>() {
            @Override
            public void onChanged(WalletData.Error error) {
                if (error.code().equals(Errors.FORBIDDEN))
                    showGetPaymentsPrivilege();
                else if (!error.code().equals(Errors.TX_INVALIDATE))
                    state_.setText("Error: "+error.message());
            }
        });
    }

    private void recoverUseCases() {
        if (model_.sendPayment().isExecuting())
            sendMessage();
    }

    private void sendMessage() {
        state_.setText("Sending...");
        message_.setEnabled(false);
        if (model_.sendPayment().isExecuting())
            model_.sendPayment().recover();
        else
            model_.sendPayment().execute("");
    }

    private void startAuthActivity(WalletData.AuthRequest r) {
        ComponentName comp = new ComponentName(r.componentPackageName(), r.componentClassName());
        Intent intent = new Intent();
        intent.setComponent(comp);
        intent.putExtra(Constants.EXTRA_AUTH_REQUEST_ID, r.id());
        startActivity(intent);
    }

    private void showGetPaymentsPrivilege() {
        Intent intent = new Intent(this, PaymentsPrivilegeActivity.class);
        intent.putExtra(Constants.EXTRA_CONTACT_ID, contactId_);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Boolean.TRUE.equals(model_.ready().getValue()) && model_.paymentListError().getValue() != null) {
            model_.paymentListRefresh();
        }
    }
}
