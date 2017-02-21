package com.pokescanner.loaders;

import com.pokegoapi.auth.PtcCredentialProvider;
import com.pokegoapi.exceptions.CaptchaActiveException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokescanner.events.AuthLoadedEvent;

import org.greenrobot.eventbus.EventBus;

import okhttp3.OkHttpClient;

/**
 * Created by Brian on 7/22/2016.
 */
public class AuthPTCLoader extends Thread {
    String username,password;

    public AuthPTCLoader(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void run() {
        try {
            OkHttpClient client = new OkHttpClient();

            PtcCredentialProvider ptcCredentialProvider = null;
            try {
                ptcCredentialProvider = new PtcCredentialProvider(client, username, password);
            } catch (CaptchaActiveException e) {
                e.printStackTrace();
            }

                EventBus.getDefault().post(new AuthLoadedEvent(AuthLoadedEvent.OK));

        } catch (LoginFailedException e) {
            EventBus.getDefault().post(new AuthLoadedEvent(AuthLoadedEvent.AUTH_FAILED));
            e.printStackTrace();
        } catch (RemoteServerException e) {
            EventBus.getDefault().post(new AuthLoadedEvent(AuthLoadedEvent.SERVER_FAILED));
            e.printStackTrace();
        }
    }
}
