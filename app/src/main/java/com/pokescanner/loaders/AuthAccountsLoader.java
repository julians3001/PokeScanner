package com.pokescanner.loaders;

import android.location.Location;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.Map;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.AsyncPokemonGoException;
import com.pokegoapi.exceptions.CaptchaActiveException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.exceptions.hash.HashException;
import com.pokegoapi.util.hash.HashProvider;
import com.pokegoapi.util.hash.legacy.LegacyHashProvider;
import com.pokegoapi.util.hash.pokehash.PokeHashKey;
import com.pokegoapi.util.hash.pokehash.PokeHashProvider;
import com.pokescanner.objects.User;
import com.pokescanner.utils.PermissionUtils;

import java.util.List;

import io.realm.Realm;
import okhttp3.OkHttpClient;

/**
 * Created by Brian on 7/31/2016.
 */
public class AuthAccountsLoader extends Thread {

    public AuthAccountsLoader(){}

    public static final String POKEHASH_KEY = "";

    public static HashProvider getHashProvider() {

            return new LegacyHashProvider();

    }

    @Override
    public void run() {
            Realm realm = Realm.getDefaultInstance();
            List<User> users = realm.copyFromRealm(realm.where(User.class).findAll());

            for (User user: users) {
                user.setStatus(User.STATUS_UNKNOWN);
            }
            PokemonGo go;
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(users);
            realm.commitTransaction();

            OkHttpClient client = new OkHttpClient();
            for (User user: users) {
                try {
                    HashProvider hasher = AuthAccountsLoader.getHashProvider();
                    PtcCredentialProvider ptcCredentialProvider = null;
                    try {
                        ptcCredentialProvider = new PtcCredentialProvider(client, user.getUsername(), user.getPassword());
                    } catch (CaptchaActiveException e) {
                        e.printStackTrace();
                    }
                    sleep(300);

                        go  = new PokemonGo(client);

                        //go.login(ptcCredentialProvider, hasher);

                    LatLng currentPosition = getCurrentLocation();
                        go.setLatitude(currentPosition.latitude);
                        go.setLongitude(currentPosition.longitude);
                    go.setAltitude(0);
                        Map map = go.getMap();
                        //MapObjects event = map.getMapObjects();

                    /*if (ptcCredentialProvider.getAuthInfo(true).hasToken()) {
                        user.setStatus(User.STATUS_VALID);
                    } else {
                        user.setStatus(User.STATUS_INVALID);
                    }*/
                } catch (RemoteServerException | LoginFailedException e) {
                    user.setStatus(User.STATUS_INVALID);
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (AsyncPokemonGoException e){
                    e.printStackTrace();
                    user.setStatus(User.STATUS_INVALID);
                }

            }

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(users);
        realm.commitTransaction();

        realm.close();
    }

    @SuppressWarnings({"MissingPermission"})
    public LatLng getCurrentLocation() {
        if (PermissionUtils.doWeHaveGPSandLOC(MultiAccountLoader.context)) {
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
