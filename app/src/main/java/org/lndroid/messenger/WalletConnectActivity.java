package org.lndroid.messenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class WalletConnectActivity extends AppCompatActivity {

    private static final String TAG = "WalletConnectActivity";

    private static final int APP_CONNECT_RC = 1001;

    private WalletConnectViewModel model_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_connect);

        model_ = ViewModelProviders.of(this).get(WalletConnectViewModel.class);

        Button b = findViewById(R.id.connect);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGetWalletServiceActivity();
            }
        });

    }

    private void startGetWalletServiceActivity() {
        Intent intent = model_.createGetWalletServiceIntent();
        startActivityForResult(intent, APP_CONNECT_RC);
    }

    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data) {
        if (requestCode == APP_CONNECT_RC) {
            if (resultCode == RESULT_OK && data != null) {
                model_.setWalletService(data);
                showHome();
            } else {
                Log.e(TAG, "Cancelled app connect");
            }
        }
    }

    private void showHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
