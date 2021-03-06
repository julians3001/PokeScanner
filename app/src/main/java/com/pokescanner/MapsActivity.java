package com.pokescanner;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.multidex.MultiDex;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.protobuf.InvalidProtocolBufferException;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.CaptchaActiveException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.exceptions.hash.HashException;
import com.pokescanner.events.ForceLogoutEvent;
import com.pokescanner.events.ForceRefreshEvent;
import com.pokescanner.events.InterruptedExecptionEvent;
import com.pokescanner.events.LoginFailedExceptionEvent;
import com.pokescanner.events.RemoteServerExceptionEvent;
import com.pokescanner.events.RestartRefreshEvent;
import com.pokescanner.exceptions.NoCameraPositionException;
import com.pokescanner.exceptions.NoMapException;
import com.pokescanner.helper.Generation;
import com.pokescanner.helper.GoMapPokemon;
import com.pokescanner.helper.GoMapPokemonList;
import com.pokescanner.helper.GymFilter;
import com.pokescanner.helper.PokemonListLoader;
import com.pokescanner.helper.SolveCaptchaHelper;
import com.pokescanner.loaders.LoginPTC;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.loaders.PokemonGoWithUsername;
import com.pokescanner.objects.FilterItem;
import com.pokescanner.objects.Gym;
import com.pokescanner.objects.PokeStop;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.objects.User;
import com.pokescanner.service.AutoScanService;
import com.pokescanner.service.SomeFragment;
import com.pokescanner.settings.Settings;
import com.pokescanner.settings.SettingsActivity;
import com.pokescanner.utils.DrawableUtils;
import com.pokescanner.utils.MarkerDetails;
import com.pokescanner.utils.PermissionUtils;
import com.pokescanner.utils.SettingsUtil;
import com.pokescanner.utils.UiUtils;
import com.zl.reik.dilatingdotsprogressbar.DilatingDotsProgressBar;

import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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


//git clone --recursive -b Development https://github.com/gegy1000/PokeGOAPI-Java.git && cd PokeGOAPI-Java && ./gradlew build


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraChangeListener,
        OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    @BindView(R.id.btnSearch)
    FloatingActionButton button;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.btnAddressSearch)
    com.github.clans.fab.FloatingActionButton btnAddressSearch;
    @BindView(R.id.btnListMode)
    com.github.clans.fab.FloatingActionButton btnListMode;
    @BindView(R.id.btnClear)
    ImageButton btnClear;
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
    @BindView(R.id.btnCenterCamera)
    com.github.clans.fab.FloatingActionButton btnCenterCamera;
    @BindView(R.id.btnOverlayActivity)
    ImageButton btnOverlayActivity;
    @BindView(R.id.tvLoggedInUsers)
    TextView tvLoggedInUsers;
    @BindView(R.id.tvTotalAmountOfUser)
    TextView tvTotalAmountOfUser;
    @BindView(R.id.btnSolveCaptchas)
    Button btnSolveCaptchas;
    boolean AutoScan = false;


    public AlertDialog builderHeatMap;
    public ArrayList<LatLng> pokemonPosition;
    public static String POKEMONFILENAME = "pokemonlist.json";

    public ArrayList<MarkerOptions> markerHeatList;


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

    AutoScanService mService;
    boolean mBound = false;

    RelativeLayout rl;

    Polygon mBoundingHexagon = null;

    public boolean scanCurrentPosition = false;

    public ArrayList<Pokemons> pokemonHeatList;

    public HeatmapTileProvider mHeatProvider;

    String TAG = "wear";


    int pos = 1;
    //Used for determining Scan status

    boolean LIST_MODE = false;
    boolean CENTER_ALWAYS = false;
    //Used for our refreshing of the map
    Subscription pokeonRefresher;
    Subscription gymstopRefresher;
    Realm realmDataBase;
    private PokemonGo result;
    private java.lang.String url;

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
        MultiAccountLoader.setmGoogleApiClient(mGoogleWearApiClient);
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

        createHeatMapList();

        //So if our realm has no users then we'll send our user back to the login screen
        //otherwise set our user and move on!
        /*if (realm.where(User.class).findAll().size() != 0) {
            user = realm.copyFromRealm(realm.where(User.class).findFirst());
        } else {
            Toast.makeText(MapsActivity.this, "No login!", Toast.LENGTH_SHORT).show();
            logOut();
        }*/

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
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

        if (getIntent() != null) {
            if (getIntent().getStringExtra("methodName") != null) {
                onNewIntent(getIntent());
            }
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

        if (MultiAccountLoader.autoScan) {
            MultiAccountLoader.autoScan = false;
        } else {
            MultiAccountLoader.autoScan = true;
        }
        if (MultiAccountLoader.autoScan) {
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
                    //MultiAccountLoader.cachedGo = new PokemonGo[40];
                    //Set our users
                    boolean inList = false;
                    ArrayList<String> usersToAddString = new ArrayList<>();
                    ArrayList<User> usersToAdd = new ArrayList<>();
                    ArrayList<PokemonGoWithUsername> pokemonGoWithUsernames = MultiAccountLoader.cachedGo;
                    for (PokemonGoWithUsername elem : MultiAccountLoader.cachedGo) {
                        if (!elem.banned && !elem.api.hasChallenge()) {
                            usersToAddString.add(elem.username);
                        }
                    }
                    for (int i = 0; i < usersToAddString.size(); i++) {
                        for (int j = 0; j < users.size(); j++) {
                            if (usersToAddString.get(i).equals(users.get(j).getUsername())) {
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
            intentService.putExtra("mode", 0);
            //startService(intentService);
            bindService(intentService, MultiAccountLoader.mConnection, Context.BIND_AUTO_CREATE);
            StartStopSendToWear(true, scanMap.size());
        } else {
            btnAutoScan.setBackground(getDrawable(R.drawable.circle_button));
            Intent intentService = new Intent(this, AutoScanService.class);
            try {
                unbindService(MultiAccountLoader.mConnection);
                // stopService(intentService);
            } catch (Exception e) {
                e.printStackTrace();
            }
            MultiAccountLoader.cancelAllThreads();
            StartStopSendToWear(false, 1);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @OnLongClick(R.id.btnAutoScan)
    public boolean AutoScanCamera() {


        if (MultiAccountLoader.autoScan) {
            MultiAccountLoader.autoScan = false;
        } else {
            MultiAccountLoader.autoScan = true;
        }
        if (MultiAccountLoader.autoScan) {
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
                    //MultiAccountLoader.cachedGo = new PokemonGo[40];
                    //Set our users
                    boolean inList = false;
                    ArrayList<String> usersToAddString = new ArrayList<>();
                    ArrayList<User> usersToAdd = new ArrayList<>();
                    ArrayList<PokemonGoWithUsername> pokemonGoWithUsernames = MultiAccountLoader.cachedGo;
                    for (PokemonGoWithUsername elem : MultiAccountLoader.cachedGo) {
                        if (!elem.banned && !elem.api.hasChallenge()) {
                            usersToAddString.add(elem.username);
                        }
                    }
                    for (int i = 0; i < usersToAddString.size(); i++) {
                        for (int j = 0; j < users.size(); j++) {
                            if (usersToAddString.get(i).equals(users.get(j).getUsername())) {
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
            bindService(intentService, MultiAccountLoader.mConnection, Context.BIND_AUTO_CREATE);
            StartStopSendToWear(true, scanMap.size());
        } else {
            btnAutoScan.setBackground(getDrawable(R.drawable.circle_button));
            Intent intentService = new Intent(this, AutoScanService.class);
            unbindService(MultiAccountLoader.mConnection);
            MultiAccountLoader.cancelAllThreads();
            StartStopSendToWear(false, 1);
        }

        return true;
    }


    @OnClick(R.id.btnOverlayActivity)
    public void startOverlayActivity() {
        floatingActionMenu.close(true);
        if (!checkDrawOverlayPermission()) {
            return;
        }
        Intent intent = new Intent(this, OverlayMapsActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @OnClick(R.id.btnSearch)
    public void PokeScan() {
        if (MultiAccountLoader.SCANNING_STATUS) {
            stopPokeScan();
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            iprogressBar = 1;
            prefsEditor.putInt("progressbar", 1);
            prefsEditor.commit();
            scanCurrentPosition = false;
            StartStopSendToWear(false, 1);
        } else {
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            iprogressBar = 1;
            prefsEditor.putInt("progressbar", 1);
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

            if (scanCurrentPosition) {
                scanPosition = getCurrentLocation();
                scanCurrentPosition = false;
            }

            //System.out.println("Bounds: " +bounds.northeast.latitude+", "+bounds.northeast.longitude+", "+bounds.southwest.latitude+", "+bounds.southwest.longitude);
            String test = "&w=6.482853474751764&e=7.0571951244914&n=51.39983340455366&s=51.040104762186175";
            LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
            getPokemonFromGomap(bounds.southwest.longitude, bounds.northeast.longitude, bounds.northeast.latitude, bounds.southwest.latitude);
            if (scanPosition != null) {
                scanMap = makeHexScanMap(scanPosition, scanValue, 1, new ArrayList<LatLng>());
                if (scanMap != null) {
                    //Pull our users from the realm
                    ArrayList<User> users = new ArrayList<>(realm.copyFromRealm(realm.where(User.class).findAll()));

                    MultiAccountLoader.setSleepTime(UiUtils.BASE_DELAY * SERVER_REFRESH_RATE);
                    //Set our map
                    MultiAccountLoader.setScanMap(scanMap);
                    //Set our users
                    boolean inList;
                    ArrayList<String> usersToAddString = new ArrayList<>();
                    ArrayList<User> usersToAdd = new ArrayList<>();
                    ArrayList<PokemonGoWithUsername> pokemonGoWithUsernames = MultiAccountLoader.cachedGo;
                    for (PokemonGoWithUsername elem : MultiAccountLoader.cachedGo) {
                        if (!elem.banned && !elem.api.hasChallenge()) {
                            usersToAddString.add(elem.username);
                        }
                    }
                    for (int i = 0; i < usersToAddString.size(); i++) {
                        for (int j = 0; j < users.size(); j++) {
                            if (usersToAddString.get(i).equals(users.get(j).getUsername())) {
                                usersToAdd.add(users.get(j));
                                break;
                            }
                        }
                    }

                    if (usersToAdd.size() == 0) {
                        showProgressbar(false);
                        return;
                    }
                    MultiAccountLoader.setUsers(usersToAdd);
                    //Set GoogleWearAPI
                    MultiAccountLoader.setmGoogleApiClient(mGoogleWearApiClient);
                    boolean createNewLogin = false;
                    /*for(int i = 0 ; i<Math.min(scanMap.size(),users.size());i++){
                        if(MultiAccountLoader.cachedGo[i]==null){
                            createNewLogin = true;
                        } else if(MultiAccountLoader.cachedGo[i].hasChallenge()){
                            MultiAccountLoader.cachedGo[i] = null;
                        }
                    }
                    if(createNewLogin){
                        MultiAccountLoader.cachedGo = new PokemonGo[40];
                        System.out.println("Created new Logins");
                    }*/
                    //Set Context
                    MultiAccountLoader.setContext(this);
                    //Begin our threads???
                    MultiAccountLoader.startThreads();
                    System.out.println("Threads gestartet");
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
        MultiAccountLoader.setContext(this);
        MultiAccountLoader.setmGoogleApiClient(mGoogleWearApiClient);
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

    public void showToast(String resString) {
        Toast.makeText(MapsActivity.this, resString, Toast.LENGTH_SHORT).show();
    }

    public void showProgressbar(boolean status) {
        if (status) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progressBar.setVisibility(View.VISIBLE);
            button.setImageDrawable(ContextCompat.getDrawable(MapsActivity.this, R.drawable.ic_pause_white_24dp));
            MultiAccountLoader.SCANNING_STATUS = true;
        } else {
            removeCircleArray();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            progressBar.setVisibility(View.INVISIBLE);
            button.setImageDrawable(ContextCompat.getDrawable(MapsActivity.this, R.drawable.ic_track_changes_white_24dp));
            MultiAccountLoader.SCANNING_STATUS = false;
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

    AlertDialog alertD;
    boolean webViewOpen;
    boolean captchaSolved;

    @OnClick(R.id.btnSolveCaptchas)
    public void solveCaptchas() {
        Intent intent = new Intent(this, SolveCaptchaActivity.class);

        startActivity(intent);


    }

    public void pokemonNotification(Pokemons pokemon) {
        Intent launchIntent = new Intent(this, MapsActivity.class);
        launchIntent.setAction(Long.toString(System.currentTimeMillis()));
        launchIntent.putExtra("methodName", "newPokemon");
        Gson gson = new Gson();
        String json = gson.toJson(pokemon, new TypeToken<Pokemons>() {
        }.getType());
        launchIntent.putExtra("pokemon", json);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_ONE_SHOT);

        Bitmap bitmap = DrawableUtils.getBitmapFromView(pokemon.getResourceID(this), "", this, DrawableUtils.PokemonType);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_refresh_white_36dp)
                        .setLargeIcon(bitmap)
                        .setContentTitle("New Pokémon nearby")
                        .setVibrate(new long[]{100, 100})
                        .setContentIntent(pIntent)
                        .setAutoCancel(true)
                        .setContentText(pokemon.getFormalName(this) + " (" + String.format("%.2f", pokemon.getIvInPercentage()) + "%) (" + DrawableUtils.getExpireTime(pokemon.getExpires(), pokemon.getFoundTime()) + ")");
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);
        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (pokemon.isNotExpired())
            mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
    }

    //Map related Functions
    public void refreshMap() {

        /*if (!MultiAccountLoader.areThreadsRunning() && !MultiAccountLoader.autoScan) {
            showProgressbar(false);
        }*/
        checkLoggedInUsers();

        if (MultiAccountLoader.challengeURLs.size() > 0) {
            btnSolveCaptchas.setVisibility(View.VISIBLE);
            btnSolveCaptchas.setText("Solve Captchas (" + MultiAccountLoader.challengeURLs.size() + ")");
        } else {
            btnSolveCaptchas.setVisibility(View.GONE);
        }

        if (CENTER_ALWAYS) {
            moveCameraToCurrentPosition(true);
        }

        if (!LIST_MODE) {
            if (mMap != null) {
                final LatLngBounds curScreen = mMap.getProjection().getVisibleRegion().latLngBounds;
                createMapObjects();

                //Load our Pokemon Array
                //ArrayList<Pokemons> pokemons = new ArrayList<Pokemons>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
                final ArrayList<Pokemons> pokemons = MapsActivity.getPokelist();
                ArrayList<Pokemons> pokemonsCollection = new ArrayList<>(pokemonsMarkerMap.keySet());

                for (int i = 0; i < pokemonsCollection.size(); i++) {
                    if (!pokemons.contains(pokemonsCollection.get(i))) {
                        if (pokemonsMarkerMap.get(pokemonsCollection.get(i)) != null)
                            pokemonsMarkerMap.get(pokemonsCollection.get(i)).remove();
                        pokemonsMarkerMap.remove(pokemonsCollection.get(i));
                        try {
                            System.out.println(pokemonsCollection.get(i).getFormalName(this) + " removed");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                //Okay so we're going to fix the annoying issue where the markers were being constantly redrawn
                if (!SettingsUtil.getSettings(this).isShowPokemon()) {
                    pokemonsMarkerMap = new ArrayMap<Pokemons, Marker>();

                    mMap.clear();

                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < pokemons.size(); i++) {

                            //Get our pokemon from the list
                            final Pokemons pokemon = pokemons.get(i);


                            //Is our pokemon contained within the bounds of the camera?
                            if (curScreen.contains(new LatLng(pokemon.getLatitude(), pokemon.getLongitude()))) {
                                //If yes then has he expired?
                                if (pokemon.isNotExpired()) {
                                    if (UiUtils.isPokemonFiltered(pokemon) ||
                                            UiUtils.isPokemonExpiredFiltered(pokemon, MapsActivity.instance)) {
                                        if (pokemonsMarkerMap.containsKey(pokemon)) {
                                            final Marker marker = pokemonsMarkerMap.get(pokemon);
                                            if (marker != null) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        marker.remove();
                                                    }
                                                });
                                                pokemonsMarkerMap.remove(pokemon);
                                            }
                                        }
                                    } else {
                                        //Okay finally is he contained within our hashmap?
                                        if (pokemonsMarkerMap.containsKey(pokemon)) {
                                            //Well if he is then lets pull out our marker.
                                            final Marker[] marker = {pokemonsMarkerMap.get(pokemon)};
                                            //Update the marker
                                            //UNTESTED
                                            /*if (marker[0] != null) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        marker[0] = pokemon.updateMarker(marker[0], MapsActivity.instance);
                                                    }
                                                });
                                            }  */
                                        } else {
                                            final MarkerOptions markerOptions = pokemon.getMarker(MapsActivity.instance);
                                            //If our pokemon wasn't in our hashmap lets add him
                                            MapsActivity.instance.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    pokemonsMarkerMap.put(pokemon, mMap.addMarker(markerOptions));
                                                }
                                            });

                                        }
                                    }
                                } else {
                                    //If our pokemon expired lets remove the marker

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (pokemonsMarkerMap.get(pokemon) != null)
                                                pokemonsMarkerMap.get(pokemon).remove();
                                        }
                                    });
                                    //Then remove the pokemon
                                    pokemonsMarkerMap.remove(pokemon);
                                    //Finally lets remove him from our realm.
                                }
                            } else {
                                //If our pokemon expired lets remove the marker

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (pokemonsMarkerMap.get(pokemon) != null)
                                            try {
                                                pokemonsMarkerMap.get(pokemon).remove();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                    }
                                });
                                //Then remove the pokemon
                                pokemonsMarkerMap.remove(pokemon);
                            }
                        }
                    }
                }).start();

            }
        }
    }

    private void checkLoggedInUsers() {
        ArrayList<User> users = new ArrayList<>(realm.copyFromRealm(realm.where(User.class).findAll()));
        tvTotalAmountOfUser.setText(users.size() + "");
        int loggedIn = 0;
        for (int i = 0; i < MultiAccountLoader.cachedGo.size(); i++) {
            if (MultiAccountLoader.cachedGo.get(i).api.isActive() && !MultiAccountLoader.cachedGo.get(i).api.hasChallenge()) {
                if (!MultiAccountLoader.cachedGo.get(i).banned) {
                    loggedIn++;
                }
            } else if (MultiAccountLoader.cachedGo.get(i).api.hasChallenge()) {
                MultiAccountLoader.challengeURLs.put(user.getUsername(), MultiAccountLoader.cachedGo.get(i).api.getChallengeURL());
            }
        }
        tvLoggedInUsers.setText(loggedIn + "");
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

        if (scanMap.size() > 0) {
            removeBoundingBox();

            List<LatLng> boundingPoints = Generation.getCorners(scanMap);
            PolygonOptions polygonOptions = new PolygonOptions();
            for (LatLng latLng : boundingPoints) {
                polygonOptions.add(latLng);
            }
            polygonOptions.strokeColor(Color.parseColor("#80d22d2d"));

            mBoundingHexagon = mMap.addPolygon(polygonOptions);
        }
    }

    public void removeBoundingBox() {
        if (mBoundingHexagon != null)
            mBoundingHexagon.remove();
    }

    public void removeCircleArray() {
        if (circleArray != null) {
            for (Circle circle : circleArray) {
                circle.remove();
            }

            circleArray.clear();
        }
        //removeBoundingBox();
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
        pokeonRefresher = Observable.interval(Settings.get(this).getMapRefresh(), TimeUnit.SECONDS, AndroidSchedulers.mainThread())
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

    float tempOldProgress;
    int iprogressBar = 1;


    @TargetApi(Build.VERSION_CODES.M)
    public Circle createInitialCircle(LatLng pos) {
        CircleOptions circleOptions = new CircleOptions()
                .radius(80)
                .strokeWidth(0)
                .fillColor(adjustAlpha(getColor(R.color.YellowCircle), 0.5f))
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
            if (!isBanned) {
                color = adjustAlpha(getColor(R.color.GreenCircle), 0.5f);
            } else {
                color = adjustAlpha(getColor(R.color.RedCircle), 0.5f);
            }
            float progress = (float) iprogressBar * 100 / scanMap.size();
            System.out.println("progress: " + progress);
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
                iprogressBar = 1;
                showProgressbar(false);
            } else {
                showProgressbar(true);
            }

            tempOldProgress = progress;
            iprogressBar++;

        }
    }


    public int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
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

    public static String convertStreamToString(FileInputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoginPTC.currentActivity = this;
        SomeFragment.isInOverlayMode = false;
        MultiAccountLoader.mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                AutoScanService.LocalBinder binder = (AutoScanService.LocalBinder) service;
                mService = binder.getService();
                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
        if (pokemonsMarkerMap != null)
            pokemonsMarkerMap.clear();
        if (mMap != null)
            //mMap.clear();
            forceRefreshEvent(new ForceRefreshEvent());
        onRestartRefreshEvent(new RestartRefreshEvent());
        realm = Realm.getDefaultInstance();
        checkIfShowPokemon();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (activityStarted == false) {
            mGoogleApiClient.connect();
            mGoogleWearApiClient.connect();

            EventBus.getDefault().register(this);
        }
        activityStarted = true;
    }

    @Override
    public void onStop() {

        EventBus.getDefault().unregister(this);
        //mGoogleApiClient.disconnect();
        //mGoogleWearApiClient.disconnect();
        super.onStop();

        activityStarted = false;
    }

    @Override
    protected void onDestroy() {
        realm.close();
        if (realmDataBase != null) {
            realmDataBase.close();
        }
        if (MultiAccountLoader.mConnection != null) {
            try {
                unbindService(MultiAccountLoader.mConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        //createBoundingBox();
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

                listViewActivity.putExtra("cameraLocation", locationOut);
                startActivity(listViewActivity);
            }
        } catch (NoMapException e) {
            e.printStackTrace();
        } catch (NoCameraPositionException e) {
            e.printStackTrace();
        }
    }

    private void openRealm() {
        if (!isExternalStorageReadable()) {
            return;
        }
        File file = new File(myContext.getFilesDir() + "/Pokescanner/Db/");

        if (!file.exists()) {
            boolean result = file.mkdirs();
            Log.e("TTT", "Results: " + result);
        }
        RealmConfiguration realmDatabaseConfiguration = new RealmConfiguration.Builder(file)
                .name("pokemondatabase" + ".realm")
                .build();
        realmDataBase = Realm.getInstance(realmDatabaseConfiguration);
    }

    @OnClick(R.id.btnHeatMapMode)
    public void createHeatMap() {
        floatingActionMenu.close(false);
        openRealm();
        builderHeatMap.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                realmDataBase.close();
            }
        });
        if (radioButtonID == 0) {
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

        btnCancel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!LIST_MODE) {

                    mMap.clear();
                    LIST_MODE = true;
                } else {
                    LIST_MODE = false;
                }
                return true;
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
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                realmDataBase.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        realm.where(Pokemons.class).findAll().deleteAllFromRealm();

                                    }
                                });
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

                final DilatingDotsProgressBar heatProgress = (DilatingDotsProgressBar) dialoglayoutHeatMap.findViewById(R.id.heatProgress);
                final Button heatProgressOverlay = (Button) dialoglayoutHeatMap.findViewById(R.id.heatProgressOverlay);

                heatProgressOverlay.setVisibility(View.VISIBLE);
                heatProgress.setVisibility(View.VISIBLE);
                heatProgress.show();

                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        final LatLng markerLatLng = latLng;
                        markerHeatList = new ArrayList<MarkerOptions>();

                        if (pokemonPosition == null || pokemonPosition.size() == 0) {
                            return;
                        }

                        cleanPokemon();
                        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mHeatProvider));

                        for (int i = 0; i < pokemonPosition.size(); i++) {

                            if (getDistance(markerLatLng, pokemonPosition.get(i)) < 200) {

                                String markerText = "";

                                if (pokemonHeatList.get(i).getFoundTime() == 0) {
                                    markerText = new DateTime(pokemonHeatList.get(i).getExpires() - 900000).toString(DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss"));
                                } else {
                                    markerText = new DateTime(pokemonHeatList.get(i).getFoundTime()).toString(DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")) + " (Found)";
                                }

                                MarkerOptions newMarkerOptions = new MarkerOptions()
                                        .draggable(true)
                                        .title(pokemonHeatList.get(i).getFormalName(myContext))
                                        .snippet(markerText)
                                        .position(pokemonPosition.get(i));

                                String title = newMarkerOptions.getSnippet();

                                for (int j = 0; j < markerHeatList.size(); j++) {
                                    if (markerHeatList.get(j).getPosition().equals(newMarkerOptions.getPosition())) {
                                        title = markerHeatList.get(j).getSnippet() + "\n" + newMarkerOptions.getSnippet();
                                        markerHeatList.remove(j);
                                    }
                                }
                                newMarkerOptions.snippet(title);

                                markerHeatList.add(newMarkerOptions);

                            }

                        }

                        for (int i = 0; i < markerHeatList.size(); i++) {
                            mMap.addMarker(markerHeatList.get(i));
                        }
                    }
                });

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        pokemonPosition = new ArrayList<LatLng>();
                        pokemonHeatList = new ArrayList<Pokemons>();
                        Gson gson = new Gson();

                        radioButtonID = radioGroupHeatMap.getCheckedRadioButtonId();
                        View radioButton = radioGroupHeatMap.findViewById(radioButtonID);
                        final int selectedPokemon = radioGroupHeatMap.indexOfChild(radioButton);

                        if (selectedPokemon == 0) {
                            if (mOverlay != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mOverlay.remove();
                                    }
                                });
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    heatProgressOverlay.setVisibility(View.INVISIBLE);
                                    heatProgress.setVisibility(View.INVISIBLE);
                                    heatProgress.hide();
                                }
                            });
                            builderHeatMap.dismiss();
                            return;
                        }

                        if (!isExternalStorageReadable()) {
                            return;
                        }


                        final Semaphore mutex = new Semaphore(0);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pokemonHeatList = new ArrayList<Pokemons>(realmDataBase.copyFromRealm(realmDataBase.where(Pokemons.class).equalTo("Number", selectedPokemon).findAll()));
                                mutex.release();
                            }
                        });
                        try {
                            mutex.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        for (Pokemons pokeElement : pokemonHeatList) {
                            pokemonPosition.add(new LatLng(pokeElement.getLatitude(), pokeElement.getLongitude()));
                        }

                        if (pokemonPosition.size() == 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    heatProgressOverlay.setVisibility(View.INVISIBLE);
                                    heatProgress.setVisibility(View.INVISIBLE);
                                    heatProgress.hide();
                                    if (mOverlay != null) {
                                        mOverlay.remove();
                                    }
                                    builderHeatMap.dismiss();
                                    showToast(R.string.noPokeHeat);
                                }
                            });

                            return;
                        }

                        mHeatProvider = new HeatmapTileProvider.Builder()
                                .data(pokemonPosition)
                                .build();
                        if (mOverlay != null) {
                            mHeatProvider.setData(pokemonPosition);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    mOverlay.remove();
                                    mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mHeatProvider));
                                    showToast(pokemonPosition.size() + " " + ((RadioButton) radioGroupHeatMap.getChildAt(selectedPokemon)).getText().toString().split(" ")[0] + " were found");
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mHeatProvider));
                                    showToast(pokemonPosition.size() + " " + ((RadioButton) radioGroupHeatMap.getChildAt(selectedPokemon)).getText().toString().split(" ")[0] + " were found");
                                }
                            });
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                heatProgressOverlay.setVisibility(View.INVISIBLE);
                                heatProgress.setVisibility(View.INVISIBLE);
                                heatProgress.hide();
                            }
                        });
                        builderHeatMap.dismiss();

                    }
                }).start();
            }
        });
        btnConfirm.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                final DilatingDotsProgressBar heatProgress = (DilatingDotsProgressBar) dialoglayoutHeatMap.findViewById(R.id.heatProgress);
                final Button heatProgressOverlay = (Button) dialoglayoutHeatMap.findViewById(R.id.heatProgressOverlay);
                final RadioGroup radioGroup = (RadioGroup) dialoglayoutHeatMap.findViewById(R.id.radioGroupHeat);
                final int[] numberOfPokemons = new int[252];

                heatProgressOverlay.setVisibility(View.VISIBLE);
                heatProgress.setVisibility(View.VISIBLE);
                heatProgress.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isExternalStorageReadable()) {
                            return;
                        }


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                realmDataBase.executeTransaction(new Realm.Transaction() {
                                    @Override
                                    public void execute(Realm realm) {
                                        for (int i = 1; i < numberOfPokemons.length; i++) {
                                            ArrayList<Pokemons> tempPokelist = new ArrayList<Pokemons>(realm.where(Pokemons.class).equalTo("Number", i).findAll());
                                            numberOfPokemons[i] = tempPokelist.size();
                                        }
                                    }
                                });
                            }
                        });


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 1; i < radioGroup.getChildCount(); i++) {

                                    RadioButton tempRadioButton = (RadioButton) radioGroup.getChildAt(i);
                                    tempRadioButton.setText(tempRadioButton.getText().toString().split(" ")[0] + " (" + numberOfPokemons[i] + ")");
                                }
                            }
                        });

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                heatProgressOverlay.setVisibility(View.INVISIBLE);
                                heatProgress.setVisibility(View.INVISIBLE);
                                heatProgress.hide();
                            }
                        });
                    }

                }).start();


                return true;
            }
        });


    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


    public float getDistance(LatLng arg1, LatLng arg2) {
        Location location1 = new Location("");
        location1.setLatitude(arg1.latitude);
        location1.setLongitude(arg1.longitude);

        Location location2 = new Location("");
        location2.setLatitude(arg2.latitude);
        location2.setLongitude(arg2.longitude);


        return location1.distanceTo(location2);
    }

    public void createHeatMapList() {
        LayoutInflater inflater = getLayoutInflater();


        if (builderHeatMap == null) {
            dialoglayoutHeatMap = inflater.inflate(R.layout.dialog_radiogroup, null);
            builderHeatMap = new AlertDialog.Builder(this).create();
            builderHeatMap.setView(dialoglayoutHeatMap);
        }
        if (radioGroupHeatMap == null) {
            radioGroupHeatMap = (RadioGroup) dialoglayoutHeatMap.findViewById(R.id.radioGroupHeat);
        }


        try {
            ArrayList<FilterItem> pokeList = PokemonListLoader.getPokelist(this);
            String uri;
            final float scale = getResources().getDisplayMetrics().density;
            int pixels = (int) (40 * scale + 0.5f);
            for (int i = 0; i < pokeList.size(); i++) {
                RadioButton radioButton = new RadioButton(this);


                radioButton.setText(pokeList.get(i).getFormalName(this));
                if (SettingsUtil.getSettings(this).isShuffleIcons()) {
                    uri = "p" + pokeList.get(i).getNumber();
                } else uri = "p" + pokeList.get(i).getNumber();

                int resourceID = getResources().getIdentifier(uri, "drawable", getPackageName());

                Drawable dr = ResourcesCompat.getDrawable(getResources(), resourceID, null);
                Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
                Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, pixels, pixels, true));


                radioButton.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
                RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, pixels);
                radioButton.setHeight(pixels);
                radioButton.setLayoutParams(lp);
                radioGroupHeatMap.addView(radioButton);


            }

            if (radioButtonID == 0) {
                radioGroupHeatMap.check(R.id.radioButtonNoHeatmap);
            } else {
                radioGroupHeatMap.check(radioButtonID);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
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

    boolean nightMode = false;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void checkIfShowPokemon() {
        if (SettingsUtil.getSettings(this).isShowPokemon()) {
            btnSataliteMode.setColorNormal(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
            btnSataliteMode.setImageDrawable(getDrawable(R.drawable.toggle_switch));
        } else {
            btnSataliteMode.setColorNormal(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
            btnSataliteMode.setImageDrawable(getDrawable(R.drawable.toggle_switch_off));
        }
    }


    @OnClick(R.id.btnSataliteMode)
    public void toggleMapType() {
        if (mMap != null) {

            /*if (nightMode) {
                mMap.setMapStyle(null);
                nightMode = false;
            } else {
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_style_night));
                nightMode = true;
            }*/
            SettingsUtil.showPokemon(this);
            checkIfShowPokemon();


            /*if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                btnSataliteMode.setImageResource(R.drawable.ic_map_white_24dp);
            } else {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                btnSataliteMode.setImageResource(R.drawable.ic_satellite_white_24dp);
            }*/
            //mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.maps_style_night));
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
                if (markerKey == null) {
                    markerKey = marker;
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
        loginIntoAccounts();
        startRefresher();
    }

    public void loginIntoAccounts() {
        ArrayList<User> users = new ArrayList<>(realm.copyFromRealm(realm.where(User.class).findAll()));
        for (final User elem : users) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PokemonGo go;
                    go = new LoginPTC().getPokemongo(elem);

                    final PokemonGoWithUsername goWithUsername = new PokemonGoWithUsername(elem.getUsername(), go);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MultiAccountLoader.cachedGo.add(goWithUsername);
                            System.out.println("Account added: " + goWithUsername.username);
                        }
                    });

                }
            }).start();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        System.out.println("onLowMemory Called!");
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                ArrayList<Pokemons> pokemons = new ArrayList<Pokemons>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
                realm.where(Pokemons.class).findAll().deleteAllFromRealm();
                for (int i = 0; i < pokemons.size(); i++) {
                    if (pokemons.get(i).isNotExpired()) {
                        realm.copyToRealm(pokemons.get(i));
                    } else {
                        System.out.println(pokemons.get(i).getFormalName(myContext) + " has been removed");
                    }
                }


            }
        });
    }

    public String pokemonGomapString;

    public void getPokemonFromGomap(double w, double e, double n, double s) {
        ArrayList<Pokemons> pokemonsArrayList = new ArrayList<>();
        String param = "&w=" + w + "&e=" + e + "&n=" + n + "&s=" + s;
        System.out.println("Param: " + param);
        final String sUrl = "http://148.251.192.149/m.php?mid=0&ex=%5B14%2C17%2C37%2C52%2C54%2C60%2C69%2C79%2C90%2C100%2C116%2C120%2C124%2C125%2C129%2C133%2C162%2C164%2C190%2C220%2C221%2C223%5D" + param;
        AsyncTask asyncTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPostExecute(Void aVoid) {

            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    URL url = new URL(sUrl);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader r = new BufferedReader(new InputStreamReader(in));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }
                    pokemonGomapString = total.toString();
                    GoMapPokemonList goMapPokemonList = new Gson().fromJson(pokemonGomapString, GoMapPokemonList.class);
                    ArrayList<Pokemons> pokemonsArrayList1 = getPokelist();
                    int counter = 0;
                    for (GoMapPokemon elem : goMapPokemonList.pokemons) {


                        Pokemons pokemons = new Pokemons(elem);
                        if (!pokemonsArrayList1.contains(pokemons)) {
                            counter++;
                            pokemons.setName(pokemons.getFormalName(MapsActivity.instance));
                            pokemonsArrayList1.add(pokemons);
                            if (UiUtils.isPokemonNotification(pokemons)) {
                                pokemonNotification(pokemons);
                            }
                        }
                    }
                    final int finalCounter = counter;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            showToast(finalCounter + " Pokémon added from Gomap");
                        }
                    });
                    savePokelist(pokemonsArrayList1);
                    System.out.println("Gomap Pokemon Hinzugefügt");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

    }

    public boolean moveCameraToCurrentPosition(boolean zoom) {
        LatLng GPS_LOCATION = getCurrentLocation();
        if (GPS_LOCATION != null) {
            if (mMap != null) {
                if (zoom || Settings.get(this).isDrivingModeEnabled()) {
                    this.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(GPS_LOCATION, 15));
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

    public void moveCameraToLocation(LatLng location, GoogleMap mMap) {
        if (mMap != null) {

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16));

        }
    }

    @OnClick(R.id.btnClear)
    public void cleanPokemon() {

        savePokelist(new ArrayList<Pokemons>());

        pokemonsMarkerMap = new ArrayMap<Pokemons, Marker>();

        mMap.clear();

        forceRefreshEvent(new ForceRefreshEvent());
        clearPokemonListOnWear();

    }

    public void cleanPokemon(long encounterid) {
        final long e = encounterid;
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(Pokemons.class).equalTo("encounterid", e).findAll().deleteAllFromRealm();
            }
        });
    }

    @OnClick(R.id.btnCenterCamera)
    public void btnCenterCamera() {
        floatingActionMenu.close(true);
        moveCameraToCurrentPosition(true);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @OnLongClick(R.id.btnCenterCamera)
    public boolean btnCenterAlways() {
        floatingActionMenu.close(true);
        if (CENTER_ALWAYS) {
            CENTER_ALWAYS = false;
            btnCenterCamera.setColorNormal(ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));

        } else {
            CENTER_ALWAYS = true;
            btnCenterCamera.setColorNormal(ResourcesCompat.getColor(getResources(), R.color.colorAccent, null));
        }

        return true;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public final static int REQUEST_CODE = 12345;

    @TargetApi(Build.VERSION_CODES.M)
    public boolean checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        if (!android.provider.Settings.canDrawOverlays(this)) {
            /** if not construct intent to request permission */
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            /** request permission via start activity for result */
            startActivityForResult(intent, REQUEST_CODE);
            return false;
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /** check if received result code
         is equal our requested code for draw permission  */
        if (requestCode == REQUEST_CODE) {
            /** if so check once again if we have permission */
            if (android.provider.Settings.canDrawOverlays(this)) {
                // continue here - permission was granted
            }
        }
    }

    @OnLongClick(R.id.btnClear)
    public boolean cleanEntireMap() {
        if (mMap != null) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    savePokelist(new ArrayList<Pokemons>());
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
                    Pokemons pokemons = ((Pokemons) markerKey);
                    Format formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                    snippet.setText(MapsActivity.this.getText(R.string.expires_in) + " " + formatter.format(new Date(pokemons.getExpires())) + "\n" + "Attack: " + pokemons.getIndividualAttack() + "\n" + "Defense: " + pokemons.getIndividualDefense() + "\n" + "Stamina: " + pokemons.getIndividualStamina());
                } else {
                    snippet.setText(marker.getSnippet());
                    info.addView(title);
                    info.addView(snippet);
                    return info;
                }

                TextView navigate = new TextView(MapsActivity.this);
                navigate.setTextColor(Color.GRAY);
                navigate.setGravity(Gravity.CENTER);
                navigate.setText(getText(R.string.click_open_in_gmaps));

                info.addView(title);
                info.addView(snippet);
                //info.addView(navigate);

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

    public static ArrayList<Pokemons> getPokelist() {
        String pokelistjson = "";
        try {
            FileInputStream fileInputStream = MapsActivity.instance.openFileInput(MapsActivity.POKEMONFILENAME);
            pokelistjson = MapsActivity.convertStreamToString(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ArrayList<Pokemons> pokelist = (new Gson()).fromJson(pokelistjson, new TypeToken<ArrayList<Pokemons>>() {
        }.getType());
        if (pokelist == null) {
            return new ArrayList<>();
        }
        return pokelist;
    }

    public static void savePokelist(ArrayList<Pokemons> pokelist) {
        FileOutputStream fos;
        try {
            String string = (new Gson()).toJson(pokelist);
            fos = MapsActivity.instance.openFileOutput(MapsActivity.POKEMONFILENAME, Context.MODE_PRIVATE);

            fos.write(string.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeAdapterAndListener() {
        mMap.setInfoWindowAdapter(null);
        mMap.setOnInfoWindowClickListener(null);
    }

    private void clearPokemonListOnWear() {
        ArrayList<Pokemons> pokelist = new ArrayList<>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
        ArrayList<Pokemons> listout = new ArrayList<>();


        Gson gson = new Gson();
        String json = gson.toJson(listout, new TypeToken<ArrayList<Pokemons>>() {
        }.getType());
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/pokemonlist");
        putDataMapReq.getDataMap().putString("pokemons", json);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleWearApiClient, putDataReq);
    }

    private void StartStopSendToWear(boolean scanstatus, int scanmapsize) {

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/startstopscan");
        putDataMapReq.getDataMap().putBoolean("scanstatus", scanstatus);
        putDataMapReq.getDataMap().putInt("scanmapsize", scanmapsize);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleWearApiClient, putDataReq);
    }

    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                return false;
                //throw new PackageManager.NameNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            context.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getStringExtra("methodName") == null)
            return;


        if (intent.getStringExtra("methodName").equals("newPokemon")) {
            Gson gson = new Gson();
            Pokemons newPokemon = gson.fromJson(intent.getStringExtra("pokemon"), new TypeToken<Pokemons>() {
            }.getType());
            LatLng location = new LatLng(newPokemon.getLatitude(), newPokemon.getLongitude());

            if (SomeFragment.someFragment != null) {
                GoogleMap gMap = SomeFragment.someFragment.getFragmentMap();
                moveCameraToLocation(location, gMap);

                /*Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);*/
                openApp(this, "com.nianticlabs.pokemongo");
                return;
            }
            moveCameraToLocation(location, mMap);
        }
    }
}
