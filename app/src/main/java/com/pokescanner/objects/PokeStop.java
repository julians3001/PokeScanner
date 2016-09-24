package com.pokescanner.objects;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.DrawableUtils;
import com.pokescanner.utils.SettingsUtil;
import com.pokescanner.utils.UiUtils;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PokeStop extends RealmObject
{
    double latitude, longitude;
    @PrimaryKey
    String id;
    @Index
    String activePokemonName;
    boolean hasLureInfo;
    long lureExpiryTimestamp;
    long activePokemonNo;
    public PokeStop()
    {
    }

    public PokeStop(Pokestop pokestopData){
        setLatitude(pokestopData.getLatitude());
        setLongitude(pokestopData.getLongitude());
        setId(pokestopData.getId());
        setHasLureInfo(pokestopData.hasLure());
        if(hasLureInfo){
            setLureExpiryTimestamp(pokestopData.getFortData().getLureInfo().getEncounterId());
            setActivePokemonNo(pokestopData.getFortData().getLureInfo().getActivePokemonId().getNumber());
            setActivePokemonName(pokestopData.getFortData().getLureInfo().getActivePokemonId().toString());
        }

    }

    public DateTime getExpiryTime()
    {
        return new DateTime(getLureExpiryTimestamp());
    }

    public MarkerOptions getMarker(Context context)
    {

        String uri = "";
        String snippetMessage  = "";
        String iconMessage = "Pokestop";
        if(hasLureInfo) //There is a lure active at the pokestop
        {


            if(true)  //The lure is currently active
            {
                //Interval interval = new Interval(new Instant(), getExpiryTime());
                //DateTime dt = new DateTime(interval.toDurationMillis());
                //DateTimeFormatter fmt = DateTimeFormat.forPattern("mm:ss");

                String activePokemonName = getActivePokemonName();
                activePokemonName = activePokemonName.substring(0, 1).toUpperCase() + activePokemonName.substring(1).toLowerCase();
                snippetMessage = "A lure is active here, and has attracted a " + activePokemonName;
            }
        }

        LatLng position = new LatLng(getLatitude(), getLongitude());

        MarkerOptions pokestopMarker = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(getBitmap(context)))
                .position(position);
        if(Settings.get(context).isUseOldMapMarker()){
            pokestopMarker.title("Pokestop");
            pokestopMarker.snippet(snippetMessage);
        }
        return pokestopMarker;
    }

    public String getLureExpiryTime()
    {
        String result = "";
        if(hasLureInfo )
        {
            Interval interval = new Interval(new Instant(), getExpiryTime());
            DateTime dt = new DateTime(interval.toDurationMillis());
            DateTimeFormatter fmt = DateTimeFormat.forPattern("mm:ss");
            result = fmt.print(dt);
        }
        return result;
    }

    public String getLuredPokemonName()
    {
        String luredPokemonName = getActivePokemonName();
        luredPokemonName = luredPokemonName.substring(0, 1).toUpperCase() + luredPokemonName.substring(1).toLowerCase();
        return luredPokemonName;
    }

    public Bitmap getBitmap(Context context)
    {
        int pokeStopType = DrawableUtils.PokeStopType;
        int pokemonnumber = (int) getActivePokemonNo();

        String uri = "stop";

        if(hasLureInfo) {
            uri = "stop_lure";
            pokeStopType = DrawableUtils.LuredPokeStopType;
            //if ShowLuredPokemon is enabled, show the icon of the lured pokemon
            if (SettingsUtil.getSettings(context).isShowLuredPokemon()) {
                if (SettingsUtil.getSettings(context).isShuffleIcons()) {
                    uri = "ps" + pokemonnumber;
                }
                else uri = "p" + pokemonnumber;
            }

            //but don't show it if it's filtered, just show the lured pokestop icon
            if (UiUtils.isPokemonFiltered(pokemonnumber)) {
                uri = "stop_lure";
            }
        }

        int resourceID = context.getResources().getIdentifier(uri, "drawable", context.getPackageName());
        Bitmap out = DrawableUtils.getBitmapFromView(resourceID, null, context, pokeStopType);

        return out;
    }
}
