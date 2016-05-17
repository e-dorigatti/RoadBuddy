package it.unitn.roadbuddy.app;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter( FragmentManager fm, int NumOfTabs ) {
        super( fm );
        this.mNumOfTabs = NumOfTabs;
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
    public int getCount( ) {
        return mNumOfTabs;
    }
}
