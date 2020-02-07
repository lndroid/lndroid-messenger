package org.lndroid.messenger;

import android.app.Application;

import org.lndroid.framework.usecases.ActionAddContactPaymentsPrivilege;

public class PaymentsPrivilegeViewModel extends WalletViewModelBase {

    private static final String TAG = "PaymentsPrivilegeViewModel";

    private ActionAddContactPaymentsPrivilege addContactPaymentsPrivilege_;

    public PaymentsPrivilegeViewModel(Application app) {
        super(app);
    }

    @Override
    protected void onCleared() {
        addContactPaymentsPrivilege_.destroy();
    }

    ActionAddContactPaymentsPrivilege addContactPaymentsPrivilege() { return addContactPaymentsPrivilege_; }

    @Override
    protected void onConnect() {
        addContactPaymentsPrivilege_ = new ActionAddContactPaymentsPrivilege(pluginClient());
    }
}



