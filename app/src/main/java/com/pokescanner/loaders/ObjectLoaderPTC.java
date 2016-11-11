package com.pokescanner.loaders;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokegoapi.api.PokemonGo;
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
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
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

import POGOProtos.Map.Fort.FortDataOuterClass;
import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import io.realm.Realm;
import io.realm.RealmConfiguration;
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


            if (MultiAccountLoader.cachedGo[position] == null) {
                OkHttpClient client = new OkHttpClient();
                //Create our provider and set it to null

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
                    MultiAccountLoader.cachedGo[position] = new PokemonGo(client);
                    MultiAccountLoader.cachedGo[position].login(provider);
                }
            }

            go = MultiAccountLoader.cachedGo[position];
            if (go == null) {
                return;
            }

            int scanPos = 0;


            if (go != null) {
                System.out.println("Erfolgreich eingeloggt: " + user.getUsername());
                for (LatLng pos : scanMap) {
                    go.setLatitude(pos.latitude);
                    go.setLongitude(pos.longitude);
                    go.setAltitude(0);
                    Map map = go.getMap();
                    if(MultiAccountLoader.cancelThreads){
                        return;
                    }
                    final Collection<CatchablePokemon> catchablePokemon = map.getCatchablePokemon();

                    final List<com.pokegoapi.api.gym.Gym> collectionGyms = map.getGyms();
                    final Collection<Pokestop> collectionPokeStops = map.getMapObjects().getPokestops();
                    boolean isBanned = map.getNearbyPokemon().size() > 0 ? false : true;

                    EventBus.getDefault().post(new ScanCircleEvent(pos, isBanned,user.getUsername(), user.getAccountColor()));
                    final ArrayList<EncounterResult> encounterResults = new ArrayList<>();
                    //long starttime = System.currentTimeMillis();
                    for(CatchablePokemon pokemonOut : catchablePokemon){
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
                    }

                    //System.out.println("Duration: " +(System.currentTimeMillis()-starttime));

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

                                Pokemons pokemon = new Pokemons(pokemonOut);
                                pokemon.setIvInPercentage(pokemonEncounter.getIvInPercentage());
                                pokemon.setIndividualAttack(pokemonEncounter.getIndividualAttack());
                                pokemon.setIndividualDefense(pokemonEncounter.getIndividualDefense());
                                pokemon.setIndividualStamina(pokemonEncounter.getIndividualStamina());
                                ArrayList<Pokemons> pokelist = new ArrayList<>(realm.copyFromRealm(realm.where(Pokemons.class).findAll()));
                                boolean alreadyNotificated = false;

                                if (pokemon.getExpires() < 0) {
                                    long currentTime = System.currentTimeMillis();
                                    pokemon.setExpires(currentTime + 900000);
                                    pokemon.setFoundTime(currentTime);
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
                                                    .setContentText(pokemon.getFormalName(context) + " (" + pokemon.getIvInPercentage() + "%) (" + DrawableUtils.getExpireTime(pokemon.getExpires()) + ")");
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

