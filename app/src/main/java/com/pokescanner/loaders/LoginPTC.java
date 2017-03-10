package com.pokescanner.loaders;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.listener.LoginListener;
import com.pokegoapi.auth.CredentialProvider;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.AsyncPokemonGoException;
import com.pokegoapi.exceptions.CaptchaActiveException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.exceptions.hash.HashException;
import com.pokegoapi.util.hash.HashProvider;
import com.pokescanner.R;
import com.pokescanner.events.ForceLogoutEvent;
import com.pokescanner.objects.User;

import org.greenrobot.eventbus.EventBus;

import java.util.regex.Pattern;

import okhttp3.OkHttpClient;

/**
 * Created by Julian on 27.02.2017.
 */
public class LoginPTC {

    private LoginListener loginListener;
    private String url;
    private boolean captchaSolved = true;
    public static Activity currentActivity;
    private HashProvider hasher;
    private DialogInterface.OnClickListener negativeButton;
    private boolean webViewOpen=false;
    private AlertDialog alertD;


    public PokemonGo getPokemongo(final User user) {
        CredentialProvider provider = null;
        OkHttpClient client = new OkHttpClient();
        final PokemonGo result = new PokemonGo(client);
        try {

            loginListener = new LoginListener() {
                @Override
                public void onLogin(PokemonGo api) {
                    System.out.println("Successfully logged in with SolveCaptcha! " + user.getUsername());
                }

                @Override
                public void onChallenge(PokemonGo api, final String challengeURL) {
                    System.out.println("Captcha received " + user.getUsername() + "! URL: " + challengeURL);
                    captchaSolved = false;
                    url = challengeURL;
                    currentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MultiAccountLoader.challengeURLs.put(user.getUsername(),challengeURL);

                        }
                    });
                }
            };
            result.addListener(loginListener);
            if (user.getAuthType() == User.GOOGLE) {
                if (user.getToken() != null) {
                    provider = new GoogleUserCredentialProvider(client, user.getToken().getRefreshToken());
                } else {
                    EventBus.getDefault().post(new ForceLogoutEvent());
                }
            } else {
                try{
                    provider = new PtcCredentialProvider(client, user.getUsername(), user.getPassword());
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            if (provider != null) {
                hasher = AuthAccountsLoader.getHashProvider();

                result.login(provider, hasher);

            }
        } catch (HashException e) {
            e.printStackTrace();
            return result;
        } catch (RemoteServerException e) {
            e.printStackTrace();
            return result;
        } catch (CaptchaActiveException e) {
            e.printStackTrace();
            return result;
        } catch (LoginFailedException e) {
            user.setStatus(User.STATUS_WRONGCREDENTIALS);
            e.printStackTrace();
            return null;
        }catch (AsyncPokemonGoException e) {

            e.printStackTrace();


            System.out.println("AsyncPokemonGo: " + user.getUsername());

            return this.getPokemongo(user);
        }


        if(result!=null){
            if(result.isActive()){
                user.setStatus(User.STATUS_VALID);
            }
        }
        return result;
    }
}
