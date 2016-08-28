package com.pokescanner;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.multidex.MultiDex;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.pokescanner.events.ForceLogoutEvent;
import com.pokescanner.events.ForceRefreshEvent;
import com.pokescanner.events.InterruptedExecptionEvent;
import com.pokescanner.events.LoginFailedExceptionEvent;
import com.pokescanner.events.RemoteServerExceptionEvent;
import com.pokescanner.events.RestartRefreshEvent;
import com.pokescanner.events.ScanCircleEvent;
import com.pokescanner.exceptions.NoCameraPositionException;
import com.pokescanner.exceptions.NoMapException;
import com.pokescanner.helper.CustomMapFragment;
import com.pokescanner.helper.Generation;
import com.pokescanner.helper.GymFilter;
import com.pokescanner.helper.PokemonListLoader;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.objects.FilterItem;
import com.pokescanner.objects.Gym;
import com.pokescanner.objects.PokeStop;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.objects.User;
import com.pokescanner.service.AutoScanService;
import com.pokescanner.settings.Settings;
import com.pokescanner.settings.SettingsActivity;
import com.pokescanner.utils.DrawableUtils;
import com.pokescanner.utils.MarkerDetails;
import com.pokescanner.utils.PermissionUtils;
import com.pokescanner.utils.SettingsUtil;
import com.pokescanner.utils.UiUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static com.pokescanner.helper.Generation.makeHexScanMap;


//git clone --recursive -b Development https://github.com/Grover-c13/PokeGOAPI-Java.git && cd PokeGOAPI-Java && ./gradlew build


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraChangeListener,
        OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    @BindView(R.id.btnSearch)
    FloatingActionButton button;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    private GoogleMap mMap;
    @BindView(R.id.btnSettings)
    ImageButton btnSettings;
    @BindView(R.id.main)
    RelativeLayout main;
    @BindView(R.id.btnSataliteMode)
    com.github.clans.fab.FloatingActionButton btnSataliteMode;
    @BindView(R.id.floatActionMenu)
    FloatingActionMenu floatingActionMenu;
    @BindView(R.id.btnAutoScan)
    ImageButton btnAutoScan;
    @BindView(R.id.btnHeatMapMode)
    ImageButton btnHeatMapMode;
    boolean AutoScan = false;


    public AlertDialog builderHeatMap;


    LocationManager locationManager;
    public static MapsActivity instance;

    Context myContext = this;

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

    RelativeLayout rl;

    Polygon mBoundingHexagon = null;

    public boolean scanCurrentPosition = false;

    String TAG = "wear";

    int pos = 1;
    //Used for determining Scan status
    boolean SCANNING_STATUS = false;
    boolean LIST_MODE = false;
    //Used for our refreshing of the map
    Subscription pokeonRefresher;
    Subscription gymstopRefresher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        instance = this;

        mGoogleWearApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        // Now you can use the Data Layer API
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .build();

        MultiDex.install(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this)
                .name(Realm.DEFAULT_REALM_NAME)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
        realm = Realm.getDefaultInstance();

        //So if our realm has no users then we'll send our user back to the login screen
        //otherwise set our user and move on!
        if (realm.where(User.class).findAll().size() != 0) {
            user = realm.copyFromRealm(realm.where(User.class).findFirst());
        } else {
            Toast.makeText(MapsActivity.this, "No login!", Toast.LENGTH_SHORT).show();
            logOut();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (CustomMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Start our location manager so we can center our map
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            PokemonListLoader.populatePokemonList(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }


        /*Intent launchIntent = new Intent(this, MapsActivity.class);

        PendingIntent pIntent = PendingIntent.getActivity(MapsActivity.this, 0,   launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_track_changes_white_24dp)
                        .setLargeIcon(largeIcon)
                        .setContentTitle("Pokéscanner started")
                        .setContentText("No Pokémon nearby!")
                        .addAction(R.drawable.ic_refresh_white_36dp,"Start scan!", pIntent);
        mBuilder.setContentIntent(pIntent).setOngoing(true);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());*/
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

    @SuppressWarnings({"MissingPermission"})
    public LatLng getCurrentLocation() {
        if (PermissionUtils.doWeHaveGPSandLOC(this)) {
            if (mGoogleApiClient.isConnected()) {
                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location != null) {
                    return new LatLng(location.getLatitude(), location.getLongitude());
                }
                return null;
            }
            return null;
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @OnClick(R.id.btnAutoScan)
        public void AutoScan() {
        if(MultiAccountLoader.autoScan){
            MultiAccountLoader.autoScan = false;
        } else {
            MultiAccountLoader.autoScan = true;
        }
        if(MultiAccountLoader.autoScan) {
            btnAutoScan.setBackground(getDrawable(R.drawable.circle_button_blue));
            int SERVER_REFRESH_RATE = Settings.get(MapsActivity.this).getServerRefresh();
            int scanValue = Settings.get(MapsActivity.this).getScanValue();

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
                    //Set our users
                    MultiAccountLoader.setUsers(users);
                    //Set GoogleWearAPI
                    MultiAccountLoader.setmGoogleApiClient(mGoogleWearApiClient);
                    //Set Context
                    MultiAccountLoader.setContext(MapsActivity.this);
                    //Begin our threads???


                } else {
                    showToast(R.string.SCAN_FAILED);
                    showProgressbar(false);
                }
            } else {
                showToast(R.string.SCAN_FAILED);
                showProgressbar(false);
            }
            Intent intentService = new Intent(this, AutoScanService.class);
            intentService.putExtra("mode",0);
            startService(intentService);
            StartStopSendToWear(true, scanMap.size());
        } else {
            btnAutoScan.setBackground(getDrawable(R.drawable.circle_button));
            Intent intentService = new Intent(this, AutoScanService.class);
            stopService(intentService);
            MultiAccountLoader.cancelAllThreads();
            StartStopSendToWear(false, 1);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @OnLongClick(R.id.btnAutoScan)
    public boolean AutoScanCamera() {
        if(MultiAccountLoader.autoScan){
            MultiAccountLoader.autoScan = false;
        } else {
            MultiAccountLoader.autoScan = true;
        }
        if(MultiAccountLoader.autoScan) {
            btnAutoScan.setBackground(getDrawable(R.drawable.circle_button_green));
            int SERVER_REFRESH_RATE = Settings.get(MapsActivity.this).getServerRefresh();
            int scanValue = Settings.get(MapsActivity.this).getScanValue();

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
                    //Set our users
                    MultiAccountLoader.setUsers(users);
                    //Set GoogleWearAPI
                    MultiAccountLoader.setmGoogleApiClient(mGoogleWearApiClient);
                    //Set Context
                    MultiAccountLoader.setContext(MapsActivity.this);
                    //Begin our threads???


                } else {
                    showToast(R.string.SCAN_FAILED);
                    showProgressbar(false);
                }
            } else {
                showToast(R.string.SCAN_FAILED);
                showProgressbar(false);
            }
            Intent intentService = new Intent(this, AutoScanService.class);
            startService(intentService);
            StartStopSendToWear(true, scanMap.size());
        } else {
            btnAutoScan.setBackground(getDrawable(R.drawable.circle_button));
            Intent intentService = new Intent(this, AutoScanService.class);
            stopService(intentService);
            MultiAccountLoader.cancelAllThreads();
            StartStopSendToWear(false, 1);
        }

        return true;
    }




    @OnClick(R.id.btnSearch)
    public void PokeScan() {
        if (SCANNING_STATUS) {
            stopPokeScan();
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.putInt("progressbar",1);
            prefsEditor.commit();
            scanCurrentPosition = false;
            StartStopSendToWear(false, 1);
        } else {
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.putInt("progressbar",1);
            prefsEditor.commit();
            //Progress Bar Related Stuff
            pos = 1;
            int SERVER_REFRESH_RATE = Settings.get(this).getServerRefresh();

            System.out.println(SERVER_REFRESH_RATE);

            progressBar.setProgress(0);
            int scanValue = Settings.get(this).getScanValue();
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

            if (SettingsUtil.getSettings(MapsActivity.this).isDrivingModeEnabled() && moveCameraToCurrentPosition(false)) {
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
                    MultiAccountLoader.setUsers(users);
                    //Set GoogleWearAPI
                    MultiAccountLoader.setmGoogleApiClient(mGoogleWearApiClient);
                    //Set Context
                    MultiAccountLoader.setContext(this);
                    //Begin our threads???
                    MultiAccountLoader.startThreads();
                    StartStopSendToWear(true, scanMap.size());
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

    @OnClick(R.id.btnSettings)
    public void onSettingsClick() {
        Intent settingsIntent = new Intent(MapsActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void stopPokeScan() {
        MultiAccountLoader.cancelAllThreads();
        if (!MultiAccountLoader.areThreadsRunning()) {
            showProgressbar(false);
        }
    }

    @OnLongClick(R.id.btnSearch)
    public boolean onLongClickSearch() {
        SettingsUtil.searchRadiusDialog(this);
        return true;
    }

    public void showToast(int resString) {
        Toast.makeText(MapsActivity.this, getString(resString), Toast.LENGTH_SHORT).show();
    }

    public void showProgressbar(boolean status) {
        if (status) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progressBar.setVisibility(View.VISIBLE);
            button.setImageDrawable(ContextCompat.getDrawable(MapsActivity.this, R.drawable.ic_pause_white_24dp));
            SCANNING_STATUS = true;
        } else {
            removeCircleArray();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progressBar.setVisibility(View.INVISIBLE);
            button.setImageDrawable(ContextCompat.getDrawable(MapsActivity.this, R.drawable.ic_track_changes_white_24dp));
            SCANNING_STATUS = false;
        }
    }

    public void logOut() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(User.class).findAll().deleteAllFromRealm();
                realm.where(PokeStop.class).findAll().deleteAllFromRealm();
                realm.where(Pokemons.class).findAll().deleteAllFromRealm();
                realm.where(Gym.class).findAll().deleteAllFromRealm();
                Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
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
                        if (pokemon.isExpired()) {
                            if (UiUtils.isPokemonFiltered(pokemon) ||
                                    UiUtils.isPokemonExpiredFiltered(pokemon, this)) {
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
                                        marker = pokemon.updateMarker(marker, this);
                                    }
                                } else {
                                    //If our pokemon wasn't in our hashmap lets add him
                                    pokemonsMarkerMap.put(pokemon, mMap.addMarker(pokemon.getMarker(this)));
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

                if (SettingsUtil.getSettings(MapsActivity.this).isGymsEnabled()) {
                    for (int i = 0; i < gyms.size(); i++) {
                        Gym gym = gyms.get(i);
                        LatLng pos = new LatLng(gym.getLatitude(), gym.getLongitude());
                        if (curScreen.contains(pos) && !shouldGymBeRemoved(gym)) {
                            Marker marker = mMap.addMarker(gym.getMarker(this));
                            gymMarkerMap.put(gym, marker);
                        }
                    }
                }

                boolean showAllStops = !Settings.get(this).isShowOnlyLured();

                if (SettingsUtil.getSettings(MapsActivity.this).isPokestopsEnabled()) {
                    for (int i = 0; i < pokestops.size(); i++) {
                        PokeStop pokestop = pokestops.get(i);
                        LatLng pos = new LatLng(pokestop.getLatitude(), pokestop.getLongitude());
                        if (curScreen.contains(pos)) {
                            if (pokestop.isHasLureInfo() || showAllStops) {
                                Marker marker = mMap.addMarker(pokestop.getMarker(this));
                                pokestopMarkerMap.put(pokestop, marker);
                            }
                        }
                    }
                }
            }
        }
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

    public boolean shouldGymBeRemoved(Gym gym) {
        GymFilter currentGymFilter = GymFilter.getGymFilter(MapsActivity.this);
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

    public void createMapObjects() {
        if (SettingsUtil.getSettings(this).isBoundingBoxEnabled()) {
            createBoundingBox();
        } else {
            removeBoundingBox();
        }
    }

    public void startRefresher() {
        if (pokeonRefresher != null)
            pokeonRefresher.unsubscribe();
        if (gymstopRefresher != null)
            gymstopRefresher.unsubscribe();

        //Using RX java we setup an interval to refresh the map
        pokeonRefresher = Observable.interval(SettingsUtil.getSettings(this).getMapRefresh(), TimeUnit.SECONDS, AndroidSchedulers.mainThread())
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void createCircle(ScanCircleEvent event) {
        if (event.pos != null)
        {
            CircleOptions circleOptions = new CircleOptions()
                    .radius(80)
                    .strokeWidth(0)
                    .fillColor(ResourcesCompat.getColor(getResources(),R.color.colorPrimaryTransparent,null))
                    .center(event.pos);
            circleArray.add(mMap.addCircle(circleOptions));
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            int iprogressBar = mPrefs.getInt("progressbar",1);

            float progress = (float) iprogressBar * 100 / scanMap.size();
            progressBar.setProgress((int) progress);
            if((int) progress == 100) {
                showProgressbar(false);
            }
            if((int) progress>=100){
                removeCircleArray();
            }

        }
    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void forceRefreshEvent(ForceRefreshEvent event) {
        refreshGymsAndPokestops();
        refreshMap();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRestartRefreshEvent(RestartRefreshEvent event) {
        System.out.println(Settings.get(this).getServerRefresh());
        refreshGymsAndPokestops();
        refreshMap();
        startRefresher();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThreadInterruptedEvent(InterruptedExecptionEvent event) {
        showToast(R.string.ERROR_THREAD);
        showProgressbar(false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemoteServerFailedEvent(RemoteServerExceptionEvent event) {
        showToast(R.string.ERROR_SERVER);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUserAuthFailedEvent(LoginFailedExceptionEvent event) {
        showToast(R.string.ERROR_LOGIN);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onForceLogOutEvent(ForceLogoutEvent event) {
        showToast(R.string.ERROR_LOGIN);
        logOut();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pokemonsMarkerMap != null)
            pokemonsMarkerMap.clear();
        if (mMap != null)
            mMap.clear();
        forceRefreshEvent(new ForceRefreshEvent());
        onRestartRefreshEvent(new RestartRefreshEvent());
        realm = Realm.getDefaultInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(activityStarted==false) {
            mGoogleApiClient.connect();
            mGoogleWearApiClient.connect();

            EventBus.getDefault().register(this);
        }
        activityStarted = true;
    }

    @Override
    public void onStop() {

        EventBus.getDefault().unregister(this);
        mGoogleApiClient.disconnect();
        //mGoogleWearApiClient.disconnect();
        super.onStop();

        activityStarted = false;
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        pokeonRefresher.unsubscribe();
        gymstopRefresher.unsubscribe();
        super.onPause();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        createBoundingBox();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    @Override

    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

    @OnClick(R.id.btnListMode)
    public void listModeDialog() {
        floatingActionMenu.close(false);
        Intent listViewActivity = new Intent(this, ListViewActivity.class);
        try {
            LatLng cameraLocation = getCameraLocation();

            if (cameraLocation != null) {
                double[] locationOut = new double[2];
                locationOut[0] = cameraLocation.latitude;
                locationOut[1] = cameraLocation.longitude;

                listViewActivity.putExtra("cameraLocation",locationOut);
                startActivity(listViewActivity);
            }
        } catch (NoMapException e) {
            e.printStackTrace();
        } catch (NoCameraPositionException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.btnHeatMapMode)
    public void createHeatMap(){
        floatingActionMenu.close(false);
        /*File file = new File(getFilesDir(), "pokeList.txt");
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fin));
        String receiveString = "";
        StringBuilder stringBuilder = new StringBuilder();
*/
        LayoutInflater inflater = getLayoutInflater();


        if(builderHeatMap==null) {
            dialoglayoutHeatMap = inflater.inflate(R.layout.dialog_radiogroup, null);
            builderHeatMap = new AlertDialog.Builder(this).create();
            builderHeatMap.setView(dialoglayoutHeatMap);
        }
        if(radioGroupHeatMap==null){
            radioGroupHeatMap = (RadioGroup) dialoglayoutHeatMap.findViewById(R.id.radioGroupHeat);
        }

        if(radioGroupHeatMap.getChildCount()>2){
            builderHeatMap.show();
            return;
        }
        try {
            ArrayList<FilterItem> pokeList = PokemonListLoader.getPokelist(this);
        String uri;
            final float scale = getResources().getDisplayMetrics().density;
            int pixels = (int) (40 * scale + 0.5f);
        for(int i = 0;i<pokeList.size();i++){
            RadioButton radioButton = new RadioButton(this);


            radioButton.setText(pokeList.get(i).getFormalName(this));
            if (SettingsUtil.getSettings(this).isShuffleIcons()) {
                uri = "ps" + pokeList.get(i).getNumber();
            }
            else uri = "p" + pokeList.get(i).getNumber();

            int resourceID = getResources().getIdentifier(uri, "drawable", getPackageName());

            Drawable dr = ResourcesCompat.getDrawable(getResources(), resourceID, null);
            Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
            Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, pixels, pixels, true));


            radioButton.setCompoundDrawablesWithIntrinsicBounds(d,null,null,null);
            RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, pixels);
            radioButton.setHeight(pixels);
            radioButton.setLayoutParams(lp);
            radioGroupHeatMap.addView(radioButton);



        }

            if(radioButtonID==0){
                radioGroupHeatMap.check(R.id.radioButtonNoHeatmap);
            } else {
                radioGroupHeatMap.check(radioButtonID);
            }
            builderHeatMap.show();

            Button btnCancel = (Button) dialoglayoutHeatMap.findViewById(R.id.btnCancel);
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    builderHeatMap.cancel();
                }
            });

            Button btnDelete = (Button) dialoglayoutHeatMap.findViewById(R.id.btnDelete);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder;
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    //Yes button clicked
                                    File file = new File(getFilesDir(), "pokeList.txt");
                                    file.delete();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    };

                    builderDeleteFile = new AlertDialog.Builder(myContext);
                    builderDeleteFile.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                }
            });

            Button btnConfirm = (Button) dialoglayoutHeatMap.findViewById(R.id.btnAccept);
            btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ArrayList<LatLng> pokemonPosition = new ArrayList<LatLng>();
                    Gson gson = new Gson();

                    radioButtonID = radioGroupHeatMap.getCheckedRadioButtonId();
                    View radioButton = radioGroupHeatMap.findViewById(radioButtonID);
                    int selectedPokemon = radioGroupHeatMap.indexOfChild(radioButton);

                    if(selectedPokemon == 0){
                        if(mOverlay!=null){
                            mOverlay.remove();
                        }
                        builderHeatMap.dismiss();
                        return;
                    }

                    File file = new File(getFilesDir(), "pokeList.txt");
                    FileInputStream fin = null;
                    try {
                        fin = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fin));
                    String receiveString = "";
                    StringBuilder stringBuilder = new StringBuilder();

                    try {
                        while ( (receiveString = bufferedReader.readLine()) != null ) {
                            JsonReader reader = new JsonReader(new StringReader(receiveString));
                            reader.setLenient(true);
                            Pokemons pokemons = gson.fromJson(reader, new TypeToken<Pokemons>() {
                            }.getType());

                            if(pokemons.getNumber()==selectedPokemon){
                                pokemonPosition.add(new LatLng(pokemons.getLatitude(), pokemons.getLongitude()));
                            }
                        }

                        if(pokemonPosition.size()==0){
                            builderHeatMap.dismiss();
                            showToast(R.string.noPokeHeat);
                            return;
                        }

                        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                                .data(pokemonPosition)
                                .build();
                        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
                        builderHeatMap.dismiss();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.btnAddressSearch)
    public void searchAddressDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.dialog_search_address, null);
        final AlertDialog builder = new AlertDialog.Builder(this).create();

        final EditText etAddress = (EditText) dialoglayout.findViewById(R.id.etAddress);
        Button btnSearch = (Button) dialoglayout.findViewById(R.id.btnSearch);
        Button btnCancel = (Button) dialoglayout.findViewById(R.id.btnCancel);


        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String addy = etAddress.getText().toString();
                if (addy.length() > 0) {
                    Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocationName(addy, 10);
                        if (addresses != null) {
                            if (addresses.size() > 0) {
                                Address address = addresses.get(0);
                                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                                builder.dismiss();
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Address Search");
                        e.printStackTrace();
                    }
                } else {
                    etAddress.setError("Cannot be empty");
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
            }
        });

        builder.setView(dialoglayout);
        builder.show();
        floatingActionMenu.close(false);
    }
    @OnClick(R.id.btnSataliteMode)
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
    @SuppressWarnings({"MissingPermission"})
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        System.out.println("Map ready");
        if (PermissionUtils.doWeHaveGPSandLOC(this)) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            moveCameraToCurrentPosition(true);
        }
        mMap.setOnCameraChangeListener(this);
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                Pokemons deletePokemon = null;
                if (pokemonsMarkerMap.containsValue(marker)) {
                    for (Map.Entry<Pokemons, Marker> e : pokemonsMarkerMap.entrySet()) {
                        if (e.getValue().equals(marker)) {
                            deletePokemon = e.getKey();
                        }
                    }
                    if (deletePokemon != null) {
                        final Pokemons finalDeletePokemon = deletePokemon;
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                if (realm.where(Pokemons.class).equalTo("encounterid", finalDeletePokemon.getEncounterid()).findAll().deleteAllFromRealm()) {
                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    v.vibrate(50);
                                    pokemonsMarkerMap.get(finalDeletePokemon).remove();
                                    pokemonsMarkerMap.remove(finalDeletePokemon);
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

            }
        });
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
                if (markerKey != null) {
                    if (!Settings.get(MapsActivity.this).isUseOldMapMarker()) {
                        removeAdapterAndListener();
                        MarkerDetails.showMarkerDetailsDialog(MapsActivity.this, markerKey);
                    } else {
                        setAdapterAndListener(markerKey);
                        marker.showInfoWindow();
                    }
                }
                return false;
            }
        });
        startRefresher();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        cleanEntireMap();
    }
    public boolean moveCameraToCurrentPosition(boolean zoom) {
        LatLng GPS_LOCATION = getCurrentLocation();
        if (GPS_LOCATION != null) {
            if (mMap != null) {
                if (zoom || Settings.get(this).isDrivingModeEnabled()) {
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

    public void moveCameraToLocation(LatLng location){
        if (mMap != null) {

            this.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16));

        }
    }
    @OnClick(R.id.btnClear)
    public void cleanPokemon(){
        if (mMap != null) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.where(Pokemons.class).findAll().deleteAllFromRealm();

                    pokemonsMarkerMap = new ArrayMap<Pokemons, Marker>();

                    mMap.clear();
                    showToast(R.string.cleared_map);
                }
            });

            forceRefreshEvent(new ForceRefreshEvent());
            clearPokemonListOnWear();
        }
    }
    @OnLongClick(R.id.btnClear)
    public boolean cleanEntireMap(){
        if (mMap != null) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.where(Pokemons.class).findAll().deleteAllFromRealm();
                    realm.where(Gym.class).findAll().deleteAllFromRealm();
                    realm.where(PokeStop.class).findAll().deleteAllFromRealm();

                    pokemonsMarkerMap = new ArrayMap<Pokemons, Marker>();
                    pokestopMarkerMap = new ArrayMap<PokeStop, Marker>();
                    gymMarkerMap = new ArrayMap<Gym, Marker>();

                    mMap.clear();
                    showToast(R.string.cleared_map);

                }
            });
            forceRefreshEvent(new ForceRefreshEvent());

            clearPokemonListOnWear();
        }
        return true;
    }
    private void setAdapterAndListener(final Object markerKey) {
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(final Marker marker) {
                LinearLayout info = new LinearLayout(MapsActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(MapsActivity.this);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(MapsActivity.this);
                snippet.setTextColor(Color.GRAY);
                snippet.setGravity(Gravity.CENTER);
                if (markerKey instanceof Pokemons) {
                    snippet.setText(MapsActivity.this.getText(R.string.expires_in) + DrawableUtils.getExpireTime(((Pokemons) markerKey).getExpires()));
                } else {
                    snippet.setText(marker.getSnippet());
                }

                TextView navigate = new TextView(MapsActivity.this);
                navigate.setTextColor(Color.GRAY);
                navigate.setGravity(Gravity.CENTER);
                navigate.setText(getText(R.string.click_open_in_gmaps));

                info.addView(title);
                info.addView(snippet);
                info.addView(navigate);

                return info;
            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + marker.getPosition().latitude + "," + marker.getPosition().longitude + "(" + marker.getTitle() + ")");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(MapsActivity.this.getPackageManager()) != null) {
                    MapsActivity.this.startActivity(mapIntent);
                }
            }
        });
    }
    private void removeAdapterAndListener() {
        mMap.setInfoWindowAdapter(null);
        mMap.setOnInfoWindowClickListener(null);
    }

    private void clearPokemonListOnWear(){
        ArrayList<Pokemons> pokelist = new ArrayList<>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
        ArrayList<Pokemons> listout = new ArrayList<>();



        Gson gson = new Gson();
        String json = gson.toJson(listout,new TypeToken<ArrayList<Pokemons>>() {}.getType());
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/pokemonlist");
        putDataMapReq.getDataMap().putString("pokemons", json);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleWearApiClient, putDataReq);
    }

    private void StartStopSendToWear(boolean scanstatus, int scanmapsize){

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/startstopscan");
        putDataMapReq.getDataMap().putBoolean("scanstatus", scanstatus);
        putDataMapReq.getDataMap().putInt("scanmapsize", scanmapsize);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleWearApiClient, putDataReq);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.getStringExtra("methodName")==null)
            return;
        if(intent.getStringExtra("methodName").equals("newPokemon"))
        {
            Gson gson = new Gson();
            Pokemons newPokemon = gson.fromJson(intent.getStringExtra("pokemon"), new TypeToken<Pokemons>() {
            }.getType());
            LatLng location = new LatLng(newPokemon.getLatitude(), newPokemon.getLongitude());
            moveCameraToLocation(location);
        }
    }
}
