package org.lndroid.messenger;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.lndroid.framework.common.HEX;

public class WalletKeyStore {

    private static final String ALIAS = "WalletAppKeyPair";

    private static final WalletKeyStore INSTANCE = new WalletKeyStore();
    private static final String TAG = "WalletKeyStore";

    private String pubkey_;

    static WalletKeyStore getInstance() { return INSTANCE; }

    private WalletKeyStore() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);

            if (!ks.containsAlias(ALIAS))
                return;

            PublicKey key = ((KeyStore.PrivateKeyEntry)ks.getEntry(ALIAS, null)).getCertificate().getPublicKey();
            pubkey_ = HEX.fromBytes(key.getEncoded());
        } catch (Exception e) {
            Log.e(TAG, "keystore error " + e);
        }
    }

    private String generateAppKeyPair() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return null;

        try {

            KeyGenParameterSpec params = new KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256,
                            KeyProperties.DIGEST_SHA512)
                    .build();

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            kpg.initialize(params);

            KeyPair kp = kpg.generateKeyPair();

            return HEX.fromBytes(kp.getPublic().getEncoded());

        } catch (Exception e) {
            Log.e(TAG, "generate key pair error "+e);
        }

        return null;
    }

    String getAppPubkey() {
        if (pubkey_ == null)
            pubkey_ = generateAppKeyPair();
        return pubkey_;
    }
}
