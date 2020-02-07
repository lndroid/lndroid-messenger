package org.lndroid.messenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.content.Intent;
import android.os.Bundle;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        WelcomeViewModel model = ViewModelProviders.of(this).get(WelcomeViewModel.class);
        model.walletService().observe(this, new Observer<WalletService>() {
            @Override
            public void onChanged(WalletService walletService) {
                if (walletService != null)
                    showHome();
                else
                    showConnect();
            }
        });
    }

    private void showHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showConnect() {
        Intent intent = new Intent(this, WalletConnectActivity.class);
        startActivity(intent);
        finish();
    }

}
