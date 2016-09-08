package com.pokescanner.multiboxing;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.google.android.gms.location.LocationServices;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.pokescanner.R;
import com.pokescanner.loaders.AuthAccountsLoader;
import com.pokescanner.loaders.AuthSingleAccountLoader;
import com.pokescanner.objects.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;



public class MultiboxingActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.rvMultiboxingAccountList)
    RecyclerView userRecycler;
    private ArrayList<User> userList;
    private MultiboxingAdapter userAdapter;

    private Realm realm;
    private final static int STORAGE_PERMISSION_REQUESTED = 1300;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getReadWritePermission();
        setContentView(R.layout.activity_multiboxing);
        ButterKnife.bind(this);

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this)
                .name(Realm.DEFAULT_REALM_NAME)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
        realm = Realm.getDefaultInstance();

        realm.addChangeListener(new RealmChangeListener<Realm>() {
            @Override
            public void onChange(Realm element) {
                loadAccounts();
            }
        });

        userList = new ArrayList<User>();

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        userRecycler.setLayoutManager(mLayoutManager);

        userAdapter = new MultiboxingAdapter(this, userList, new MultiboxingAdapter.accountRemovalListener() {
            @Override
            public void onRemove(User user) {
                removeAccount(user);
            }
        });

        userRecycler.setAdapter(userAdapter);
    }

    private void removeAccount(final User user) {
        int realmSize = realm.where(User.class).findAll().size();
        if (realmSize != 1) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    if (realm.where(User.class).equalTo("username", user.getUsername())
                            .findAll().deleteAllFromRealm()) {
                        int index = userList.indexOf(user);
                        userList.remove(index);
                        userAdapter.notifyItemRemoved(index);
                    }
                }
            });
        }
    }


    private void loadAccounts(){
        userList.clear();
        userList.addAll(realm.copyFromRealm(realm.where(User.class).findAll()));
        userAdapter.notifyDataSetChanged();
    }

    public void getReadWritePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setCancelable(false)
                    .setMessage(R.string.Permission_Required_Auto_Updater)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MultiboxingActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUESTED);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(MultiboxingActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUESTED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        realm = Realm.getDefaultInstance();
        loadAccounts();
        refreshAccounts();
    }


    @OnClick(R.id.btnAddAccount)
    public void addAccountDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_account,null);
        final AlertDialog builder = new AlertDialog.Builder(this).create();

        final TextView etUsername = (TextView) view.findViewById(R.id.etAddUsername);
        final TextView etPassword = (TextView) view.findViewById(R.id.etAddPassword);

        Button btnAdd = (Button) view.findViewById(R.id.btnOk);
        Button btnCancel = (Button) view.findViewById(R.id.btnCancel);


        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                int color = userList.size();

                User user = new User(username,password,null,User.PTC,User.STATUS_UNKNOWN);
                TypedArray colors = getResources().obtainTypedArray(R.array.circleColors);
                Random r = new Random();

                int nextInt = r.nextInt(199);
                int colorId = colors.getColor(nextInt, -1);
                user.setAccountColor(colorId);

                realm.beginTransaction();
                realm.copyToRealmOrUpdate(user);
                realm.commitTransaction();

                AuthSingleAccountLoader singleloader = new AuthSingleAccountLoader(user);
                singleloader.start();

                builder.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
            }
        });



        builder.setView(view);
        builder.show();
    }

    @OnLongClick(R.id.btnAddAccount)
    public boolean addAccountFromFile(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    0);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        File file = null;
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();

                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            Uri uri = clip.getItemAt(i).getUri();
                            file = new File(uri.getPath());

                        }
                    }
                    // For Ice Cream Sandwich
                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra
                            (FilePickerActivity.EXTRA_PATHS);

                    if (paths != null) {
                        for (String path: paths) {
                            Uri uri = Uri.parse(path);
                            file = new File(uri.getPath());
                        }
                    }
                }

            } else {
                Uri uri = data.getData();
                file = new File(uri.getPath());
            }

            FileInputStream fin = null;
            try {
                fin = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if(fin == null){

                return;
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fin));
            String receiveString = "";
            int color = userList.size();

            try {
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    String [] credentials = receiveString.split(" ");
                    String username = credentials[0];
                    String password = credentials[1];

                    User user = new User(username,password,null,User.PTC,User.STATUS_UNKNOWN);

                    TypedArray colors = getResources().obtainTypedArray(R.array.circleColors);
                    Random r = new Random();

                    int nextInt = r.nextInt(199);
                    int colorId = colors.getColor(nextInt, -1);
                    user.setAccountColor(colorId);

                    realm.beginTransaction();
                    realm.copyToRealmOrUpdate(user);
                    realm.commitTransaction();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    @OnClick(R.id.btnRefresh)
    public void refreshAccounts() {
        new AuthAccountsLoader().start();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
