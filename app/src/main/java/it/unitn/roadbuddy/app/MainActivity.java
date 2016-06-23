package it.unitn.roadbuddy.app;

import android.Manifest;
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
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.sql.SQLException;

import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.User;
import it.unitn.roadbuddy.app.backend.postgres.PostgresUtils;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
                   LocationListener {

    public static final String
            INTENT_JOIN_TRIP = "join-trip",
            JOIN_TRIP_INVITER_KEY = "trip-inviter",
            CURRENT_USER_KEY = "current-user",
            CURRENT_USER_ID_KEY = "current-user-id";

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 123;

    boolean locationPermissionEnabled = false;

    ViewPager mPager;
    PagerAdapter mAdapter;

    GoogleApiClient googleApiClient;
    HandlerThread backgroundThread;
    Handler backgroundTasksHandler;
    CheckInvitesRunnable inviteRunnable;
    GetCurrentUserRunnable getUserRunnable;

    /**
     * Store the intent used to launch the app as well as the previous
     * saved instance state. The map fragment will need most of them
     * to decide which state to launch (e.g. joining a trip or resuming
     * the previous activity)
     */
    Intent intent;
    Bundle savedInstanceState;

    /**
     * current user is retrieved from db so it might take a while but
     * the user id is immediately available and someone might need it
     */
    User currentUser;
    Integer currentUserId;

    private GoogleApiClient client;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        FacebookSdk.sdkInitialize( getApplicationContext( ) );  // Initialize the SDK before executing any other operations
        setContentView( R.layout.activity_main );

        AppEventsLogger.activateApp( getApplication( ) );
        TabLayout tabLayout = ( TabLayout ) findViewById( R.id.tab_layout );
        assert tabLayout != null;
        tabLayout.setTabGravity( TabLayout.GRAVITY_FILL );
        mPager = ( ViewPager ) findViewById( R.id.pager );
        mAdapter = new PagerAdapter( getSupportFragmentManager( ) );
        mPager.setAdapter( mAdapter );
        mPager.addOnPageChangeListener( new TabLayout.TabLayoutOnPageChangeListener( tabLayout ) );
        tabLayout.setOnTabSelectedListener( new TabLayout.OnTabSelectedListener( ) {
            @Override
            public void onTabSelected( TabLayout.Tab tab ) {
                mPager.setCurrentItem( tab.getPosition( ) );
            }

            @Override
            public void onTabUnselected( TabLayout.Tab tab ) {

            }

            @Override
            public void onTabReselected( TabLayout.Tab tab ) {

            }
        } );


        this.savedInstanceState = savedInstanceState;
        if ( savedInstanceState == null ) {
            this.intent = getIntent( );
        }
        else {
            currentUser = ( User ) savedInstanceState.getParcelable( CURRENT_USER_KEY );
            currentUserId = ( Integer ) savedInstanceState.getSerializable( CURRENT_USER_ID_KEY );
        }

        googleApiClient = new GoogleApiClient.Builder( this )
                .addConnectionCallbacks( this )
                .addApi( LocationServices.API )
                .build( );

        client = new GoogleApiClient.Builder( this ).addApi( AppIndex.API ).build( );
        Log.v( "MY_STATE_LOG", "main activity creato" );
    }

    @Override
    protected void onStart( ) {
        super.onStart( );

        backgroundThread = new HandlerThread( "background worker" );
        backgroundThread.start( );
        backgroundTasksHandler = new Handler( backgroundThread.getLooper( ) );

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );
        currentUserId = pref.getInt( SettingsFragment.KEY_PREF_USER_ID, -1 );
        if ( currentUserId <= 0 ) {
            Intent intent = new Intent( this, SplashActivity.class );
            startActivity( intent );
            return;
        }

        inviteRunnable = new CheckInvitesRunnable(
                backgroundTasksHandler, getApplicationContext( ), currentUserId
        );

        getUserRunnable = new GetCurrentUserRunnable(
                backgroundTasksHandler
        );

        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) !=
                PackageManager.PERMISSION_GRANTED ) {
            if ( ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION ) ) {

                showMessageOKCancel(
                        "You need to allow access to the current position",
                        new DialogInterface.OnClickListener( ) {
                            @Override
                            public void onClick( DialogInterface dialog, int which ) {
                                ActivityCompat.requestPermissions(
                                        MainActivity.this,
                                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                                        LOCATION_PERMISSION_REQUEST_CODE );
                            }
                        }
                );
            }
            else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            }
        }
        else {
            locationPermissionEnabled = true;
        }

        if ( locationPermissionEnabled ) {
            googleApiClient.connect( );
            /*
            if ( mAdapter.getCurrentMF( ) != null ) {
                mAdapter.getCurrentMF( ).onStart( );
            }
            */
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect( );
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse( "http://host/path" ),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse( "android-app://it.unitn.roadbuddy.app/http/host/path" )
        );
        AppIndex.AppIndexApi.start( client, viewAction );
    }

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        outState.putParcelable( CURRENT_USER_KEY, currentUser );
        outState.putSerializable( CURRENT_USER_ID_KEY, currentUserId );
    }

    private void showMessageOKCancel( String message, DialogInterface.OnClickListener okListener ) {
        new AlertDialog.Builder( MainActivity.this )
                .setMessage( message )
                .setPositiveButton( "OK", okListener )
                .setNegativeButton( "Cancel", null )
                .create( )
                .show( );
    }

    @Override
    protected void onStop( ) {
        try {
            PostgresUtils.getInstance( ).close( );  // FIXME [ed] find a better place
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "on destroy", exc );
        }
        if ( locationPermissionEnabled && googleApiClient.isConnected( ) ) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    googleApiClient, this
            );

            googleApiClient.disconnect( );
        }

        backgroundThread.quit( );

        super.onStop( );
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse( "http://host/path" ),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse( "android-app://it.unitn.roadbuddy.app/http/host/path" )

        );
        AppIndex.AppIndexApi.end( client, viewAction );
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect( );
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults ) {
        switch ( requestCode ) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if ( grantResults.length > 0
                        && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ) {

                    locationPermissionEnabled = true;
                    googleApiClient.connect( );

                    mAdapter = new PagerAdapter( getSupportFragmentManager( ) );
                    mPager.setAdapter( mAdapter );
                }
                else {
                    locationPermissionEnabled = false;
                }
                break;
            }

            default:
                super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        }

    }

    @Override
    public void onConnected( Bundle connectionHint ) {
        // called when the google api client has successfully connected to whatever

        // ask for periodic location updates running the listener on the background worker
        LocationRequest requestType = LocationRequest
                .create( )
                .setPriority( LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY )
                .setInterval( 5 * 60 * 1000 )
                .setFastestInterval( 15 * 1000 );

        if ( ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION )
                == PackageManager.PERMISSION_GRANTED ) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, requestType,
                    this,
                    backgroundThread.getLooper( )
            );
        }
    }

    @Override
    public void onConnectionSuspended( int n ) {

    }

    @Override
    public void onLocationChanged( Location location ) {
        // gets run in a background thread

        try {
            DAOFactory.getUserDAO( ).setCurrentLocation(
                    currentUserId, new LatLng( location.getLatitude( ),
                                               location.getLongitude( ) )
            );
        }
        catch ( BackendException exc ) {
            Log.e( getClass( ).getName( ), "while updating user position", exc );
        }
    }


    public void showChoosenPath( Path path ) {
        mPager.setCurrentItem( 0 );
        // ((MapFragment) mAdapter.getCurrentMF()).setZoomOnTrip(path);
        /*LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.fragment_drawable_path_info_large, null);
        TextView txtPathDescription = (TextView) linearLayout.findViewById(R.id.txtPathDescription);
        TextView txtTotalDistance = (TextView) linearLayout.findViewById(R.id.txtTotalDistance);
        TextView txtTotalDuration = (TextView) linearLayout.findViewById(R.id.txtTotalDuration);
        txtPathDescription.setText(path.getDescription());
        txtTotalDistance.setText("Distance: " + Long.toString(path.getDistance()));
        txtTotalDuration.setText("Expected Duration: " + Long.toString(path.getDuration()));*/

        MapFragment fragment =( MapFragment ) mAdapter.getCurrentMF( );

        fragment.setSLiderStatus( SlidingUpPanelLayout.PanelState.COLLAPSED );
        fragment.showTrip( path );
    }

    public boolean isLocationPermissionEnabled( ) {
        return locationPermissionEnabled;
    }

    class GetCurrentUserRunnable implements Runnable {

        public static final int INTERVAL = 15 * 1000;

        Handler handler;

        public GetCurrentUserRunnable( Handler handler ) {
            this.handler = handler;

            handler.post( this );
        }

        @Override
        public void run( ) {
            try {
                currentUser = DAOFactory.getUserDAO( ).getUser( currentUserId );
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), "while retrieving current user", exc );
            }

            handler.postDelayed( this, INTERVAL );
        }
    }
}