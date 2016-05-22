package it.unitn.roadbuddy.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.PointOfInterest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MapFragment extends Fragment implements OnMapReadyCallback {


    FrameLayout mainFrameLayout;
    View currentMenuBar;
    GoogleMap googleMap;
    NFA nfa;
    Map<Marker, PointOfInterest> shownPOIs;
    PointOfInterest selectedPOI;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shownPOIs = new HashMap<>( );

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.mainFrameLayout = (FrameLayout) view.findViewById(R.id.mainFrameLayout);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync( this );
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        nfa = new NFA( this, new RestState( ) );
    }

    public void RefreshMapContent( ) {
        LatLngBounds bounds = googleMap.getProjection( ).getVisibleRegion( ).latLngBounds;
        new RefreshMapAsync( ).executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, bounds );
    }


    public View setCurrentMenuBar( int view ) {
        View v = getActivity().getLayoutInflater().inflate( view, mainFrameLayout, false );
        setCurrentMenuBar( v );

        return currentMenuBar;
    }

    public void setCurrentMenuBar( View v ) {
        removeMenuBar( );
        currentMenuBar = v;
        mainFrameLayout.addView( v );
    }

    public void removeMenuBar( ) {
        if ( currentMenuBar != null )
            mainFrameLayout.removeView( currentMenuBar );
        currentMenuBar = null;
    }

    public void showMenuBar( ) {
        if ( currentMenuBar != null )
            currentMenuBar.setVisibility( View.VISIBLE );
    }

    public void hideMenuBar( ) {
        if ( currentMenuBar != null )
            currentMenuBar.setVisibility( View.INVISIBLE );
    }

    public void toggleMenuBar( ) {
        if ( currentMenuBar != null ) {
            if ( currentMenuBar.getVisibility( ) == View.VISIBLE )
                currentMenuBar.setVisibility( View.INVISIBLE );
            else
                currentMenuBar.setVisibility( View.VISIBLE );
        }
    }

    public void showToast( String text ) {
        Toast.makeText( getActivity().getApplicationContext( ), text, Toast.LENGTH_LONG ).show( );
    }

    public void showToast( int textId ) {
        String msg = getString( textId );
        showToast( msg );
    }

    class RefreshMapAsync extends AsyncTask<LatLngBounds, Integer, List<PointOfInterest>> {

        String exceptionMessage;

        @Override
        protected List<PointOfInterest> doInBackground(LatLngBounds... bounds) {
            try {
                return DAOFactory.getPoiDAOFactory().getPOIsInside(
                        getActivity().getApplicationContext(), bounds[0]
                );
            } catch (BackendException e) {
                Log.e("roadbuddy", "backend exception", e);
                exceptionMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<PointOfInterest> points) {
            if (points != null) {
                /**
                 * [ed] overwrite all POIs with fresh data coming from the database, also
                 * redraw all of them except for the currently selected POI
                 *
                 * TODO do not redraw existing POIs, just update their data.
                 * I suspect this would case noticeable flicker
                 */
                for (Map.Entry<Marker, PointOfInterest> entry : shownPOIs.entrySet()) {
                    if (entry.getValue() != selectedPOI) {
                        entry.getKey().remove();
                    }
                }

                shownPOIs.clear();
                for (PointOfInterest p : points) {
                    Marker marker;
                    if (p != selectedPOI) {
                        marker = p.drawToMap(googleMap);
                    } else {
                        marker = selectedPOI.getMarker();
                        p.setMarker(marker);
                    }
                    shownPOIs.put(marker, p);
                }
            } else if (exceptionMessage != null) {
                showToast(exceptionMessage);
            } else {
                showToast(R.string.generic_backend_error);
            }
        }
    }
}