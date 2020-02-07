package org.lndroid.messenger;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public abstract class WalletServiceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void setWalletService(WalletService s);
    @Query("SELECT * FROM WalletService WHERE id = 0")
    abstract WalletService getWalletService();
}
