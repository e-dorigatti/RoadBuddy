package it.unitn.roadbuddy.app;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;


public class PagerAdapter extends FragmentStatePagerAdapter {
    public static final int TAB_COUNT = 3;

    MapFragment mapFragment;
    TripsFragment tripsFragment;
    SettingsFragment settingsFragment;

    public PagerAdapter( FragmentManager fm ) {
        super( fm );
    }

    @Override
    public Fragment getItem( int position ) {
        switch ( position ) {
            case 0:
                return new MapFragment( );
            case 1:
                return new TripsFragment( );
            case 2:
                return new SettingsFragment( );
            default:
                return null;
        }
    }

    @Override
    public Object instantiateItem( ViewGroup container, int position ) {
        Fragment fragment = ( Fragment ) super.instantiateItem( container, position );

        if ( position == 0 )
            mapFragment = ( MapFragment ) fragment;
        else if ( position == 1 )
            tripsFragment = ( TripsFragment ) fragment;
        else if ( position == 2 )
            settingsFragment = ( SettingsFragment ) fragment;

        return fragment;
    }

    @Override
    public int getCount( ) {
        return TAB_COUNT;
    }

    public MapFragment getMapFragment( ) {
        return mapFragment;
    }

    public TripsFragment getTripsFragment( ) {
        return tripsFragment;
    }

    public SettingsFragment getSettingsFragment( ) {
        return settingsFragment;
    }
}
