package org.lndroid.messenger;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.paging.PagedList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.usecases.ActionShareContact;
import org.lndroid.framework.usecases.ListContacts;
import org.lndroid.framework.usecases.ActionAddAppContact;

public class MainViewModel extends WalletViewModelBase {

    private static final String TAG = "MainViewModel";

    private ActionAddAppContact addAppContact_;
    private ActionShareContact shareContact_;
    private ListContacts contactListLoader_;
    private ListContacts.Pager contactListPager_;

    public MainViewModel(Application app) {
        super(app);
    }

    @Override
    protected void onCleared() {
        addAppContact_.destroy();
        shareContact_.destroy();
        contactListLoader_.destroy();
    }

    ActionAddAppContact addAppContact() { return addAppContact_; }
    ActionShareContact shareContact() { return shareContact_; }

    @Override
    protected void onConnect() {

        addAppContact_ = new ActionAddAppContact(pluginClient());
        shareContact_ = new ActionShareContact(pluginClient());

        contactListLoader_ = new ListContacts(pluginClient());

        PagedList.Config contactListConfig = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                .build();
        contactListPager_ = contactListLoader_.createPager(contactListConfig);
    }

    // set new request, invalidates paymentList PagedList so that
    // results could be reloaded
    void setListContactsRequest(WalletData.ListContactsRequest r) {
        contactListPager_.setRequest(r.toBuilder()
                .setNoAuth(true)
                .setEnablePaging(true)
                .build()
        );
    }

    LiveData<PagedList<WalletData.Contact>> contactList() { return contactListPager_.pagedList(); };
    LiveData<WalletData.Error> contactListError () { return contactListLoader_.error(); }
    void contactListRefresh() { contactListPager_.invalidate(); }
}
