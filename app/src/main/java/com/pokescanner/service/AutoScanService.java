package com.pokescanner.service;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.pokescanner.MapsActivity;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

import static com.pokescanner.helper.Generation.makeHexScanMap;

/**
 * Created by Julian on 10.08.2016.
 */
public class AutoScanService extends IntentService{

    List<LatLng> scanMap = new ArrayList<>();

    public AutoScanService(){
        super("PokeScanner");
    }
    @Override
    protected void onHandleIntent(Intent intent) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(MultiAccountLoader.autoScan){
                    LatLng scanPosition = getCurrentLocation();
                    int scanValue = Settings.get(getApplicationContext()).getScanValue();
                    if (scanPosition != null) {
                        scanMap = makeHexScanMap(scanPosition, scanValue, 1, new ArrayList<LatLng>());
                        if (scanMap != null) {
                            MultiAccountLoader.setScanMap(scanMap);
                            MultiAccountLoader.startThreads();
                        }
                    }

                    Handler handler = new Handler(Looper.getMainLooper());

                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(AutoScanService.this.getApplicationContext(),"New Scan started...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    for(Thread thread: MultiAccountLoader.threads){
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(11000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    @SuppressWarnings({"MissingPermission"})
    public LatLng getCurrentLocation() {
        if (PermissionUtils.doWeHaveGPSandLOC(this)) {
            if (MultiAccountLoader.mGoogleApiClient.isConnected()) {
                Location location = LocationServices.FusedLocationApi.getLastLocation(MultiAccountLoader.mGoogleApiClient);
                if (location != null) {
                    return new LatLng(location.getLatitude(), location.getLongitude());
                }
                return null;
            }
            return null;
        }
        return null;
    }
}
