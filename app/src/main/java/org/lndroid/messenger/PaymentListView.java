package org.lndroid.messenger;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.lndroid.framework.WalletData;

class PaymentListView {

    public static class Holder extends RecyclerView.ViewHolder {
        private View parent_;
        private TextView time_;
        private TextView text_;
        private TextView amount_;
        private TextView status_;
        private DateFormat dateFormat_;
        private WalletData.Payment payment_;

        public Holder(View itemView) {
            super(itemView);

            parent_ = itemView;
            time_ = itemView.findViewById(R.id.time);
            text_ = itemView.findViewById(R.id.text);
            amount_ = itemView.findViewById(R.id.amount);
            status_ = itemView.findViewById(R.id.status);

            dateFormat_ = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("en", "US"));
        }

        public void bindToInvoice(WalletData.Payment p, WalletData.Invoice i) {
            // FIXME make relative to screen width
            parent_.setPadding(0, 20, 200, 0);
            text_.setText(p.message());
            amount_.setText("+"+i.amountPaidMsat()/1000+" sat,");
            time_.setText(dateFormat_.format(new Date(p.time())));
            status_.setText("");
        }

        public void bindToSendPayment(WalletData.Payment p, WalletData.SendPayment sp) {
            parent_.setPadding(200, 20, 0, 0);
            switch (sp.state()) {
                case WalletData.SEND_PAYMENT_STATE_OK:
                    status_.setText("");
                    break;
                case WalletData.SEND_PAYMENT_STATE_FAILED:
                    status_.setText("Failed");
                    break;
                case WalletData.SEND_PAYMENT_STATE_PENDING:
                    if (sp.nextTryTime() > 0)
                        status_.setText("Retrying");
                    else
                        status_.setText("Sending");
                    break;
                case WalletData.SEND_PAYMENT_STATE_SENDING:
                    status_.setText("Sending");
                    break;
            }

            amount_.setText("-"+sp.valueMsat()/1000+" sat,");
            time_.setText(dateFormat_.format(new Date(sp.createTime())));
            text_.setText(p.message());
        }

        public void bindTo(WalletData.Payment p) {
            payment_= p;
            switch (p.type()) {
                case WalletData.PAYMENT_TYPE_INVOICE:
                    bindToInvoice(p, p.invoices().get(p.sourceId()));
                    break;
                case WalletData.PAYMENT_TYPE_SENDPAYMENT:
                    bindToSendPayment(p, p.sendPayments().get(p.sourceId()));
                    break;
            }
        }

        public WalletData.Payment payment() {
            return payment_;
        }

        public void clear() {
            payment_ = null;
            text_.setText("");
            status_.setText("");
            amount_.setText("");
            time_.setText("");
        }
    }

    public static class Adapter extends PagedListAdapter<WalletData.Payment, Holder> {

        protected Adapter() {
            super(DIFF_CALLBACK);
        }

        private View.OnClickListener itemClickListener_;

        public void setItemClickListener (View.OnClickListener cl) {
            itemClickListener_ = cl;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            // Inflate the custom layout
            View view = inflater.inflate(R.layout.list_payments, parent, false);
            if (itemClickListener_ != null)
                view.setOnClickListener(itemClickListener_);

            // Return a new holder instance
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder,
                                     int position) {
            WalletData.Payment payment = getItem(position);

            if (payment != null)
                holder.bindTo(payment);
            else
                holder.clear();
        }

        private static DiffUtil.ItemCallback<WalletData.Payment> DIFF_CALLBACK
                = new DiffUtil.ItemCallback<WalletData.Payment>() {
            @Override
            public boolean areItemsTheSame(
                    @NonNull WalletData.Payment a, @NonNull WalletData.Payment b) {
                Log.i("DI", "a "+a.id()+" b "+b.id());
                return a.id() == b.id();
            }

            @Override
            public boolean areContentsTheSame(
                    @NonNull WalletData.Payment a, @NonNull WalletData.Payment b) {

                Log.i("DI", "a "+a.id()+" b "+b.id()+" same "+a.equals(b));
                return a.equals(b);
            }
        };
    }

}