package com.pokescanner.helper;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokescanner.objects.FilterItem;
import com.pokescanner.objects.NotificationItem;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.settings.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

import io.realm.Realm;

/**
 * Created by Brian on 7/21/2016.
 */
public class PokemonListLoader {

    public static ArrayList<FilterItem> getPokelist(Context context) throws IOException {
        Realm realm = Realm.getDefaultInstance();
        populatePokemonList(context);
        ArrayList<FilterItem> returnlist = new ArrayList<>(realm.copyFromRealm(
                realm.where(FilterItem.class)
                        .findAll()
                        .sort("Number")));
        return returnlist;
    }

    public static ArrayList<NotificationItem> getNotPokelist(Context context) throws IOException{
        Realm realm = Realm.getDefaultInstance();
        populateNotPokemonList(context);
        ArrayList<NotificationItem> returnlist = new ArrayList<>(realm.copyFromRealm(
                realm.where(NotificationItem.class)
                        .findAll()
                        .sort("Number")));
        return returnlist;
    }

    public static ArrayList<FilterItem> getFilteredList() {
        Realm realm = Realm.getDefaultInstance();
        ArrayList returnArray =  new ArrayList<>(realm.copyFromRealm(
                realm.where(FilterItem.class)
                .equalTo("filtered",true)
                .findAll()
                .sort("Number")));
        realm.close();
        return returnArray;
    }

    public static ArrayList<NotificationItem> getNotificationList() {
        Realm realm = Realm.getDefaultInstance();
        ArrayList returnArray =  new ArrayList<>(realm.copyFromRealm(
                realm.where(NotificationItem.class)
                        .equalTo("filtered",true)
                        .findAll()
                        .sort("Number")));
        realm.close();
        return returnArray;
    }

    public static void savePokeList(final ArrayList<FilterItem> pokelist) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(pokelist);
            }
        });
        realm.close();
    }

    public static void populatePokemonList(Context context) throws IOException {
        Realm realm = Realm.getDefaultInstance();
        if (realm.where(FilterItem.class).findAll().size() != 251) {
            InputStream is = context.getAssets().open("pokemons.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String bufferString = new String(buffer);
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<FilterItem>>() {}.getType();
            final ArrayList<FilterItem> filterItems = gson.fromJson(bufferString, listType);
            translateNamesIfNeeded(context, filterItems);
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.copyToRealmOrUpdate(filterItems);
                }
            });
        }
        realm.close();
    }

    public static void populateNotPokemonList(Context context) throws IOException {
        Realm realm = Realm.getDefaultInstance();
        if (realm.where(NotificationItem.class).findAll().size() != 251) {
            InputStream is = context.getAssets().open("pokemons.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String bufferString = new String(buffer);
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<NotificationItem>>() {}.getType();
            final ArrayList<NotificationItem> filterItems = gson.fromJson(bufferString, listType);
            if (!Settings.get(context).isForceEnglishNames()) {
                for (NotificationItem item : filterItems) {
                    String identifierName = "p" + Integer.toString(item.getNumber());
                    int resourceID = context.getResources().getIdentifier(identifierName, "string", context.getPackageName());
                    if (resourceID != 0) {
                        item.setName(context.getResources().getString(resourceID));
                    }
                }
            }
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.copyToRealmOrUpdate(filterItems);
                }
            });
        }
        realm.close();
    }

    private static void translateNamesIfNeeded(Context context, ArrayList<FilterItem> filterItems) {
        if (!Settings.get(context).isForceEnglishNames()) {
            for (FilterItem item : filterItems) {
                String identifierName = "p" + Integer.toString(item.getNumber());
                int resourceID = context.getResources().getIdentifier(identifierName, "string", context.getPackageName());
                if (resourceID != 0) {
                    item.setName(context.getResources().getString(resourceID));
                }
            }
        }
    }
}
