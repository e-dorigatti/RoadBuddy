package it.unitn.roadbuddy.app;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
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

    ViewPager mPager;
    PagerAdapter mAdapter;
    ImageButton mapButton;
    ImageButton viaggiButton;
    ImageButton impostButton;

    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager();

    GoogleApiClient googleApiClient;

    HandlerThread backgroundThread;
    Handler backgroundTasksHandler;

    long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mPager = (ViewPager) findViewById(R.id.pager);
        mAdapter = new PagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mAdapter);

        mapButton = (ImageButton) findViewById(R.id.button_map);
        viaggiButton = (ImageButton) findViewById(R.id.button_viaggi);
        impostButton = (ImageButton) findViewById(R.id.button_impostazioni);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        mapButton.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
                        viaggiButton.getBackground().clearColorFilter();
                        impostButton.getBackground().clearColorFilter();
                        mAdapter.getTrip();
                        break;
                    case 1:
                        viaggiButton.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
                        mapButton.getBackground().clearColorFilter();
                        impostButton.getBackground().clearColorFilter();

                        break;
                    case 2:
                        impostButton.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
                        mapButton.getBackground().clearColorFilter();
                        viaggiButton.getBackground().clearColorFilter();
                        break;
                }
                mapButton.invalidate();
                viaggiButton.invalidate();
                impostButton.invalidate();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mapButton.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);

        mapButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mPager.setCurrentItem(0);
                return false;
            }
        });
        viaggiButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mPager.setCurrentItem(1);
                return false;
            }
        });
        impostButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mPager.setCurrentItem(2);
                return false;
            }
        });

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    protected void onStart() {

        // FIXME [ed] find a better place
        taskManager.startRunningTask(new AsyncInitializeDB(), true);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getBoolean(SettingsFragment.KEY_PREF_DEV_ENABLED, false)) {
            String userName = pref.getString(SettingsFragment.KEY_PREF_USER_NAME, "<unset>");
            currentUserId = pref.getLong(SettingsFragment.KEY_PREF_USER_ID, -1);

            Toast.makeText(this,
                    String.format("You are currently running as user %s (id: %d)",
                            userName, currentUserId),
                    Toast.LENGTH_SHORT).show();
        } else {
            // TODO handle *real* app users, with a login etc
            currentUserId = 1;
        }

        backgroundThread = new HandlerThread("background worker");
        backgroundThread.start();
        backgroundTasksHandler = new Handler(backgroundThread.getLooper());

        googleApiClient.connect();

        super.onStart();
    }

    @Override
    protected void onStop() {
        try {
            PostgresUtils.getInstance().close();  // FIXME [ed] find a better place
        } catch (SQLException exc) {
            Log.e(getClass().getName(), "on destroy", exc);
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient, this
        );

        googleApiClient.disconnect();
        backgroundThread.quit();

        super.onStop();
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, requestType,
                this,
                backgroundThread.getLooper()
        );
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