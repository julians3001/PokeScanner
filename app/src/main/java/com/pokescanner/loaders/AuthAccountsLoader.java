package com.pokescanner.loaders;

import android.location.Location;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.map.Map;
import com.pokegoapi.api.map.MapObjects;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.AsyncPokemonGoException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
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
                    PtcCredentialProvider ptcCredentialProvider = new PtcCredentialProvider(client, user.getUsername(), user.getPassword());
                    sleep(300);

                        go  = new PokemonGo(ptcCredentialProvider, client);
                        LatLng currentPosition = getCurrentLocation();
                        go.setLatitude(currentPosition.latitude);
                        go.setLongitude(currentPosition.longitude);
                        Map map = go.getMap();
                        MapObjects event = map.getMapObjects();

                    if (ptcCredentialProvider.getAuthInfo().hasToken()) {
                        user.setStatus(User.STATUS_VALID);
                    } else {
                        user.setStatus(User.STATUS_INVALID);
                    }
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
