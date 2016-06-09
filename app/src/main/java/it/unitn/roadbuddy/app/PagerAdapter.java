package it.unitn.roadbuddy.app;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;


public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs = 3;
    Fragment currentMF;
    Fragment currentTF;

    public PagerAdapter( FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem( int position ) {

        switch ( position ) {
            case 0:
                return  this.currentMF = new MapFragment();
            case 1:
                return  this.currentTF = new TripsFragment( );
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

    public Fragment getCurrentMF() {
        return currentMF;
    }

    public Fragment getCurrentTF() {
        return currentTF;
    }

}
