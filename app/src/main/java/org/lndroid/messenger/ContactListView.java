package org.lndroid.messenger;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.lndroid.framework.WalletData;

class ContactListView {

    public static class Holder extends RecyclerView.ViewHolder {

        private TextView name_;
        private WalletData.Contact contact_;

        public Holder(View itemView) {
            super(itemView);

            name_ = itemView.findViewById(R.id.name);
        }

        public void bindTo(WalletData.Contact c) {
            name_.setText(c.name());
            contact_ = c;
        }

        public WalletData.Contact contact() {
            return contact_;
        }

        public void clear() {
            contact_ = null;
            name_.setText("");
        }
    }

    public static class Adapter extends PagedListAdapter<WalletData.Contact, Holder> {


        private class EmptyView extends RecyclerView.AdapterDataObserver {
            private View emptyView_;

            public EmptyView(View ev) {
                emptyView_ = ev;
                checkIfEmpty();
            }

            private void checkIfEmpty() {
                boolean emptyViewVisible = Adapter.this.getItemCount() == 0;
                emptyView_.setVisibility(emptyViewVisible ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onChanged() {
                checkIfEmpty();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                checkIfEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                checkIfEmpty();
            }
        }

        private View.OnClickListener itemClickListener_;

        public Adapter() {
            super(DIFF_CALLBACK);
        }

        public void setEmptyView(View view) {
            registerAdapterDataObserver(new EmptyView(view));
        }

        public void setItemClickListener(View.OnClickListener cl) {
            itemClickListener_ = cl;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            // Inflate the custom layout
            View view = inflater.inflate(R.layout.list_contacts, parent, false);
            if (itemClickListener_ != null)
                view.setOnClickListener(itemClickListener_);

            // Return a new holder instance
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder,
                                     int position) {
            WalletData.Contact c = getItem(position);

            if (c != null)
                holder.bindTo(c);
            else
                holder.clear();
        }

        private static DiffUtil.ItemCallback<WalletData.Contact> DIFF_CALLBACK
                = new DiffUtil.ItemCallback<WalletData.Contact>() {
            @Override
            public boolean areItemsTheSame(
                    @NonNull WalletData.Contact a, @NonNull WalletData.Contact b) {
                return a.id() == b.id();
            }

            @Override
            public boolean areContentsTheSame(
                    @NonNull WalletData.Contact a, @NonNull WalletData.Contact b) {

                return a.equals(b);
            }
        };
    }
}

