package com.pokescanner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokescanner.ListViewHelper.ArrayAdapterList;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.utils.PermissionUtils;
import com.pokescanner.utils.SettingsUtil;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainWearActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final static int LOCATION_PERMISSION_REQUESTED = 1400;
    private TextView mTextView;
    ArrayList<Pokemons> pokemons;
    ArrayList<Pokemons> pokemonsNotExpired;
    ArrayAdapterList adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    String LOG_TAG = "listrefresh";
    private int mInterval = 1000; // 5 seconds by default, can be changed later
    private Handler mHandler;
    private GoogleApiClient mGoogleApiClient;
    String TAG = "GPSWEAR";
    Location location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wear);

        mHandler = new Handler();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).addApi(LocationServices.API)
                    .addApi(Wearable.API)  // used for data layer API
                    .addApi(LocationServices.API)
                    .build();
        }

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        getLocationPermission();

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
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mStatusChecker);

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mGoogleApiClient, this);
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        mStatusChecker.run();

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);


        Gson gson = new Gson();
        String json = mPrefs.getString("pokemonlist", "");
        pokemons = gson.fromJson(json, new TypeToken<ArrayList<Pokemons>>() {
        }.getType());
        pokemonsNotExpired = new ArrayList<Pokemons>();



        if(pokemons==null){
            return;
        }
        for (int i = 0; i < pokemons.size(); i++) {
            if (pokemons.get(i).isExpired()) {
                //DO MATH
                Location temp = new Location("");

                temp.setLatitude(pokemons.get(i).getLatitude());
                temp.setLongitude(pokemons.get(i).getLongitude());
                double distance=0;
                if(location!=null){

                    distance = location.distanceTo(temp);
                }
                pokemons.get(i).setDistance(distance);
                pokemonsNotExpired.add(pokemons.get(i));
            }
        }

        if (pokemonsNotExpired == null) {

            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                @Override
                public void onLayoutInflated(WatchViewStub stub) {
                    setListView();

                    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
                    mSwipeRefreshLayout.setOnRefreshListener(MainWearActivity.this);


                }
            });
            return;
        }
        if (pokemonsNotExpired.size() > 0) {
            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {

                @Override
                public void onLayoutInflated(WatchViewStub watchViewStub) {
                    setListView();

                    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
                    mSwipeRefreshLayout.setOnRefreshListener(MainWearActivity.this);
                }
            });
        } else {
            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                @Override
                public void onLayoutInflated(WatchViewStub stub) {
                   setListView();

                    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
                    mSwipeRefreshLayout.setOnRefreshListener(MainWearActivity.this);
                }
            });
        }


    }

    private void setListView(){
        adapter = new ArrayAdapterList(MainWearActivity.this, R.layout.recycler_list_view, pokemonsNotExpired);

        ListView listViewItems = (ListView) findViewById(R.id.listview);
        listViewItems.setAdapter(adapter);
        listViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Pokemons pokemons = pokemonsNotExpired.get(i);


                String uri = String.format(Locale.ENGLISH, "geo:%f,%f", pokemons.getLatitude(), pokemons.getLongitude());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                MainWearActivity.this.startActivity(intent);
            }
        });
        listViewItems.setEmptyView(findViewById(R.id.empty_list_view));
    }


    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        refreshList();


        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void refreshList() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);


        Gson gson = new Gson();
        String json = mPrefs.getString("pokemonlist", "");
        pokemons = gson.fromJson(json, new TypeToken<ArrayList<Pokemons>>() {
        }.getType());
        pokemonsNotExpired = new ArrayList<Pokemons>();

        if(pokemons == null){
            return;
        }

        for (int i = 0; i < pokemons.size(); i++) {
            if (pokemons.get(i).isExpired()) {
                //DO MATH
                Location temp = new Location("");

                temp.setLatitude(pokemons.get(i).getLatitude());
                temp.setLongitude(pokemons.get(i).getLongitude());

                double distance=0;
                if(location!=null){

                    distance = location.distanceTo(temp);
                }

                pokemons.get(i).setDistance(distance);
                pokemonsNotExpired.add(pokemons.get(i));
            }
        }


        if (adapter != null) {
            adapter.setNotifyOnChange(false);
            adapter.clear();
            adapter.addAll(pokemonsNotExpired);
            adapter.notifyDataSetChanged();
        }
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Create the LocationRequest object
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(5000);

        // Register listener using the LocationRequest object
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "connection to location client suspended");
        }
    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;

    }

    private boolean getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,}, LOCATION_PERMISSION_REQUESTED);
            return false;
        }
        return true;
    }


}
