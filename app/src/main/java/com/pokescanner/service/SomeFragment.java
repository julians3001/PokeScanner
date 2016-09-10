package com.pokescanner.service;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.pokescanner.MapsActivity;
import com.pokescanner.OverlayMapsActivity;
import com.pokescanner.R;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.PermissionUtils;

import butterknife.BindView;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmConfiguration;


public class SomeFragment extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    FloatingActionButton button;
    ProgressBar progressBar;
    GoogleMap mMap;
    ImageButton btnSettings;
    RelativeLayout main;
    com.github.clans.fab.FloatingActionButton btnSataliteMode;
    FloatingActionMenu floatingActionMenu;
    ImageButton btnAutoScan;
    ImageButton btnHeatMapMode;
    ImageButton btnCenterCamera;
    ImageButton btnStopOverlayActivity;

    Realm realm;

    MapView mapView;
    public static SomeFragment someFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.overlay_activity_maps, container, false);


        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) v.findViewById(R.id.mapview);
        btnStopOverlayActivity = (ImageButton) v.findViewById(R.id.btnStopOverlayActivity);
        btnStopOverlayActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopOverlayService();
            }
        });
        btnCenterCamera = (ImageButton) v.findViewById(R.id.btnOverlayCenterCamera);
        btnCenterCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnCenterCamera();
            }
        });
        floatingActionMenu = (FloatingActionMenu) v.findViewById(R.id.floatOverlayActionMenu);
        mapView.onCreate(savedInstanceState);

        // Gets to GoogleMap from the MapView and does initialization stuff
        mapView.getMapAsync(this);
        someFragment = this;

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(getContext())
                .name(Realm.DEFAULT_REALM_NAME)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
        realm = Realm.getDefaultInstance();



        return v;
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(true);

        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        MapsInitializer.initialize(this.getActivity());

        // Updates the location and zoom of the MapView
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(getCurrentLocation(),15);
        mMap.animateCamera(cameraUpdate);
    }

    @SuppressWarnings({"MissingPermission"})
    public LatLng getCurrentLocation() {
        if (PermissionUtils.doWeHaveGPSandLOC(getContext())) {
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public void stopOverlayService(){
        floatingActionMenu.close(true);
        Intent intent = new Intent (getContext(), OverlayService.class);
        getContext().stopService(intent);
        Intent activityIntent = new Intent(getContext(), MapsActivity.class);
        startActivity(activityIntent);
    }

    public void btnCenterCamera(){
        floatingActionMenu.close(true);
        moveCameraToCurrentPosition(true);

    }

    public boolean moveCameraToCurrentPosition(boolean zoom) {
        LatLng GPS_LOCATION = getCurrentLocation();
        if (GPS_LOCATION != null) {
            if (mMap != null) {
                if (zoom || Settings.get(getContext()).isDrivingModeEnabled()) {
                    this.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(GPS_LOCATION,15));
                } else {
                    this.mMap.animateCamera(CameraUpdateFactory.newLatLng(GPS_LOCATION));
                }

                return true;
            } else {
                return false;
            }
        } else {
            //Try again after half a second
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    moveCameraToCurrentPosition(true);
                }
            }, 500);
        }
        return false;
    }
}
