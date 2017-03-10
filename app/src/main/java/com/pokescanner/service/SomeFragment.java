package com.pokescanner.service;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.ads.internal.formats.zzk;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.pokegoapi.api.PokemonGo;
import com.pokescanner.MapsActivity;
import com.pokescanner.OverlayMapsActivity;
import com.pokescanner.R;
import com.pokescanner.events.ForceRefreshEvent;
import com.pokescanner.events.MinimizeToggleEvent;
import com.pokescanner.events.RestartRefreshEvent;
import com.pokescanner.events.ScanCircleEvent;
import com.pokescanner.exceptions.NoCameraPositionException;
import com.pokescanner.exceptions.NoMapException;
import com.pokescanner.helper.Generation;
import com.pokescanner.helper.GymFilter;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.loaders.PokemonGoWithUsername;
import com.pokescanner.objects.Gym;
import com.pokescanner.objects.PokeStop;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.objects.User;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.DrawableUtils;
import com.pokescanner.utils.MarkerDetails;
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

import static com.pokescanner.helper.Generation.makeHexScanMap;


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
    ImageButton btnClear;
    com.github.clans.fab.FloatingActionButton btnCenterCamera;
    ImageButton btnStopOverlayActivity;
    ImageButton btnMinimize;

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
    public static boolean isInOverlayMode;

    boolean CENTER_ALWAYS = false;

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

    public GoogleMap getFragmentMap(){
        return mMap;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.overlay_activity_maps, container, false);


        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) v.findViewById(R.id.mapview);
        btnStopOverlayActivity = (ImageButton) v.findViewById(R.id.btnStopOverlayActivity);
        button = (FloatingActionButton) v.findViewById(R.id.btnOverlaySearch);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PokeScan();
            }
        });
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return onLongClickSearch();
            }
        });
        progressBar = (ProgressBar) v.findViewById(R.id.OverlayprogressBar);
        btnStopOverlayActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopOverlayService();
            }
        });
        btnClear = (ImageButton) v.findViewById(R.id.btnOverlayClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cleanPokemon();
            }
        });

        btnAutoScan = (ImageButton) v.findViewById(R.id.btnOverlayAutoScan);
        btnAutoScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AutoScan();
            }
        });
        btnAutoScan.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return AutoScanCamera();
            }
        });

        btnSataliteMode = (com.github.clans.fab.FloatingActionButton) v.findViewById(R.id.btnOverlaySataliteMode);
        btnSataliteMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleMapType();
            }
        });
        btnCenterCamera = (com.github.clans.fab.FloatingActionButton) v.findViewById(R.id.btnOverlayCenterCamera);
        btnCenterCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnCenterCamera();
            }
        });
        btnCenterCamera.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return btnCenterAlways();
            }
        });
        btnMinimize = (ImageButton) v.findViewById(R.id.btnOverlayMinimize);
        btnMinimize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                minimizeOverlay();
            }
        });
        floatingActionMenu = (FloatingActionMenu) v.findViewById(R.id.floatOverlayActionMenu);
        try{
            mapView.onCreate(savedInstanceState);
        } catch (Exception e){
            e.printStackTrace();
        }

        // Gets to GoogleMap from the MapView and does initialization stuff
        mapView.getMapAsync(this);
        try{
            someFragment = this;
        } catch (Exception e){
            e.printStackTrace();
        }

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(getContext())
                .name(Realm.DEFAULT_REALM_NAME)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
        realm = Realm.getDefaultInstance();



        return v;
    }

    private void minimizeOverlay() {
        EventBus.getDefault().post(new MinimizeToggleEvent());
    }

    public void toggleMapType() {
        if(mMap != null) {
            if(mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                btnSataliteMode.setImageResource(R.drawable.ic_map_white_24dp);
            }
            else {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                btnSataliteMode.setImageResource(R.drawable.ic_satellite_white_24dp);
            }
        }
        floatingActionMenu.close(true);
    }

    @Override
    public void onStart(){
        super.onStart();
        EventBus.getDefault().register(this);

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
        isInOverlayMode = true;
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


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Object markerKey = null;
                for (Map.Entry<Pokemons, Marker> pokemonsMarkerEntry : pokemonsMarkerMap.entrySet()) {
                    if (pokemonsMarkerEntry.getValue().equals(marker)) {
                        markerKey = pokemonsMarkerEntry.getKey();
                        break;
                    }
                }
                if (markerKey == null) {
                    for (Map.Entry<Gym, Marker> gymMarkerEntry : gymMarkerMap.entrySet()) {
                        if (gymMarkerEntry.getValue().equals(marker)) {
                            markerKey = gymMarkerEntry.getKey();
                            break;
                        }
                    }
                }
                if (markerKey == null) {
                    for (Map.Entry<PokeStop, Marker> pokeStopMarkerEntry : pokestopMarkerMap.entrySet()) {
                        if (pokeStopMarkerEntry.getValue().equals(marker)) {
                            markerKey = pokeStopMarkerEntry.getKey();
                            break;
                        }
                    }
                }
                if(markerKey == null){
                    markerKey = marker;
                }

                if (markerKey != null) {
                    if (!Settings.get(getActivity()).isUseOldMapMarker()) {
                        removeAdapterAndListener();
                        MarkerDetails.showMarkerDetailsDialog(getActivity(), markerKey);
                    } else {
                        setAdapterAndListener(markerKey);
                        marker.showInfoWindow();
                    }
                }
                return false;
            }
        });

        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        MapsInitializer.initialize(this.getActivity());

        // Updates the location and zoom of the MapView
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(getCurrentLocation(),15);
        mMap.animateCamera(cameraUpdate);
        refreshGymsAndPokestops();
    }

    private void removeAdapterAndListener() {
        mMap.setInfoWindowAdapter(null);
        mMap.setOnInfoWindowClickListener(null);
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

    public boolean onLongClickSearch() {
        SettingsUtil.searchRadiusDialog(getContext());
        return true;
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
        SomeFragment.someFragment = null;
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
            MultiAccountLoader.SCANNING_STATUS = true;
        } else {
            removeCircleArray();
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progressBar.setVisibility(View.INVISIBLE);
            button.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_track_changes_white_24dp));
            MultiAccountLoader.SCANNING_STATUS = false;
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

    public int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    float tempOldProgress;
    int iprogressBar = 1;


    @TargetApi(Build.VERSION_CODES.M)
    public Circle createInitialCircle(LatLng pos){
        CircleOptions circleOptions = new CircleOptions()
                .radius(80)
                .strokeWidth(0)
                .fillColor(adjustAlpha(getActivity().getColor(R.color.YellowCircle), 0.5f))
                .center(pos);
        Circle circle = mMap.addCircle(circleOptions);
        circleArray.add(circle);
        return circle;
    }

    //@Subscribe(threadMode = ThreadMode.MAIN)
    @TargetApi(Build.VERSION_CODES.M)
    public synchronized void createCircle(LatLng pos, Circle oldCircle, boolean isBanned) {
        if (pos != null) {

            //SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            //int iprogressBar = mPrefs.getInt("progressbar", 1);

            /*if(event.isBanned){
                showToast(event.username +" maybe banned");
            }*/
            int color;
            if(!isBanned){
                color = adjustAlpha(getActivity().getColor(R.color.GreenCircle), 0.5f);
            } else {
               color = adjustAlpha(getActivity().getColor(R.color.RedCircle), 0.5f);
            }
            float progress = (float) iprogressBar * 100 / scanMap.size();
            System.out.println("progress: "+progress);
            progressBar.setProgress((int) progress);
            CircleOptions circleOptions = new CircleOptions()
                    .radius(80)
                    .strokeWidth(0)
                    .fillColor(color)
                    .center(pos);
            oldCircle.remove();
            circleArray.add(mMap.addCircle(circleOptions));
            if (progress >= 100) {
                removeCircleArray();
                showProgressbar(false);
            } else {
                showProgressbar(true);
            }

            tempOldProgress = progress;
            iprogressBar++;

        }
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

        gymstopRefresher = Observable.interval(5, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        //System.out.println("Refreshing Gyms");
                        refreshGymsAndPokestops();
                    }
                });
    }

    public boolean btnCenterAlways(){
        floatingActionMenu.close(true);
        if(CENTER_ALWAYS){
            CENTER_ALWAYS = false;
            btnCenterCamera.setColorNormal(ResourcesCompat.getColor(getResources(),R.color.colorPrimary,null));

        } else {
            CENTER_ALWAYS = true;
            btnCenterCamera.setColorNormal(ResourcesCompat.getColor(getResources(),R.color.colorAccent,null));
        }

        return true;
    }

    //Map related Functions
    public void refreshMap() {

        if (!MultiAccountLoader.areThreadsRunning()&&!MultiAccountLoader.autoScan) {
            showProgressbar(false);
        }

        if(CENTER_ALWAYS){
            moveCameraToCurrentPosition(true);
        }

        if (!LIST_MODE) {
            if (mMap != null) {
                LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
                createMapObjects();

                //Load our Pokemon Array
                ArrayList<Pokemons> pokemons = MapsActivity.getPokelist();
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


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean AutoScanCamera() {



        if(MultiAccountLoader.autoScan){
            MultiAccountLoader.autoScan = false;
        } else {
            MultiAccountLoader.autoScan = true;
        }
        if(MultiAccountLoader.autoScan) {
            btnAutoScan.setBackground(getActivity().getDrawable(R.drawable.circle_button_green));
            int SERVER_REFRESH_RATE = Settings.get(getActivity()).getServerRefresh();
            int scanValue = Settings.get(getActivity()).getScanValue();

            LatLng scanPosition = null;

            try {
                scanPosition = getCameraLocation();
            } catch (NoMapException | NoCameraPositionException e) {
                showToast(R.string.SCAN_FAILED);
                e.printStackTrace();
            }

            progressBar.setProgress(0);
            showProgressbar(true);


            if (scanPosition != null) {
                scanMap = makeHexScanMap(scanPosition, scanValue, 1, new ArrayList<LatLng>());
                if (scanMap != null) {
                    //Pull our users from the realm
                    ArrayList<User> users = new ArrayList<>(realm.copyFromRealm(realm.where(User.class).findAll()));

                    MultiAccountLoader.setSleepTime(UiUtils.BASE_DELAY * SERVER_REFRESH_RATE);
                    //Set our map
                    MultiAccountLoader.setScanMap(scanMap);
                    //MultiAccountLoader.cachedGo = new PokemonGo[40];
                    //Set our users
                    boolean inList = false;
                    ArrayList<String> usersToAddString = new ArrayList<>();
                    ArrayList<User> usersToAdd = new ArrayList<>();
                    ArrayList<PokemonGoWithUsername> pokemonGoWithUsernames = MultiAccountLoader.cachedGo;
                    for(PokemonGoWithUsername elem : MultiAccountLoader.cachedGo){
                        if(!elem.banned&&!elem.api.hasChallenge()){
                            usersToAddString.add(elem.username);
                        }
                    }
                    for(int i = 0;i<usersToAddString.size();i++){
                        for(int j = 0;j<users.size();j++){
                            if(usersToAddString.get(i).equals(users.get(j).getUsername())){
                                usersToAdd.add(users.get(j));
                                break;
                            }
                        }
                    }

                    if (usersToAdd.size() == 0) {
                        showToast(R.string.SCAN_FAILED);
                        showProgressbar(false);
                        return true;
                    }
                    MultiAccountLoader.setUsers(usersToAdd);
                    //Set GoogleWearAPI
                    //MultiAccountLoader.setmGoogleApiClient(mGoogleWearApiClient);
                    //Set Context
                    MultiAccountLoader.setContext(getActivity());
                    //Begin our threads???


                } else {
                    showToast(R.string.SCAN_FAILED);
                    showProgressbar(false);
                }
            } else {
                showToast(R.string.SCAN_FAILED);
                showProgressbar(false);
            }


            Intent intentService = new Intent(MapsActivity.instance, AutoScanService.class);
            MapsActivity.instance.bindService(intentService,MultiAccountLoader.mConnection,Context.BIND_AUTO_CREATE);
            //StartStopSendToWear(true, scanMap.size());
        } else {
            btnAutoScan.setBackground(getActivity().getDrawable(R.drawable.circle_button));
            Intent intentService = new Intent(getActivity(), AutoScanService.class);
            MapsActivity.instance.unbindService(MultiAccountLoader.mConnection);
            MultiAccountLoader.cancelAllThreads();
            //StartStopSendToWear(false, 1);
        }

        return true;
    }


    public void cleanPokemon(){
        if (mMap != null) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    MapsActivity.savePokelist(new ArrayList<Pokemons>());

                    pokemonsMarkerMap = new ArrayMap<Pokemons, Marker>();

                    mMap.clear();
                    // showToast(R.string.cleared_map);
                }
            });

            forceRefreshEvent(new ForceRefreshEvent());
            clearPokemonListOnWear();
        }
    }

    private void clearPokemonListOnWear(){
        //ArrayList<Pokemons> pokelist = new ArrayList<>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
        ArrayList<Pokemons> listout = new ArrayList<>();



        Gson gson = new Gson();
        String json = gson.toJson(listout,new TypeToken<ArrayList<Pokemons>>() {}.getType());
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/pokemonlist");
        putDataMapReq.getDataMap().putString("pokemons", json);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(MultiAccountLoader.mGoogleApiClient, putDataReq);
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
        if (MultiAccountLoader.SCANNING_STATUS) {
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

    private void stopPokeScan() {
        MultiAccountLoader.cancelAllThreads();
        if (!MultiAccountLoader.areThreadsRunning()) {
            showProgressbar(false);
        }
    }

    public LatLng getCameraLocation() throws NoMapException, NoCameraPositionException {
        if (mMap != null) {
            if (mMap.getCameraPosition() != null) {
                return mMap.getCameraPosition().target;
            }
            throw new NoCameraPositionException();
        }
        throw new NoMapException();
    }

    public void showToast(int resString) {
        Toast.makeText(getActivity(), getString(resString), Toast.LENGTH_SHORT).show();
    }

    public void showToast(String resString) {
        Toast.makeText(getActivity(), resString, Toast.LENGTH_SHORT).show();
    }

    public void PokeScan() {
        if (MultiAccountLoader.SCANNING_STATUS) {
            stopPokeScan();
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.putInt("progressbar",1);
            iprogressBar = 1;
            prefsEditor.commit();
            scanCurrentPosition = false;
            //StartStopSendToWear(false, 1);
        } else {
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.putInt("progressbar",1);
            iprogressBar = 1;
            prefsEditor.commit();
            //Progress Bar Related Stuff
            pos = 1;
            int SERVER_REFRESH_RATE = Settings.get(getContext()).getServerRefresh();

            System.out.println(SERVER_REFRESH_RATE);

            progressBar.setProgress(0);
            int scanValue = Settings.get(getContext()).getScanValue();
            showProgressbar(true);
            //get our camera position
            LatLng scanPosition = null;

            //Try to get the camera position
            try {
                scanPosition = getCameraLocation();
            } catch (NoMapException | NoCameraPositionException e) {
                showToast(R.string.SCAN_FAILED);
                e.printStackTrace();
            }

            if (SettingsUtil.getSettings(getActivity()).isDrivingModeEnabled() && moveCameraToCurrentPosition(false)) {
                scanPosition = getCurrentLocation();
            }

            if(scanCurrentPosition){
                scanPosition = getCurrentLocation();
                scanCurrentPosition = false;
            }


            if (scanPosition != null) {
                scanMap = makeHexScanMap(scanPosition, scanValue, 1, new ArrayList<LatLng>());
                if (scanMap != null) {
                    //Pull our users from the realm
                    ArrayList<User> users = new ArrayList<>(realm.copyFromRealm(realm.where(User.class).findAll()));

                    MultiAccountLoader.setSleepTime(UiUtils.BASE_DELAY * SERVER_REFRESH_RATE);
                    //Set our map
                    MultiAccountLoader.setScanMap(scanMap);
                    //Set our users
                    boolean inList = false;
                    ArrayList<String> usersToAddString = new ArrayList<>();
                    ArrayList<User> usersToAdd = new ArrayList<>();
                    ArrayList<PokemonGoWithUsername> pokemonGoWithUsernames = MultiAccountLoader.cachedGo;
                    for(PokemonGoWithUsername elem : MultiAccountLoader.cachedGo){
                        if(!elem.banned&&!elem.api.hasChallenge()){
                            usersToAddString.add(elem.username);
                        }
                    }
                    for(int i = 0;i<usersToAddString.size();i++){
                        for(int j = 0;j<users.size();j++){
                            if(usersToAddString.get(i).equals(users.get(j).getUsername())){
                                usersToAdd.add(users.get(j));
                                break;
                            }
                        }
                    }

                    if (usersToAdd.size() == 0) {
                        showToast(R.string.SCAN_FAILED);
                        showProgressbar(false);
                        return;
                    }
                    MultiAccountLoader.setUsers(usersToAdd);
                    //Set GoogleWearAPI
                    //MultiAccountLoader.setmGoogleApiClient(mGoogleWearApiClient);
                    //MultiAccountLoader.cachedGo = new PokemonGo[40];
                    //Set Context
                    MultiAccountLoader.setContext(getContext());
                    //Begin our threads???
                    MultiAccountLoader.startThreads();
                    //StartStopSendToWear(true, scanMap.size());
                } else {
                    showToast(R.string.SCAN_FAILED);
                    showProgressbar(false);
                }
            } else {
                showToast(R.string.SCAN_FAILED);
                showProgressbar(false);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void AutoScan() {

        if(MultiAccountLoader.autoScan){
            MultiAccountLoader.autoScan = false;
        } else {
            MultiAccountLoader.autoScan = true;
        }
        if(MultiAccountLoader.autoScan) {
            btnAutoScan.setBackground(getActivity().getDrawable(R.drawable.circle_button_blue));
            int SERVER_REFRESH_RATE = Settings.get(getActivity()).getServerRefresh();
            int scanValue = Settings.get(getActivity()).getScanValue();

            LatLng scanPosition = null;

            scanPosition = getCurrentLocation();

            progressBar.setProgress(0);
            showProgressbar(true);

            if (scanPosition != null) {
                scanMap = makeHexScanMap(scanPosition, scanValue, 1, new ArrayList<LatLng>());
                if (scanMap != null) {
                    //Pull our users from the realm
                    ArrayList<User> users = new ArrayList<>(realm.copyFromRealm(realm.where(User.class).findAll()));

                    MultiAccountLoader.setSleepTime(UiUtils.BASE_DELAY * SERVER_REFRESH_RATE);
                    //Set our map
                    MultiAccountLoader.setScanMap(scanMap);
                    //MultiAccountLoader.cachedGo = new PokemonGo[40];
                    //Set our users
                    boolean inList = false;
                    ArrayList<String> usersToAddString = new ArrayList<>();
                    ArrayList<User> usersToAdd = new ArrayList<>();
                    ArrayList<PokemonGoWithUsername> pokemonGoWithUsernames = MultiAccountLoader.cachedGo;
                    for(PokemonGoWithUsername elem : MultiAccountLoader.cachedGo){
                        if(!elem.banned&&!elem.api.hasChallenge()){
                            usersToAddString.add(elem.username);
                        }
                    }
                    for(int i = 0;i<usersToAddString.size();i++){
                        for(int j = 0;j<users.size();j++){
                            if(usersToAddString.get(i).equals(users.get(j).getUsername())){
                                usersToAdd.add(users.get(j));
                                break;
                            }
                        }
                    }

                    if (usersToAdd.size() == 0) {
                        showToast(R.string.SCAN_FAILED);
                        showProgressbar(false);
                        return;
                    }
                    MultiAccountLoader.setUsers(usersToAdd);
                    //Set GoogleWearAPI
                    //Set Context
                    MultiAccountLoader.setContext(getActivity());
                    //Begin our threads???


                } else {
                    showToast(R.string.SCAN_FAILED);
                    showProgressbar(false);
                }
            } else {
                showToast(R.string.SCAN_FAILED);
                showProgressbar(false);
            }

            Intent intentService = new Intent(getActivity(), AutoScanService.class);
            intentService.putExtra("mode",0);
            //startService(intentService);
            try{
                getActivity().bindService(intentService,MultiAccountLoader.mConnection,Context.BIND_AUTO_CREATE);
            } catch(Exception e){
                e.printStackTrace();
            }
            //StartStopSendToWear(true, scanMap.size());
        } else {
            btnAutoScan.setBackground(getActivity().getDrawable(R.drawable.circle_button));
            Intent intentService = new Intent(getActivity(), AutoScanService.class);
            try{
                getActivity().unbindService(MultiAccountLoader.mConnection);
                // stopService(intentService);
            } catch (Exception e){
                e.printStackTrace();
            }

            MultiAccountLoader.cancelAllThreads();
            //StartStopSendToWear(false, 1);
        }
    }



    private void setAdapterAndListener(final Object markerKey) {
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(final Marker marker) {
                LinearLayout info = new LinearLayout(getActivity());
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(getActivity());
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(getActivity());
                snippet.setTextColor(Color.GRAY);
                snippet.setGravity(Gravity.CENTER);
                if (markerKey instanceof Pokemons) {
                    Pokemons pokemons = ((Pokemons) markerKey);
                    snippet.setText(getActivity().getText(R.string.expires_in) + " " + DrawableUtils.getExpireTime(pokemons.getExpires(),pokemons.getFoundTime()) + "\n" + "Attack: " + pokemons.getIndividualAttack() + "\n" + "Defense: " + pokemons.getIndividualDefense() + "\n" + "Stamina: " + pokemons.getIndividualStamina());
                } else {
                    snippet.setText(marker.getSnippet());
                    info.addView(title);
                    info.addView(snippet);
                    return info;
                }

                TextView navigate = new TextView(getActivity());
                navigate.setTextColor(Color.GRAY);
                navigate.setGravity(Gravity.CENTER);
                navigate.setText(getText(R.string.click_open_in_gmaps));

                info.addView(title);
                info.addView(snippet);
                //info.addView(navigate);

                return info;
            }
        });

    }
}
