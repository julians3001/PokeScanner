package com.pokescanner.loaders;

import com.pokegoapi.api.PokemonGo;

/**
 * Created by Julian on 27.02.2017.
 */
public class PokemonGoWithUsername {
    public final String username;
    public final PokemonGo api;

    public PokemonGoWithUsername(String username, PokemonGo api) {
        this.username = username;
        this.api = api;
    }
}
