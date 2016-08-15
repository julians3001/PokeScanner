
package com.pokescanner.recycler;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pokescanner.R;
import com.pokescanner.objects.NotificationItem;

import java.util.ArrayList;

/**
 * Created by Brian on 7/21/2016.
 */
public class NotificationRecyclerAdapter extends RecyclerView.Adapter<BlacklistViewHolder> {
    private onCheckedListener listener;
    private ArrayList<NotificationItem> filteritems;

    public interface onCheckedListener {
        void onChecked(NotificationItem filterItem);
    }

    public NotificationRecyclerAdapter(ArrayList<NotificationItem> filteritems, onCheckedListener listener) {
        this.filteritems = filteritems;
        this.listener = listener;
    }


    @Override
    public BlacklistViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_pokemon_filter_row, viewGroup, false);
        return new BlacklistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(BlacklistViewHolder filterViewHolder, int i) {
        filterViewHolder.bind(filteritems.get(i), listener);
    }

    @Override
    public int getItemCount() {
        return filteritems.size();
    }
}
