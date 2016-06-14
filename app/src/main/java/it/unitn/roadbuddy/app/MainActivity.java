package it.unitn.roadbuddy.app;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.postgres.PostgresUtils;

import java.sql.SQLException;


public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
                   LocationListener {

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    boolean locationPermissionEnabled = false;

    ViewPager mPager;
    PagerAdapter mAdapter;

    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );
    GoogleApiClient googleApiClient;
    HandlerThread backgroundThread;
    Handler backgroundTasksHandler;

    int currentUserId;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        mPager = ( ViewPager ) findViewById( R.id.pager );
        mAdapter = new PagerAdapter( getSupportFragmentManager( ) );
        mPager.setAdapter( mAdapter );

        googleApiClient = new GoogleApiClient.Builder( this )
                .addConnectionCallbacks( this )
                .addApi( LocationServices.API )
                .build( );
    }

    @Override
    protected void onStart( ) {
        // FIXME [ed] find a better place
        taskManager.startRunningTask( new AsyncInitializeDB( ), true );

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );
        if ( pref.getBoolean( SettingsFragment.KEY_PREF_DEV_ENABLED, false ) ) {
            String userName = pref.getString( SettingsFragment.KEY_PREF_USER_NAME, "<unset>" );
            currentUserId = pref.getInt( SettingsFragment.KEY_PREF_USER_ID, -1 );

            Toast.makeText( this,
                            String.format( "You are currently running as user %s (id: %d)",
                                           userName, currentUserId ), Toast.LENGTH_SHORT ).show( );
        }
        else {
            // TODO handle *real* app users, with a login etc
            currentUserId = 1;
        }

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
            if ( mAdapter.getCurrentMF( ) != null ) {
                mAdapter.getCurrentMF( ).onStart( );
            }
        }

        backgroundThread = new HandlerThread( "background worker" );
        backgroundThread.start( );
        backgroundTasksHandler = new Handler( backgroundThread.getLooper( ) );
        super.onStart( );
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
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults ) {
        switch ( requestCode ) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if ( grantResults.length > 0
                        && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    locationPermissionEnabled = true;
                    mAdapter = new PagerAdapter( getSupportFragmentManager( ) );
                    mPager.setAdapter( mAdapter );

                }
                else {
                    locationPermissionEnabled = false;
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                break;
            }
            default:
                super.onRequestPermissionsResult( requestCode, permissions, grantResults );

                // other 'case' lines to check for other
                // permissions this app might request
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

    public boolean isLocationPermissionEnabled( ) {
        return locationPermissionEnabled;
    }

    class AsyncInitializeDB extends CancellableAsyncTask<Void, Void, Boolean> {

        ProgressDialog waitDialog;

        public AsyncInitializeDB( ) {
            super( taskManager );
        }

        @Override
        protected void onPreExecute( ) {
            waitDialog = new ProgressDialog( MainActivity.this );
            waitDialog.setProgressStyle( ProgressDialog.STYLE_SPINNER );
            waitDialog.setMessage( getString( R.string.app_initial_loading ) );
            waitDialog.setIndeterminate( true );
            waitDialog.setCanceledOnTouchOutside( false );
            waitDialog.show( );
        }

        @Override
        protected Boolean doInBackground( Void... args ) {
            if ( !PostgresUtils.Init( ) )
                return false;

            try {
                PostgresUtils.InitSchemas( );
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), "while initializing database", exc );
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute( Boolean ok ) {
            if ( ok ) {
                waitDialog.hide( );
            }
            else {
                finish( );
            }
        }
    }
}