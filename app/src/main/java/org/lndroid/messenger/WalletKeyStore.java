package org.lndroid.messenger;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;

import org.lndroid.framework.common.HEX;
import org.lndroid.framework.common.ISigner;
import org.lndroid.framework.defaults.DefaultSigner;

public class WalletKeyStore {

    private static final String ALIAS = "WalletAppKeyPair";

    private static final WalletKeyStore INSTANCE = new WalletKeyStore();
    private static final String TAG = "WalletKeyStore";

    private ISigner signer_;

    static WalletKeyStore getInstance() { return INSTANCE; }

    private WalletKeyStore() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            if (!ks.containsAlias(ALIAS))
                return;

            KeyStore.PrivateKeyEntry key = ((KeyStore.PrivateKeyEntry)ks.getEntry(ALIAS, null));
            signer_ = DefaultSigner.create(key.getPrivateKey(),
                    HEX.fromBytes(key.getCertificate().getPublicKey().getEncoded()));
        } catch (Exception e) {
            Log.e(TAG, "keystore error " + e);
        }
    }

    private void generateAppKeyPair() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        try {

            KeyGenParameterSpec params = new KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build();

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            kpg.initialize(params);

            KeyPair kp = kpg.generateKeyPair();
            signer_ = DefaultSigner.create(kp.getPrivate(),
                    HEX.fromBytes(kp.getPublic().getEncoded()));

        } catch (Exception e) {
            Log.e(TAG, "generate key pair error "+e);
        }
    }

    String getAppPubkey() {
        if (signer_ == null) {
            generateAppKeyPair();
        }
        return signer_.getPublicKey();
    }

    ISigner getSigner() {
        return signer_;
    }
}
