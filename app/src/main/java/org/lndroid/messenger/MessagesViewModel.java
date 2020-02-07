package org.lndroid.messenger;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.paging.PagedList;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.usecases.JobSendPayment;
import org.lndroid.framework.usecases.ListPayments;

public class MessagesViewModel extends WalletViewModelBase {


    private static final String TAG = "MainViewModel";

    private JobSendPayment sendPayment_;
    private ListPayments paymentListLoader_;
    private ListPayments.Pager paymentListPager_;

    public MessagesViewModel(Application app) {
        super(app);
    }

    @Override
    protected void onCleared() {
        paymentListLoader_.destroy();
        sendPayment_.destroy();
    }

//    ActionAddAppContact addAppContact() { return addAppContact_; }

    @Override
    protected void onConnect() {

        sendPayment_ = new JobSendPayment(pluginClient());

        paymentListLoader_ = new ListPayments(pluginClient());

        PagedList.Config listConfig = new PagedList.Config.Builder()
                .setEnablePlaceholders(true)
                .setPageSize(10)
                .build();
        paymentListPager_ = paymentListLoader_.createPager(listConfig);
    }

    JobSendPayment sendPayment() { return sendPayment_; }

    // set new request, invalidates paymentList PagedList so that
    // results could be reloaded
    void setListPaymentsRequest(WalletData.ListPaymentsRequest r) {
        paymentListPager_.setRequest(r.toBuilder()
                .setOnlyMessages(true)
                .setNoAuth(true)
                .setEnablePaging(true)
                .build()
        );
    }

    LiveData<PagedList<WalletData.Payment>> paymentList() { return paymentListPager_.pagedList(); };
    void paymentListRefresh() { paymentListPager_.invalidate(); }
    LiveData<WalletData.Error> paymentListError () { return paymentListLoader_.error(); }


}
