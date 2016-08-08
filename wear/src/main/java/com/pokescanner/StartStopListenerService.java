package com.pokescanner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class StartStopListenerService extends WearableListenerService {

    private static final String TAG = "WearData";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
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
                if (item.getUri().getPath().compareTo("/startstopscan") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    Intent dialogIntent = new Intent(this, MainWearActivity.class);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    boolean scanstatus = dataMap.getBoolean("scanstatus");
                    int scanmapsize = dataMap.getInt("scanmapsize");

                    SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

                    SharedPreferences.Editor prefsEditor = mPrefs.edit();
                    if(!scanstatus){
                        prefsEditor.putInt("progressbar",1);
                    }
                    prefsEditor.putBoolean("scanstatus", scanstatus);
                    prefsEditor.putInt("scanmapsize", scanmapsize);
                    prefsEditor.commit();
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }
}