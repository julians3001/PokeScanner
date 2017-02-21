package com.pokescanner.loaders;

import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.transition.Scene;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.InvalidProtocolBufferException;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.listener.LoginListener;
import com.pokegoapi.api.map.Map;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.encounter.EncounterResult;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.AsyncPokemonGoException;
import com.pokegoapi.exceptions.CaptchaActiveException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.exceptions.hash.HashException;
import com.pokegoapi.util.CaptchaSolveHelper;
import com.pokegoapi.util.hash.HashProvider;
import com.pokescanner.MapsActivity;
import com.pokescanner.R;
import com.pokescanner.events.ForceLogoutEvent;
import com.pokescanner.events.ScanCircleEvent;
import com.pokescanner.helper.PokemonListLoader;
import com.pokescanner.objects.FilterItem;
import com.pokescanner.objects.Gym;
import com.pokescanner.objects.PokeStop;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.objects.User;
import com.pokescanner.utils.DrawableUtils;
import com.pokescanner.utils.UiUtils;

import org.greenrobot.eventbus.EventBus;
import org.junit.rules.Stopwatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import POGOProtos.Map.Fort.FortDataOuterClass;
import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import okhttp3.OkHttpClient;

public class ObjectLoaderPTC extends Thread {
    User user;
    List<LatLng> scanMap;
    int SLEEP_TIME;
    private Realm realm;
    int position;
    GoogleApiClient mGoogleWearApiClient;
    Context context;
    int progressBar;
    ArrayList<Pokemons> listout;
    Realm realmDataBase;
    PokemonGo go;
    String url;
    boolean captchaSolved = false;
    LoginListener loginListener;


    public ObjectLoaderPTC(User user, List<LatLng> scanMap, int SLEEP_TIME, int pos, GoogleApiClient mGoogleWearApiClient, Context context) {
        this.user = user;
        this.scanMap = scanMap;
        this.SLEEP_TIME = SLEEP_TIME;
        this.position = pos;
        this.mGoogleWearApiClient = mGoogleWearApiClient;
        this.context = context;
    }

    @Override
    public void run() {
        try {


            CredentialProvider provider = null;

            final boolean[] successfulllogin = {false};
            if (MultiAccountLoader.cachedGo[position] == null) {
                OkHttpClient client = new OkHttpClient();
                //Create our provider and set it to null
                MultiAccountLoader.cachedGo[position] = new PokemonGo(client);
                loginListener = new LoginListener() {
                    @Override
                    public void onLogin(PokemonGo api) {
                        System.out.println("Successfully logged in with SolveCaptchaExample! "+user.getUsername());
                        successfulllogin[0] = true;
                    }

                    @Override
                    public void onChallenge(PokemonGo api, String challengeURL) {
                        System.out.println("Captcha received "+user.getUsername()+"! URL: " + challengeURL);
                        url = challengeURL;
                        MapsActivity.instance.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity.instance);
                                alert.setTitle("Captcha "+user.getUsername());

                                WebView wv = new WebView(MapsActivity.instance.getApplicationContext());
                                wv.getSettings().setJavaScriptEnabled(true);
                                wv.loadUrl(url);
                                wv.setWebViewClient(new WebViewClient() {
                                    @Override
                                    public boolean shouldOverrideUrlLoading(WebView view, String url) {

                                        if (url.startsWith("unity:")) {
                                            // magic
                                            String token = url.toString().split(Pattern.quote(":"))[1];
                                            System.out.println(token);
                                            try {
                                                if (MultiAccountLoader.cachedGo[position].verifyChallenge(token)) {
                                                    System.out.println("Captcha was correctly solved!");
                                                    captchaSolved = true;
                                                } else {
                                                    System.out.println("Captcha was incorrectly solved! Please try again.");
                                            /*
                                                Ask for a new challenge url, don't need to check the result,
                                                because the LoginListener will be called when this completed.
                                            */
                                                    //MultiAccountLoader.cachedGo[position].checkChallenge();
                                                }
                                            } catch (RemoteServerException e) {
                                                e.printStackTrace();
                                            } catch (CaptchaActiveException e) {
                                                e.printStackTrace();
                                            } catch (LoginFailedException e) {
                                                e.printStackTrace();
                                            } catch (InvalidProtocolBufferException e) {
                                                e.printStackTrace();
                                            } catch (HashException e) {
                                                e.printStackTrace();
                                            }
                                            return true;
                                        } else {
                                            view.loadUrl(url);

                                            return true;
                                        }
                                    }

                                });

                                alert.setView(wv);
                                alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                });
                                alert.show();
                            }
                        });

                        //Todo solve captcha: api.verifyChallenge(token)
                        //completeCaptcha(api, challengeURL);
                    }
                };
                MultiAccountLoader.cachedGo[position].addListener(loginListener);
                //Is our user google or PTC?
                if (user.getAuthType() == User.GOOGLE) {
                    if (user.getToken() != null) {
                        provider = new GoogleUserCredentialProvider(client, user.getToken().getRefreshToken());
                    } else {
                        EventBus.getDefault().post(new ForceLogoutEvent());
                    }
                } else {
                    provider = new PtcCredentialProvider(client, user.getUsername(), user.getPassword());
                }

                if (provider != null) {
                    HashProvider hasher = AuthAccountsLoader.getHashProvider();

                    MultiAccountLoader.cachedGo[position].login(provider, hasher);

                }

                while (!successfulllogin[0]){

                }
            }
            boolean test = MultiAccountLoader.cachedGo[position].hasChallenge();
            while(test){
                Thread.sleep(5000);
                if(captchaSolved){
                    HashProvider hasher = AuthAccountsLoader.getHashProvider();
                    MultiAccountLoader.cachedGo[position].login(provider, hasher);
                }
                test = MultiAccountLoader.cachedGo[position].hasChallenge();

            }
            go = MultiAccountLoader.cachedGo[position];
            if (go == null) {
                return;
            }

            int scanPos = 0;


            if (go != null) {
                System.out.println("Erfolgreich eingeloggt: " + user.getUsername());
                MultiAccountLoader.cachedGo[position].removeListener(loginListener);
                for (LatLng pos : scanMap) {
                    go.setLocation(pos.latitude,pos.longitude,Math.random() * 15.0);
                    go.getMap().awaitUpdate();
                    if(MultiAccountLoader.cancelThreads){
                        return;
                    }
                    final Collection<CatchablePokemon> catchablePokemon = go.getMap().getMapObjects().getPokemon();;

                    final ArrayList<com.pokegoapi.api.gym.Gym> collectionGyms = new ArrayList<>(go.getMap().getMapObjects().getGyms());
                    final Collection<Pokestop> collectionPokeStops = go.getMap().getMapObjects().getPokestops();
                    boolean isBanned = go.getMap().getMapObjects().getNearby().size() <= 0;
                    System.out.println(user.getUsername() + " is banned: "+isBanned);

                    EventBus.getDefault().post(new ScanCircleEvent(pos, isBanned,user.getUsername(), user.getAccountColor()));
                    final ArrayList<EncounterResult> encounterResults = new ArrayList<>();
                    //long starttime = System.currentTimeMillis();
                    for(final CatchablePokemon pokemonOut : catchablePokemon){
                        if(MultiAccountLoader.cancelThreads){
                            return;
                        }
                        EncounterResult encResult = null;
                        try {
                            encResult = pokemonOut.encounterPokemon();
                        } catch (LoginFailedException e) {
                            e.printStackTrace();
                        } catch (RemoteServerException e) {
                            e.printStackTrace();
                        }
                        encounterResults.add(encResult);

                        final Pokemons pokemon = new Pokemons(pokemonOut);
                        MapsActivity.instance.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MapsActivity.instance.cleanPokemon(pokemon.getEncounterid());
                            }
                        });

                    }

                    realm = Realm.getDefaultInstance();
                    //System.out.println("Realm start: "+user.getUsername());
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            for (CatchablePokemon pokemonOut : catchablePokemon) {
                                if(MultiAccountLoader.cancelThreads){
                                    return;
                                }


                                Pokemon pokemonEncounter = new Pokemon(go, encounterResults.get(0).getPokemonData());
                                encounterResults.remove(0);
                                long currentTime = System.currentTimeMillis();
                                Pokemons pokemon = new Pokemons(pokemonOut);


                                pokemon.setIvInPercentage(pokemonEncounter.getIvInPercentage());
                                pokemon.setIndividualAttack(pokemonEncounter.getIndividualAttack());
                                pokemon.setIndividualDefense(pokemonEncounter.getIndividualDefense());
                                pokemon.setIndividualStamina(pokemonEncounter.getIndividualStamina());
                                pokemon.setFoundTime(currentTime);
                                ArrayList<Pokemons> pokelist = new ArrayList<>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
                                boolean alreadyNotificated = false;

                                if (pokemon.getExpires() < 0) {

                                   // pokemon.setExpires(currentTime + 900000);

                                    for (Pokemons allPokemon : pokelist) {
                                        if (allPokemon.getEncounterid() == pokemon.getEncounterid()) {
                                            alreadyNotificated = true;
                                            break;
                                        }
                                    }
                                }

                                try {
                                    if (!pokelist.contains(pokemon)) {
                                        savePokemonToFile(pokemon);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                realm.copyToRealmOrUpdate(pokemon);
                                if (UiUtils.isPokemonNotification(pokemon) && !pokelist.contains(pokemon) && !alreadyNotificated) {


                                    Intent launchIntent = new Intent(context, MapsActivity.class);
                                    launchIntent.setAction(Long.toString(System.currentTimeMillis()));
                                    launchIntent.putExtra("methodName", "newPokemon");
                                    Gson gson = new Gson();
                                    String json = gson.toJson(pokemon, new TypeToken<Pokemons>() {
                                    }.getType());
                                    launchIntent.putExtra("pokemon", json);
                                    PendingIntent pIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_ONE_SHOT);

                                    Bitmap bitmap = DrawableUtils.getBitmapFromView(pokemon.getResourceID(context), "", context, DrawableUtils.PokemonType);
                                    NotificationCompat.Builder mBuilder =
                                            new NotificationCompat.Builder(context)
                                                    .setSmallIcon(R.drawable.ic_refresh_white_36dp)
                                                    .setLargeIcon(bitmap)
                                                    .setContentTitle("New Pokémon nearby")
                                                    .setVibrate(new long[]{100, 100})
                                                    .setContentIntent(pIntent)
                                                    .setAutoCancel(true)
                                                    .setContentText(pokemon.getFormalName(context) + " (" + pokemon.getIvInPercentage() + "%) (" + DrawableUtils.getExpireTime(pokemon.getExpires(),pokemon.getFoundTime()) + ")");
                                    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                    mBuilder.setSound(alarmSound);
                                    NotificationManager mNotificationManager =
                                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                    if (pokemon.isNotExpired())
                                        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
                                }
                            }

                            for (com.pokegoapi.api.gym.Gym gymOut : collectionGyms)
                                realm.copyToRealmOrUpdate(new Gym(gymOut));

                            for (Pokestop pokestopOut : collectionPokeStops)
                                realm.copyToRealmOrUpdate(new PokeStop(pokestopOut));
                        }
                    });
                    //System.out.println("Realm end: "+user.getUsername());
                    SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor prefsEditor = mPrefs.edit();
                    progressBar = mPrefs.getInt("progressbar", 0);
                    progressBar++;
                    prefsEditor.putInt("progressbar", progressBar);
                    prefsEditor.commit();


                    sendPokemonListToWear();

                    realm.close();
                    Thread.sleep(SLEEP_TIME);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("InterruptedException: " + user.getUsername());
        } catch (RemoteServerException e) {
            e.printStackTrace();
            System.out.println("RemoteServerException: " + user.getUsername());
            this.run();
        } catch (LoginFailedException e) {
            e.printStackTrace();
            System.out.println("LoginFailedException: " + user.getUsername());
            this.run();
        } catch (AsyncPokemonGoException e) {
            e.printStackTrace();
            MultiAccountLoader.cachedGo[position] = null;

            System.out.println("AsyncPokemonGo: " + user.getUsername());
            this.run();
        } catch (HashException e) {
            e.printStackTrace();
        } catch (CaptchaActiveException e) {
            e.printStackTrace();
        }
    }

    private void savePokemonToFile(final Pokemons pokemons) throws IOException {

        if (!isExternalStorageWritable()) {
            return;
        }
        openRealm();
        realmDataBase.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realmDataBase) {
                ArrayList<Pokemons> pokelist = new ArrayList<>(realmDataBase.copyFromRealm(realmDataBase.where(Pokemons.class).findAll()));
                if (!pokelist.contains(pokemons)) {
                    realmDataBase.copyToRealmOrUpdate(pokemons);
                }
            }
        });
        realmDataBase.close();
    }

    private void openRealm() {
        if (!isExternalStorageWritable()) {
            return;
        }
        File file = new File(context.getFilesDir() + "/Pokescanner/Db/");

        if (!file.exists()) {
            boolean result = file.mkdirs();
            Log.e("TTT", "Results: " + result);
        }
        RealmConfiguration realmDatabaseConfiguration = new RealmConfiguration.Builder(file)
                .name("pokemondatabase" + ".realm")
                .build();
        realmDataBase = Realm.getInstance(realmDatabaseConfiguration);
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    private void sendPokemonListToWear() {
        ArrayList<Pokemons> pokelist = new ArrayList<>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
        listout = new ArrayList<>();


        for (int i = 0; i < pokelist.size(); i++) {
            Pokemons pokemons = pokelist.get(i);
            //IF OUR POKEMANS IS FILTERED WE AINT SHOWIN HIM
            if (!PokemonListLoader.getFilteredList().contains(new FilterItem(pokemons.getNumber()))) {

                //ADD OUR POKEMANS TO OUR OUT LIST
                listout.add(pokemons);
            }
        }


        Gson gson = new Gson();
        String json = gson.toJson(listout, new TypeToken<ArrayList<Pokemons>>() {
        }.getType());
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/pokemonlist");
        putDataMapReq.getDataMap().putString("pokemons", json);
        putDataMapReq.getDataMap().putInt("progressbar", progressBar);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleWearApiClient, putDataReq);
    }


}

