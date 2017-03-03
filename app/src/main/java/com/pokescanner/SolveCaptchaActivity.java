package com.pokescanner;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.protobuf.InvalidProtocolBufferException;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.exceptions.CaptchaActiveException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.exceptions.hash.HashException;
import com.pokescanner.loaders.MultiAccountLoader;
import com.pokescanner.loaders.PokemonGoWithUsername;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public class SolveCaptchaActivity extends AppCompatActivity {

    WebView wvSolveCaptcha;

    private PokemonGo api;
    private boolean captchaSolved;
    Toolbar toolbar;
    PokemonGoWithUsername pokemonGoWithUsername;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solve_captcha);
        wvSolveCaptcha = (WebView) findViewById(R.id.wvSolveCaptcha);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Iterator it = MultiAccountLoader.challengeURLs.entrySet().iterator();
        if (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            solveCaptcha(pair);
            it.remove();
        } else {
            finish();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.captcha_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_skip) {
            Iterator it = MultiAccountLoader.challengeURLs.entrySet().iterator();
            if (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                solveCaptcha(pair);
                it.remove();
            } else {
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public void solveCaptcha(Map.Entry pair){

        for(PokemonGoWithUsername elem : MultiAccountLoader.cachedGo){
            if(elem.username.equals(pair.getKey().toString())){
                api = elem.api;
                pokemonGoWithUsername = elem;
            }
        }
        setTitle("Solve Captcha "+pair.getKey().toString());
        wvSolveCaptcha.getSettings().setJavaScriptEnabled(true);
        wvSolveCaptcha.loadUrl((String) pair.getValue());
        wvSolveCaptcha.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                if (url.startsWith("unity:")) {
                    // magic
                    String token = url.toString().split(Pattern.quote(":"))[1];
                    System.out.println(token);

                    try {
                        if (api.verifyChallenge(token)) {
                            System.out.println("Captcha was correctly solved!");
                            captchaSolved = true;
                            pokemonGoWithUsername.banned = false;
                            Iterator it = MultiAccountLoader.challengeURLs.entrySet().iterator();
                            if (it.hasNext()) {
                                Map.Entry pair = (Map.Entry) it.next();
                                solveCaptcha(pair);
                                it.remove();
                            } else {
                                finish();
                            }
                        } else {
                            System.out.println("Captcha was incorrectly solved! Please try again.");

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
    }

}
