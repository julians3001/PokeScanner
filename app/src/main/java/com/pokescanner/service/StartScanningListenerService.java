package com.pokescanner.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.pokescanner.MapsActivity;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.objects.User;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.PermissionUtils;
import com.pokescanner.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.pokescanner.helper.Generation.makeHexScanMap;

/**
 * Created by Julian on 02.08.2016.
 */
public class StartScanningListenerService extends WearableListenerService {

    private GoogleApiClient mGoogleApiClient;
    List<LatLng> scanMap = new ArrayList<>();

    private static final String TAG = "WearData";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents);
        }
        final List events = FreezableUtils
                .freezeIterable(dataEvents);

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        // Loop through the events and send a message
        // to the node that created the data item.

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/start_scanning") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    if(dataMap.getBoolean("scan")){
                        scanMap();
                    }

                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    private void scanMap() {

        final MapsActivity mapsActivity = MapsActivity.instance;

        if (mapsActivity != null) {
            // we are calling here activity's method
            mapsActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!mapsActivity.activityStarted){

                        mapsActivity.onStart();
                    }
                    mapsActivity.PokeScan();
                }
            });
        }
    }

}