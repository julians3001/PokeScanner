package com.pokescanner;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokescanner.ListViewHelper.ArrayAdapterList;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.utils.SettingsUtil;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainWearActivity extends Activity implements WearableListView.ClickListener, SwipeRefreshLayout.OnRefreshListener {

    private TextView mTextView;
    ArrayList<Pokemons> pokemons;
    ArrayList<Pokemons> pokemonsNotExpired;
    ArrayAdapterList adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    String LOG_TAG = "listrefresh";
    private int mInterval = 1000; // 5 seconds by default, can be changed later
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wear);

        mHandler = new Handler();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);


        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                /*adapter = new ArrayAdapterList(MainWearActivity.this, R.layout.recycler_list_view, pokemons);

                ListView listViewItems = (ListView) findViewById(R.id.listview);
                listViewItems.setAdapter(adapter);
                listViewItems.setEmptyView(findViewById(R.id.empty_list_view));*/





            }
        });
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                refreshList(); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };


    @Override
    protected void onPause(){
        super.onPause();
        mHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        mStatusChecker.run();

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);


        Gson gson = new Gson();
        String json = mPrefs.getString("pokemonlist", "");
        pokemons = gson.fromJson(json, new TypeToken<ArrayList<Pokemons>>() {}.getType());
        pokemonsNotExpired = new ArrayList<Pokemons>();

        for(int i = 0;i<pokemons.size();i++){
            if(pokemons.get(i).isExpired()){
                pokemonsNotExpired.add(pokemons.get(i));
            }
        }

        if(pokemonsNotExpired==null){

                stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                    @Override
                    public void onLayoutInflated(WatchViewStub stub) {
                        adapter = new ArrayAdapterList(MainWearActivity.this, R.layout.recycler_list_view, pokemonsNotExpired);

                        ListView listViewItems = (ListView) findViewById(R.id.listview);
                        listViewItems.setAdapter(adapter);
                        listViewItems.setEmptyView(findViewById(R.id.empty_list_view));

                        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
                        mSwipeRefreshLayout.setOnRefreshListener(MainWearActivity.this);


                    }
                });
            return;
        }
        if(pokemonsNotExpired.size()>0){
            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {

                @Override
                public void onLayoutInflated(WatchViewStub watchViewStub) {
                    adapter = new ArrayAdapterList(MainWearActivity.this, R.layout.recycler_list_view, pokemonsNotExpired);

                    ListView listViewItems = (ListView) findViewById(R.id.listview);
                    listViewItems.setAdapter(adapter);
                    listViewItems.setEmptyView(findViewById(R.id.empty_list_view));

                    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
                    mSwipeRefreshLayout.setOnRefreshListener(MainWearActivity.this);
                }
            });
        } else {
            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                @Override
                public void onLayoutInflated(WatchViewStub stub) {
                    adapter = new ArrayAdapterList(MainWearActivity.this, R.layout.recycler_list_view, pokemonsNotExpired);

                    ListView listViewItems = (ListView) findViewById(R.id.listview);
                    listViewItems.setAdapter(adapter);
                    listViewItems.setEmptyView(findViewById(R.id.empty_list_view));

                    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
                    mSwipeRefreshLayout.setOnRefreshListener(MainWearActivity.this);
                }
            });
        }



    }

    // WearableListView click listener
    @Override
    public void onClick(WearableListView.ViewHolder v) {
        Integer tag = (Integer) v.itemView.getTag();
        // use this data to complete some action ...
    }

    @Override
    public void onTopEmptyRegionClick() {
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        refreshList();


        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void refreshList(){
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);


        Gson gson = new Gson();
        String json = mPrefs.getString("pokemonlist", "");
        pokemons = gson.fromJson(json, new TypeToken<ArrayList<Pokemons>>() {}.getType());
        pokemonsNotExpired = new ArrayList<Pokemons>();

        for(int i = 0;i<pokemons.size();i++){
            if(pokemons.get(i).isExpired()){
                pokemonsNotExpired.add(pokemons.get(i));
            }
        }
        if(adapter!=null){
            adapter.setNotifyOnChange(false);
            adapter.clear();
            adapter.addAll(pokemonsNotExpired);
            adapter.notifyDataSetChanged();
        }
    }
}
