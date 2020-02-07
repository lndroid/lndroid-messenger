package org.lndroid.messenger;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class WalletService {
    @PrimaryKey
    public long id;
    public String pubkey;
    public String className;
    public String packageName;
}
