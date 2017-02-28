package com.pokescanner.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.pokescanner.MapsActivity;
import com.pokescanner.R;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.PermissionUtils;
import com.pokescanner.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.services.concurrency.Priority;
import io.realm.Realm;

import static com.pokescanner.helper.Generation.makeHexScanMap;

/**
 * Created by Julian on 10.08.2016.
 */
public class AutoScanService extends Service{

    List<LatLng> scanMap = new ArrayList<>();
    ArrayList<Pokemons> oldPokelist = new ArrayList<>();

    public AutoScanService(){
        super();
    }


    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId){


        return START_REDELIVER_INTENT;
    }*/

    @Override
    public void onCreate(){
        Intent notificationIntent = new Intent(this, MapsActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icon).copy(Bitmap.Config.ARGB_8888, true);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                .setLargeIcon(bm)
                .setContentTitle("Autoscan-Service is running")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        final Intent incomingIntent = intent;


        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean even = false;
                while(MultiAccountLoader.autoScan){
                    even = !even;
                    if(incomingIntent.getIntExtra("mode",-1)==0) {
                        LatLng scanPosition = getCurrentLocation();
                        int scanValue = Settings.get(getApplicationContext()).getScanValue();
                        if (scanPosition != null) {

                            scanMap = makeHexScanMap(scanPosition, scanValue, 1, new ArrayList<LatLng>());
                            //if(!even){
                              //  Collections.reverse(scanMap);
                            //}
                            MultiAccountLoader.even = even;
                            if (scanMap != null) {
                                MultiAccountLoader.setScanMap(scanMap);
                                MultiAccountLoader.startThreads();
                            }
                        }
                    } else {
                        MultiAccountLoader.startThreads();
                    }
                    Handler handler = new Handler(Looper.getMainLooper());

                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(AutoScanService.this.getApplicationContext(),"New Scan started...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    System.out.println("New Scan started...");
                    for(Thread thread: MultiAccountLoader.threads){
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        return null;
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

    public class LocalBinder extends Binder {
        public AutoScanService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AutoScanService.this;
        }
    }
}
