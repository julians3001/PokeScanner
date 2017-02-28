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
                public void onChallenge(PokemonGo api, String challengeURL) {
                    System.out.println("Captcha received " + user.getUsername() + "! URL: " + challengeURL);
                    captchaSolved = false;
                    url = challengeURL;
                    currentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            final AlertDialog.Builder alert = new AlertDialog.Builder(currentActivity);

                            alert.setTitle("Captcha " + user.getUsername());

                            WebView wv = new WebView(currentActivity.getApplicationContext());
                            wv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            wv.getSettings().setJavaScriptEnabled(true);
                            wv.loadUrl(url);
                            wv.setWebViewClient(new WebViewClient() {
                                @Override
                                public boolean shouldOverrideUrlLoading(WebView view, String url) {

                                    if (url.startsWith("unity:")) {
                                        // magic
                                        String token = url.toString().split(Pattern.quote(":"))[1];
                                        System.out.println(token);

                                        try {
                                            if (result.verifyChallenge(token)) {
                                                System.out.println("Captcha was correctly solved!");
                                                alertD.cancel();

                                                webViewOpen = false;
                                                captchaSolved = true;
                                            } else {
                                                System.out.println("Captcha was incorrectly solved! Please try again.");
                                            /*
                                                Ask for a new challenge url, don't need to check the result,
                                                because the LoginListener will be called when this completed.
                                            */
                                                //MultiAccountLoader.cachedGo[position].checkChallenge();
                                            }
                                        } catch (RemoteServerException e) {
                                            e.printStackTrace();
                                        } catch (CaptchaActiveException e) {
                                            e.printStackTrace();
                                        } catch (LoginFailedException e) {
                                            e.printStackTrace();
                                        } catch (InvalidProtocolBufferException e) {
                                            e.printStackTrace();
                                        } catch (HashException e) {
                                            e.printStackTrace();
                                        }
                                        return true;
                                    } else {
                                        view.loadUrl(url);

                                        return true;
                                    }
                                }

                            });

                            alert.setView(wv);
                            negativeButton = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    webViewOpen = false;
                                }};
                            alert.setNegativeButton(R.string.cancel,negativeButton);
                            alertD =  alert.show();
                            webViewOpen = true;
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
                provider = new PtcCredentialProvider(client, user.getUsername(), user.getPassword());
            }

            if (provider != null) {
                hasher = AuthAccountsLoader.getHashProvider();

                result.login(provider, hasher);

            }
        } catch (HashException e) {
            e.printStackTrace();
        } catch (RemoteServerException e) {
            e.printStackTrace();
        } catch (CaptchaActiveException e) {
            e.printStackTrace();
        } catch (LoginFailedException e) {
            user.setStatus(User.STATUS_WRONGCREDENTIALS);
            e.printStackTrace();
            return null;
        }catch (AsyncPokemonGoException e) {

            e.printStackTrace();


            System.out.println("AsyncPokemonGo: " + user.getUsername());

            return this.getPokemongo(user);
        }
        int waitingCounter = 0;
        while(!captchaSolved){

            try {
            if(waitingCounter==5){
                if(webViewOpen){
                    waitingCounter = 0;
                } else{
                    return this.getPokemongo(user);
                }
            }
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            waitingCounter++;
        }
        if(result!=null){
            if(result.isActive()){
                user.setStatus(User.STATUS_VALID);
            }
        }
        return result;
    }
}
