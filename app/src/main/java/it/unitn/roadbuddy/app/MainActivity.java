package it.unitn.roadbuddy.app;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.Arrays;

import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.postgres.PostgresUtils;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        LocationListener {

    public static final String KEY_PREF_USER_NAME = "pref_dev_user_name";
    public static final String INTENT_JOIN_TRIP = "join-trip";
    public static final String JOIN_TRIP_INVITER_KEY = "trip-inviter";

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 123;

    boolean locationPermissionEnabled = false;

    ViewPager mPager;
    PagerAdapter mAdapter;

    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager();
    GoogleApiClient googleApiClient;
    HandlerThread backgroundThread;
    Handler backgroundTasksHandler;
    CheckInvitesRunnable inviteRunnable;
    CallbackManager callbackManager;

    /**
     * Store the intent used to launch the app as well as the previous
     * saved instance state. The map fragment will need most of them
     * to decide which state to launch (e.g. joining a trip or resuming
     * the previous activity)
     */
    Intent intent;
    Bundle savedInstanceState;

    int currentUserId;
    private AccessTokenTracker accessTokenTracker;
    private AccessToken FaceAccessToken = null;

    private GoogleApiClient client;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        FacebookSdk.sdkInitialize(getApplicationContext());  // Initialize the SDK before executing any other operations
        setContentView( R.layout.activity_main );
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        setInitialPreferences(loginResult);
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onError(FacebookException exception) {
                    }
                });
        AppEventsLogger.activateApp(this);
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
                FaceAccessToken = currentAccessToken;
                Log.v("Login","Vecchio token "+oldAccessToken);
                Log.v("Login","Nuovo token "+currentAccessToken);
            }
        };
        FaceAccessToken = AccessToken.getCurrentAccessToken();
        mPager = ( ViewPager ) findViewById( R.id.pager );
        mAdapter = new PagerAdapter( getSupportFragmentManager( ) );
        mPager.setAdapter( mAdapter );

        this.intent = getIntent();
        this.savedInstanceState = savedInstanceState;

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onStart() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getBoolean(SettingsFragment.KEY_PREF_DEV_ENABLED, false)) {
            String userName = pref.getString(SettingsFragment.KEY_PREF_USER_NAME, "<unset>");
            currentUserId = pref.getInt(SettingsFragment.KEY_PREF_USER_ID, -1);

            Toast.makeText( this,
                            String.format( "You are currently running as user %s (id: %d)",
                                           userName, currentUserId ), Toast.LENGTH_SHORT ).show( );
        }
        else {
            if ( FaceAccessToken == null && currentUserId <= 1 ){
                FireLogInDialogFragment dialog = new FireLogInDialogFragment();
                dialog.show( getSupportFragmentManager(), "login" );
            }
            currentUserId = 1;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                showMessageOKCancel(
                        "You need to allow access to the current position",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(
                                        MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        LOCATION_PERMISSION_REQUEST_CODE);
                            }
                        }
                );
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            }
        } else {
            locationPermissionEnabled = true;
        }

        if (locationPermissionEnabled) {
            googleApiClient.connect();
            if (mAdapter.getCurrentMF() != null) {
                mAdapter.getCurrentMF().onStart();
            }
        }

        backgroundThread = new HandlerThread("background worker");
        backgroundThread.start();
        backgroundTasksHandler = new Handler(backgroundThread.getLooper());

        inviteRunnable = new CheckInvitesRunnable(
                backgroundTasksHandler, getApplicationContext(), currentUserId
        );

        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://it.unitn.roadbuddy.app/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    protected void onStop() {
        try {
            PostgresUtils.getInstance().close();  // FIXME [ed] find a better place
        } catch (SQLException exc) {
            Log.e(getClass().getName(), "on destroy", exc);
        }
        if (locationPermissionEnabled && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    googleApiClient, this
            );

            googleApiClient.disconnect();
        }

        backgroundThread.quit();

        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://it.unitn.roadbuddy.app/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    locationPermissionEnabled = true;
                    googleApiClient.connect();

                    mAdapter = new PagerAdapter(getSupportFragmentManager());
                    mPager.setAdapter(mAdapter);
                } else {
                    locationPermissionEnabled = false;
                }
                break;
            }

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // called when the google api client has successfully connected to whatever

        // ask for periodic location updates running the listener on the background worker
        LocationRequest requestType = LocationRequest
                .create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(5 * 60 * 1000)
                .setFastestInterval(15 * 1000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, requestType,
                    this,
                    backgroundThread.getLooper()
            );
        }
    }

    @Override
    public void onConnectionSuspended(int n) {

    }

    @Override
    public void onLocationChanged(Location location) {
        // gets run in a background thread

        try {
            DAOFactory.getUserDAO().setCurrentLocation(
                    currentUserId, new LatLng(location.getLatitude(),
                            location.getLongitude())
            );
        } catch (BackendException exc) {
            Log.e(getClass().getName(), "while updating user position", exc);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }

    public void showChoosenPath(Path path) {
        mPager.setCurrentItem(0);
        LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.fragment_drawable_path_info_large, null);
        TextView txtPathDescription = (TextView) linearLayout.findViewById(R.id.txtPathDescription);
        TextView txtTotalDistance = (TextView) linearLayout.findViewById(R.id.txtTotalDistance);
        TextView txtTotalDuration = (TextView) linearLayout.findViewById(R.id.txtTotalDuration);
        txtPathDescription.setText(path.getDescription());
        txtTotalDistance.setText("Distance: " + Long.toString(path.getDistance()));
        txtTotalDuration.setText("Expected Duration: " + Long.toString(path.getDuration()));
        ((MapFragment) mAdapter.getCurrentMF()).slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        ((MapFragment) mAdapter.getCurrentMF()).sliderLayout.setView(linearLayout);
        //((MapFragment) mAdapter.getCurrentMF()).showTrip(path);
    }

    public boolean isLocationPermissionEnabled() {
        return locationPermissionEnabled;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
    public void setInitialPreferences(LoginResult loginResult){
        GraphRequest graphRequest   =   GraphRequest.newMeRequest(loginResult.getAccessToken(),
                new GraphRequest.GraphJSONObjectCallback(){
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response){
                        Log.d("JSON", ""+response.getJSONObject().toString());
                        try{
                            String email       = object.getString("email");
                            String first_name  =   object.optString("first_name");
                            String last_name   =   object.optString("last_name");
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences( MainActivity.this );
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(KEY_PREF_USER_NAME,first_name+last_name);
                            editor.apply();
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,first_name,last_name,email");
        graphRequest.setParameters(parameters);
        graphRequest.executeAsync();
    }

    public void setInitialPreferences(String username){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences( MainActivity.this );
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_PREF_USER_NAME, username);
        editor.apply();
    }

    public class FireLogInDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final View dialog_preference = getActivity().getLayoutInflater().inflate(R.layout.login_dialog, null);
            builder.setView(dialog_preference);
            builder.setPositiveButton("Custom username", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    EditText username = (EditText)dialog_preference.findViewById(R.id.username_pref);
                    if (username != null && username.getText() != null ){
                        setInitialPreferences(  username.getText().toString() );
                    }
                }
            } )
                    .setNegativeButton("Facebook", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            LoginManager.getInstance().logInWithReadPermissions(MainActivity.this, Arrays.asList("public_profile","email"));
                        }
                    } )
                    .setTitle(R.string.dialog_sign_in);
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            //Toast.makeText( MainActivity.this, "You wont be able to perform most of action", Toast.LENGTH_LONG).show( );
        }
    }
}