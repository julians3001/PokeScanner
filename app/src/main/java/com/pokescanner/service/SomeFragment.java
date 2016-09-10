package com.pokescanner.service;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.pokescanner.MapsActivity;
import com.pokescanner.OverlayMapsActivity;
import com.pokescanner.R;
import com.pokescanner.events.ForceRefreshEvent;
import com.pokescanner.events.RestartRefreshEvent;
import com.pokescanner.helper.Generation;
import com.pokescanner.helper.GymFilter;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.objects.Gym;
import com.pokescanner.objects.PokeStop;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.objects.User;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.PermissionUtils;
import com.pokescanner.utils.SettingsUtil;
import com.pokescanner.utils.UiUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


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

    public AlertDialog builderHeatMap;
    public ArrayList<LatLng> pokemonPosition;

    public ArrayList<MarkerOptions> markerHeatList;


    LocationManager locationManager;
    public static MapsActivity instance;

    Context myContext = getContext();

    public static boolean activityStarted = false;

    public int radioButtonID;

    User user;
    Realm realm;

    public RadioGroup radioGroupHeatMap;
    public View dialoglayoutHeatMap;

    TileOverlay mOverlay;

    private GoogleApiClient mGoogleApiClient;
    private GoogleApiClient mGoogleWearApiClient;
    List<LatLng> scanMap = new ArrayList<>();

    private Map<Pokemons, Marker> pokemonsMarkerMap = new HashMap<>();
    private Map<Gym, Marker> gymMarkerMap = new HashMap<>();
    private Map<PokeStop, Marker> pokestopMarkerMap = new HashMap<>();
    private ArrayList<Circle> circleArray = new ArrayList<>();
    public AlertDialog.Builder builderDeleteFile;

    AutoScanService mService;
    boolean mBound = false;

    RelativeLayout rl;

    Polygon mBoundingHexagon = null;

    public boolean scanCurrentPosition = false;

    public ArrayList<Pokemons> pokemonHeatList;

    public HeatmapTileProvider mHeatProvider;

    String TAG = "wear";
    public ServiceConnection mConnection;

    int pos = 1;
    //Used for determining Scan status
    boolean SCANNING_STATUS = false;
    boolean LIST_MODE = false;
    //Used for our refreshing of the map
    Subscription pokeonRefresher;
    Subscription gymstopRefresher;
    Realm realmDataBase;


    MapView mapView;
    public static SomeFragment someFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.overlay_activity_maps, container, false);


        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) v.findViewById(R.id.mapview);
        btnStopOverlayActivity = (ImageButton) v.findViewById(R.id.btnStopOverlayActivity);
        button = (FloatingActionButton) v.findViewById(R.id.btnOverlaySearch);
        progressBar = (ProgressBar) v.findViewById(R.id.OverlayprogressBar);
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
    public void onStart(){
        super.onStart();
        startRefresher();
    }

    @Override
    public void onStop(){
        super.onStop();

    }

    @Override
    public void onResume() {
        mapView.onResume();

        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (pokeonRefresher != null)
            pokeonRefresher.unsubscribe();
        if (gymstopRefresher != null)
            gymstopRefresher.unsubscribe();
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
        Intent launchIntent = getActivity().getPackageManager().getLaunchIntentForPackage("com.pokescanner");
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(launchIntent!=null){
            startActivity(launchIntent);
        }
        getActivity().finish();
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

    public void showProgressbar(boolean status) {
        if (status) {
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progressBar.setVisibility(View.VISIBLE);
            button.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_white_24dp));
            SCANNING_STATUS = true;
        } else {
            removeCircleArray();
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progressBar.setVisibility(View.INVISIBLE);
            button.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_track_changes_white_24dp));
            SCANNING_STATUS = false;
        }
    }

    public void createMapObjects() {
        if (SettingsUtil.getSettings(getContext()).isBoundingBoxEnabled()) {
            createBoundingBox();
        } else {
            removeBoundingBox();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void forceRefreshEvent(ForceRefreshEvent event) {
        refreshGymsAndPokestops();
        refreshMap();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRestartRefreshEvent(RestartRefreshEvent event) {
        System.out.println(Settings.get(getContext()).getServerRefresh());
        refreshGymsAndPokestops();
        refreshMap();
        startRefresher();
    }

    public void startRefresher() {
        if (pokeonRefresher != null)
            pokeonRefresher.unsubscribe();
        if (gymstopRefresher != null)
            gymstopRefresher.unsubscribe();

        //Using RX java we setup an interval to refresh the map
        pokeonRefresher = Observable.interval(SettingsUtil.getSettings(getContext()).getMapRefresh(), TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        //System.out.println("Refreshing Pokemons");
                        refreshMap();
                    }
                });

        gymstopRefresher = Observable.interval(30, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        //System.out.println("Refreshing Gyms");
                        refreshGymsAndPokestops();
                    }
                });
    }

    //Map related Functions
    public void refreshMap() {

        if (!MultiAccountLoader.areThreadsRunning()&&!MultiAccountLoader.autoScan) {
            showProgressbar(false);
        }

        if (!LIST_MODE) {
            if (mMap != null) {
                LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
                createMapObjects();

                //Load our Pokemon Array
                ArrayList<Pokemons> pokemons = new ArrayList<Pokemons>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
                //Okay so we're going to fix the annoying issue where the markers were being constantly redrawn
                for (int i = 0; i < pokemons.size(); i++) {
                    //Get our pokemon from the list
                    Pokemons pokemon = pokemons.get(i);
                    //Is our pokemon contained within the bounds of the camera?
                    if (curScreen.contains(new LatLng(pokemon.getLatitude(), pokemon.getLongitude()))) {
                        //If yes then has he expired?
                        //This isnt worded right it should say isNotExpired (Will fix later)
                        if (pokemon.isNotExpired()) {
                            if (UiUtils.isPokemonFiltered(pokemon) ||
                                    UiUtils.isPokemonExpiredFiltered(pokemon, getContext())) {
                                if (pokemonsMarkerMap.containsKey(pokemon)) {
                                    Marker marker = pokemonsMarkerMap.get(pokemon);
                                    if (marker != null) {
                                        marker.remove();
                                        pokemonsMarkerMap.remove(pokemon);
                                    }
                                }
                            } else {
                                //Okay finally is he contained within our hashmap?
                                if (pokemonsMarkerMap.containsKey(pokemon)) {
                                    //Well if he is then lets pull out our marker.
                                    Marker marker = pokemonsMarkerMap.get(pokemon);
                                    //Update the marker
                                    //UNTESTED
                                    if (marker != null) {
                                        marker = pokemon.updateMarker(marker, getContext());
                                    }
                                } else {
                                    //If our pokemon wasn't in our hashmap lets add him
                                    pokemonsMarkerMap.put(pokemon, mMap.addMarker(pokemon.getMarker(getContext())));
                                }
                            }
                        } else {
                            //If our pokemon expired lets remove the marker
                            if (pokemonsMarkerMap.get(pokemon) != null)
                                pokemonsMarkerMap.get(pokemon).remove();
                            //Then remove the pokemon
                            pokemonsMarkerMap.remove(pokemon);
                            //Finally lets remove him from our realm.
                            realm.beginTransaction();
                            realm.where(Pokemons.class).equalTo("encounterid", pokemon.getEncounterid()).findAll().deleteAllFromRealm();
                            realm.commitTransaction();
                        }
                    } else {
                        //If our pokemon expired lets remove the marker
                        if (pokemonsMarkerMap.get(pokemon) != null)
                            pokemonsMarkerMap.get(pokemon).remove();
                        //Then remove the pokemon
                        pokemonsMarkerMap.remove(pokemon);
                    }
                }
            }
        }
    }

    public void refreshGymsAndPokestops() {
        if (!LIST_MODE) {
            //The the map bounds
            if (mMap != null) {
                LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;

                //Before we refresh we want to remove the old markers so lets do that first
                for (Map.Entry<Gym, Marker> gymMarker : gymMarkerMap.entrySet())
                    gymMarker.getValue().remove();
                for (Map.Entry<PokeStop, Marker> pokestopMarker : pokestopMarkerMap.entrySet())
                    pokestopMarker.getValue().remove();

                //Clear the hashmaps
                gymMarkerMap.clear();
                pokestopMarkerMap.clear();

                //Once we refresh our markers lets go ahead and load our pokemans
                ArrayList<Gym> gyms = new ArrayList<Gym>(realm.copyFromRealm(realm.where(Gym.class).findAll()));
                ArrayList<PokeStop> pokestops = new ArrayList<PokeStop>(realm.copyFromRealm(realm.where(PokeStop.class).findAll()));

                if (SettingsUtil.getSettings(getActivity()).isGymsEnabled()) {
                    for (int i = 0; i < gyms.size(); i++) {
                        Gym gym = gyms.get(i);
                        LatLng pos = new LatLng(gym.getLatitude(), gym.getLongitude());
                        if (curScreen.contains(pos) && !shouldGymBeRemoved(gym)) {
                            Marker marker = mMap.addMarker(gym.getMarker(getContext()));
                            gymMarkerMap.put(gym, marker);
                        }
                    }
                }

                boolean showAllStops = !Settings.get(getContext()).isShowOnlyLured();

                if (SettingsUtil.getSettings(getActivity()).isPokestopsEnabled()) {
                    for (int i = 0; i < pokestops.size(); i++) {
                        PokeStop pokestop = pokestops.get(i);
                        LatLng pos = new LatLng(pokestop.getLatitude(), pokestop.getLongitude());
                        if (curScreen.contains(pos)) {
                            if (pokestop.isHasLureInfo() || showAllStops) {
                                Marker marker = mMap.addMarker(pokestop.getMarker(getContext()));
                                pokestopMarkerMap.put(pokestop, marker);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean shouldGymBeRemoved(Gym gym) {
        GymFilter currentGymFilter = GymFilter.getGymFilter(getActivity());
        int guardPokemonCp = gym.getGuardPokemonCp();
        int minCp = currentGymFilter.getGuardPokemonMinCp();
        int maxCp = currentGymFilter.getGuardPokemonMaxCp();
        if (!((guardPokemonCp >= minCp) && (guardPokemonCp <= maxCp)) && (guardPokemonCp != 0))
            return true;
        int ownedByTeamValue = gym.getOwnedByTeamValue();
        switch (ownedByTeamValue) {
            case 0:
                if (!currentGymFilter.isNeutralGymsEnabled())
                    return true;
                break;
            case 1:
                if (!currentGymFilter.isBlueGymsEnabled())
                    return true;
                break;
            case 2:
                if (!currentGymFilter.isRedGymsEnabled())
                    return true;
                break;
            case 3:
                if (!currentGymFilter.isYellowGymsEnabled())
                    return true;
                break;
        }
        return false;
    }

    public void createBoundingBox() {
        if (SCANNING_STATUS) {
            if (scanMap.size() > 0) {
                removeBoundingBox();

                List<LatLng> boundingPoints = Generation.getCorners(scanMap);
                PolygonOptions polygonOptions = new PolygonOptions();
                for (LatLng latLng: boundingPoints){
                    polygonOptions.add(latLng);
                }
                polygonOptions.strokeColor(Color.parseColor("#80d22d2d"));

                mBoundingHexagon = mMap.addPolygon(polygonOptions);
            }
        }
    }

    public void removeBoundingBox() {
        if (mBoundingHexagon != null)
            mBoundingHexagon.remove();
    }

    public void removeCircleArray() {
        if (circleArray != null)
        {
            for(Circle circle: circleArray) {
                circle.remove();
            }

            circleArray.clear();
        }
    }
}
