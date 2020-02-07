package org.lndroid.messenger;

import android.app.Application;

import org.lndroid.framework.usecases.ActionAddListContactsPrivilege;

public class ContactsPrivilegeViewModel extends WalletViewModelBase {

    private static final String TAG = "ContactsPrivilegeViewModel";

    private ActionAddListContactsPrivilege addListContactsPrivilege_;

    public ContactsPrivilegeViewModel(Application app) {
        super(app);
    }

    @Override
    protected void onCleared() {
        addListContactsPrivilege_.destroy();
    }

    ActionAddListContactsPrivilege addListContactsPrivilege() { return addListContactsPrivilege_; }

    @Override
    protected void onConnect() {
        addListContactsPrivilege_ = new ActionAddListContactsPrivilege(pluginClient());
    }
}


