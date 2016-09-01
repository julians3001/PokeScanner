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
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokescanner.ListViewHelper.ArrayAdapterList;
import com.pokescanner.objects.Pokemons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import com.github.clans.fab.FloatingActionMenu;

public class MainWearActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private int MESSAGESENTSCAN = 0;
    private int MESSAGESENTCLEAN = 0;
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
    FloatingActionMenu floatingMenu;
    FloatingActionButton buttonStartScan;
    FloatingActionButton buttonStopScan;
    FloatingActionButton buttonClear;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MultiDex.install(this);
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



        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {




            }
        });
    }

    public void onStartButtonClicked(View view){
        floatingMenu.close(true);
        sendScanStartStopMessageToPhone(true);
    }

    public void onCleanButtonClicked(View view){
        floatingMenu.close(true);
        sendCleanMessageToPhone();
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
        //getLocationPermission();


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



            stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                @Override
                public void onLayoutInflated(WatchViewStub stub) {
                    if(pokemonsNotExpired!=null){
                        setListView();
                    }
                    floatingMenu = (FloatingActionMenu) findViewById(R.id.floatActionMenu);
                    buttonStartScan = (FloatingActionButton) findViewById(R.id.btnStartScan);
                    buttonStopScan = (FloatingActionButton) findViewById(R.id.btnStopScan);
                    buttonClear = (FloatingActionButton) findViewById(R.id.btnClean);
                    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
                    mSwipeRefreshLayout.setOnRefreshListener(MainWearActivity.this);
                    progressBar = (ProgressBar) findViewById(R.id.progressBar);

                }
            });

        mStatusChecker.run();

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
        sendScanStartStopMessageToPhone(true);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void sendScanStartStopMessageToPhone(boolean scan){

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/start_scanning");
        putDataMapReq.getDataMap().putBoolean("scan", scan);
        putDataMapReq.getDataMap().putInt("sent", MESSAGESENTSCAN++);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

        Context context = getApplicationContext();
        CharSequence text = "Start scanning...";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void sendCleanMessageToPhone(){


        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/start_cleaning");
        putDataMapReq.getDataMap().putBoolean("clean", true);
        putDataMapReq.getDataMap().putInt("sent", MESSAGESENTCLEAN++);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

        Context context = getApplicationContext();
        CharSequence text = "Start cleaning...";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
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
        if(progressBar!=null){
            int pos = mPrefs.getInt("progressbar",1);
            int scanmapsize = mPrefs.getInt("scanmapsize",1);
            boolean scanstatus = mPrefs.getBoolean("scanstatus", false);
            if(scanstatus){
                progressBar.setVisibility(View.VISIBLE);
                float progress = (float) (pos-1) * 100 / scanmapsize;
                progressBar.setProgress((int) progress);
                if((int) progress == 100) {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }
        }

        sortPokemonlist();

        if (adapter != null) {
            adapter.setNotifyOnChange(false);
            adapter.clear();
            adapter.addAll(pokemonsNotExpired);
            adapter.notifyDataSetChanged();
        }
    }

    private void sortPokemonlist() {
        if(pokemonsNotExpired!=null){
            Pokemons temp;
            for(int i=1; i<pokemonsNotExpired.size(); i++) {
                for(int j=0; j<pokemonsNotExpired.size()-i; j++) {
                    if(pokemonsNotExpired.get(j).getDistance()>pokemonsNotExpired.get(j+1).getDistance()) {
                        temp=pokemonsNotExpired.get(j);
                        pokemonsNotExpired.set(j,pokemonsNotExpired.get(j+1));
                        pokemonsNotExpired.set(j+1,temp);
                    }

                }
            }
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
