
package com.pokescanner.objects;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokescanner.R;
import com.pokescanner.helper.GoMapPokemon;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.DrawableUtils;

import org.joda.time.DateTime;
import org.joda.time.Instant;

import java.math.BigDecimal;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import POGOProtos.Map.Pokemon.MapPokemonOuterClass;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Created by Brian on 7/21/2016.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false,exclude = {"distance","Name","Number","expires","foundTime"})
public class Pokemons  extends RealmObject{
    int Number;
    String Name;
    @PrimaryKey
    long encounterid;
    long expires;
    double longitude,latitude;
    double distance;
    long foundTime;
    double ivInPercentage;
    int individualAttack;
    int individualDefense;
    int individualStamina;

    public Pokemons() {}

    public Pokemons(MapPokemonOuterClass.MapPokemon pokemonIn){
            setEncounterid(pokemonIn.getEncounterId());
            setName(pokemonIn.getPokemonId().toString());
            setExpires(pokemonIn.getExpirationTimestampMs());
            setNumber(pokemonIn.getPokemonId().getNumber());
            setLatitude(pokemonIn.getLatitude());
            setLongitude(pokemonIn.getLongitude());


    }

    public Pokemons(CatchablePokemon pokemonIn){
        setEncounterid(pokemonIn.getEncounterId());
        setName(pokemonIn.getPokemonId().toString());
        setExpires(pokemonIn.getExpirationTimestampMs());
        setNumber(pokemonIn.getPokemonId().getNumber());
        setLatitude(pokemonIn.getLatitude());
        setLongitude(pokemonIn.getLongitude());


    }

    public Pokemons(GoMapPokemon goMapPokemon) {
        this.Number = goMapPokemon.pokemon_id;
        this.encounterid = goMapPokemon.eid;
        this.expires = goMapPokemon.disappear_time *1000;
        this.ivInPercentage = round((float) (goMapPokemon.iv * 2.22580645161),2);
        this.latitude = goMapPokemon.latitude;
        this.longitude = goMapPokemon.longitude;
    }
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public int getResourceID(Context context) {
        return DrawableUtils.getResourceID(getNumber(),context);
    }
    public boolean isNotExpired() {
        if(getExpires()==-1) return true;
        //Create a date
        DateTime expires = new DateTime(getExpires());
        //If this date is after the current time then it has not expired!
        return expires.isAfter(new Instant());
    }

    public MarkerOptions getMarker(Context context) {
        Format formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        int resourceID = getResourceID(context);
        //Find our interval
        String timeOut = DrawableUtils.getExpireTime(getExpires(), getFoundTime());

        //set our location
        LatLng position = new LatLng(getLatitude(), getLongitude());

        Bitmap out = DrawableUtils.getBitmapFromView(resourceID,"",context,DrawableUtils.PokemonType);

        MarkerOptions pokeIcon = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(out))
                .draggable(true)
                .position(position);
        if(Settings.get(context).isUseOldMapMarker()){
            pokeIcon.title(getFormalName(context) +" (" + String.format("%.2f", getIvInPercentage())+"%)");
            pokeIcon.draggable(true);
            pokeIcon.snippet(R.string.expires_in +"" + formatter.format(new Date(getExpires())) +"\n"+"Attack: "+getIndividualAttack()+"\n"+"Defense: "+getIndividualDefense()+"\n"+"Stamina: "+getIndividualStamina());
        }
        return pokeIcon;
    }

    public String getFormalName(Context context) {
        String name = getName();

        if (!Settings.get(context).isForceEnglishNames()) {
            name = context.getString(DrawableUtils.getStringID(getNumber(), context));
        }

        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public Marker updateMarker(Marker marker,Context context) {
        Format formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String expires = DrawableUtils.getExpireTime(getExpires(),getFoundTime());

        Bitmap newbit = DrawableUtils.getBitmapFromView(getResourceID(context),"",context,DrawableUtils.PokemonType);

        try{

            //marker.setIcon(BitmapDescriptorFactory.fromBitmap(newbit));
            marker.setSnippet(R.string.expires_in +"" + formatter.format(new Date(getExpires()))+"\n"+"Attack: "+getIndividualAttack()+"\n"+"Defense: "+getIndividualDefense()+"\n"+"Stamina: "+getIndividualStamina());
        } catch(Exception e){
            e.printStackTrace();
        }

        return marker;
    }
}
