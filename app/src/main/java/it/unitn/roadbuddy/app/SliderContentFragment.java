package it.unitn.roadbuddy.app;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;


/**
 * This fragment is used to show information in the bottom slider
 * of the map. It provides facilities to show two different layouts
 * based on the slider status: retracted or extended.
 */
public abstract class SliderContentFragment extends Fragment {

    public static final String
            SMALL_VIEW_KEY = "small-view",
            LARGE_VIEW_KEY = "large-view";

    protected Integer smallViewId = null;
    protected Integer largeViewId = null;
    FrameLayout mainLayout;
    View smallView;
    View largeView;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        super.onCreateView( inflater, container, savedInstanceState );

        if ( savedInstanceState != null ) {
            if ( smallViewId == null )
                smallViewId = ( Integer ) savedInstanceState.getSerializable( SMALL_VIEW_KEY );

            if ( largeViewId == null )
                largeViewId = ( Integer ) savedInstanceState.getSerializable( LARGE_VIEW_KEY );
        }

        mainLayout = ( FrameLayout ) inflater.inflate(
                R.layout.drawable_info_fragment_layout, container, false
        );

        if ( smallViewId != null ) {
            smallView = inflater.inflate( smallViewId, mainLayout, false );
            mainLayout.addView( smallView );
        }

        if ( largeViewId != null ) {
            largeView = inflater.inflate( largeViewId, mainLayout, false );
            if ( smallView == null )
                mainLayout.addView( largeView );
        }

        return mainLayout;
    }

    /**
     * Called when the containing view is expanded
     * allows the fragment to show additional information
     */
    public void onViewExpand( ) {
        if ( smallView != null && largeView != null ) {
            mainLayout.removeView( smallView );
            mainLayout.addView( largeView );
        }
    }

    /**
     * Called when the containing view is shrank
     * allows the fragment to show the most important details only
     */
    public void onViewShrink( ) {
        if ( smallView != null && largeView != null ) {
            mainLayout.removeView( largeView );
            mainLayout.addView( smallView );
        }
    }
}
