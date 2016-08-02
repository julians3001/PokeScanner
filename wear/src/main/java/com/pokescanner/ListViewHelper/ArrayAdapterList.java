package com.pokescanner.ListViewHelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pokescanner.R;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.utils.DrawableUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Julian on 01.08.2016.
 */
public class ArrayAdapterList extends ArrayAdapter<Pokemons>{
    Context mContext;
    int layoutResourceID;
    ArrayList<Pokemons> data;
    TextView Distance;
    TextView PokemonName;
    TextView PokemonExpires;
    ImageView PokemonImage;

    public ArrayAdapterList(Context context, int resource, ArrayList<Pokemons> objects) {
        super(context, resource, objects);
        this.layoutResourceID = resource;
        this.mContext = context;
        this.data = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceID, parent, false);
        }

        Pokemons pokemons = data.get(position);
        Distance = (TextView) convertView.findViewById(R.id.tvDistance);
        PokemonName = (TextView) convertView.findViewById(R.id.tvPokemonName);
        PokemonImage = (ImageView) convertView.findViewById(R.id.PokemonImage);
        PokemonExpires = (TextView) convertView.findViewById(R.id.tvExpires);

        Bitmap bitmap = DrawableUtils.getBitmapFromView(pokemons.getResourceID(mContext),"",mContext,DrawableUtils.PokemonType);

        PokemonImage.setImageBitmap(bitmap);
        PokemonName.setText(pokemons.getFormalName(mContext));
        Distance.setText(String.valueOf(Math.round(pokemons.getDistance()))+"M");
        PokemonExpires.setText(DrawableUtils.getExpireTime(pokemons.getExpires()));


        return convertView;

    }





}
