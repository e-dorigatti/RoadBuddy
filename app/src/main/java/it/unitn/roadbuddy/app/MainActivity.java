package it.unitn.roadbuddy.app;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;


public class MainActivity extends AppCompatActivity {

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

        try {
            Class.forName( "org.postgresql.Driver" );  // FIXME [ed] find a better place
        }
        catch ( ClassNotFoundException e ) {
            Log.e( getClass( ).getName( ), "backend exception", e );
            finish( );
        }
    }
}

