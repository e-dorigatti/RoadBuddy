package it.unitn.roadbuddy.app;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.postgres.PostgresUtils;

import java.sql.SQLException;


public class MainActivity extends AppCompatActivity {

    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        Toolbar toolbar = ( Toolbar ) findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );

        TabLayout tabLayout = ( TabLayout ) findViewById( R.id.tab_layout );
        tabLayout.addTab( tabLayout.newTab( ).setText( R.string.map_tab_name ) );
        tabLayout.addTab( tabLayout.newTab( ).setText( R.string.trips_tab_name ) );
        tabLayout.addTab( tabLayout.newTab( ).setText( R.string.settings_tab_name ) );
        tabLayout.setTabGravity( TabLayout.GRAVITY_FILL );

        final ViewPager viewPager = ( ViewPager ) findViewById( R.id.pager );
        final PagerAdapter adapter = new PagerAdapter( getSupportFragmentManager( ), tabLayout.getTabCount( ) );
        viewPager.setAdapter( adapter );
        viewPager.addOnPageChangeListener( new TabLayout.TabLayoutOnPageChangeListener( tabLayout ) );
        tabLayout.setOnTabSelectedListener( new TabLayout.OnTabSelectedListener( ) {
            @Override
            public void onTabSelected( TabLayout.Tab tab ) {
                viewPager.setCurrentItem( tab.getPosition( ) );
            }

            @Override
            public void onTabUnselected( TabLayout.Tab tab ) {

            }

            @Override
            public void onTabReselected( TabLayout.Tab tab ) {

            }
        } );
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
