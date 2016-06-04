package it.unitn.roadbuddy.app;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.postgres.PostgresUtils;

import java.sql.SQLException;


public class MainActivity extends AppCompatActivity {

    ViewPager mPager;
    PagerAdapter mAdapter;


    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mPager = ( ViewPager ) findViewById( R.id.pager );
        mAdapter = new PagerAdapter( getSupportFragmentManager( ));
        mPager.setAdapter(mAdapter);



        final ImageButton mapButton = (ImageButton) findViewById(R.id.button_map);
        mapButton.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
        final ImageButton viaggiButton = (ImageButton) findViewById(R.id.button_viaggi);
        final ImageButton impostButton = (ImageButton) findViewById(R.id.button_impostazioni);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position){
                    case 0: mapButton.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
                        viaggiButton.getBackground().clearColorFilter();
                        impostButton.getBackground().clearColorFilter();
                        break;
                    case 1: viaggiButton.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
                        mapButton.getBackground().clearColorFilter();
                        impostButton.getBackground().clearColorFilter();

                        break;
                    case 2: impostButton.getBackground().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);
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
    }

    @Override
    protected void onStart( ) {
        // FIXME [ed] find a better place
        taskManager.startRunningTask( new AsyncInitializeDB( ), true );

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );
        if ( pref.getBoolean( SettingsFragment.KEY_PREF_DEV_ENABLED, false ) ) {
            String userName = pref.getString( SettingsFragment.KEY_PREF_USER_NAME, "<unset>" );
            long userID = pref.getLong( SettingsFragment.KEY_PREF_USER_ID, -1 );

            Toast.makeText( this,
                            String.format( "You are currently running as user %s (id: %d)",
                                           userName, userID ),
                            Toast.LENGTH_SHORT ).show( );
        }

        super.onStart( );
    }

    @Override
    protected void onStop( ) {
        try {
            PostgresUtils.getInstance( ).close( );  // FIXME [ed] find a better place
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "on destroy", exc );
        }

        super.onStop( );
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
