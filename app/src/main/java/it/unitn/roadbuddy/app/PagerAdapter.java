package it.unitn.roadbuddy.app;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.text.LoginFilter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs = 3;
    String view;
    Fragment currentMP;

    public PagerAdapter( FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem( int position ) {

        switch ( position ) {
            case 0:
                return  currentMP = new MapFragment();
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

    public void setView(String v){
        this.view = v;
    }

    public void getTrip( ){
        if(view != null){
            String tempView = this.view;
            this.view = null;
            ((MapFragment) currentMP).sliderLayout.setView(R.layout.button_layout_ap);
        }
    }

    public Fragment getCurrentMP() {
        return currentMP;
    }

}
