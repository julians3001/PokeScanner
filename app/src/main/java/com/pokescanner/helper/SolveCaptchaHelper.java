package com.pokescanner.helper;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.CaptchaActiveException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.exceptions.hash.HashException;
import com.pokescanner.R;
import com.pokescanner.loaders.LoginPTC;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Created by Julian on 02.03.2017.
 */
public class SolveCaptchaHelper {
    private PokemonGo result;
    private AlertDialog alertD;
    private boolean webViewOpen;
    private boolean captchaSolved;


}
