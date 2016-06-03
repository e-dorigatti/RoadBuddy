package it.unitn.roadbuddy.app;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;


public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs = 3;
    Fragment currentF;

    public PagerAdapter( FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem( int position ) {
        switch ( position ) {
            case 0:
                return currentF = new MapFragment( );
            case 1:
                return currentF = new TripsFragment( );
            case 2:
                return currentF = new SettingsFragment( );
            default:
                return null;
        }
    }

    @Override
    public int getCount( ) {
        return mNumOfTabs;
    }

}
