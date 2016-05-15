package it.unitn.roadbuddy.app;


import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import it.unitn.roadbuddy.app.backend.models.PointOfInterest;


public class RestState implements NFAState,
                                  OnMapClickListener,
                                  OnMapLongClickListener,
                                  GoogleMap.OnMarkerClickListener,
                                  OnCameraChangeListener {

    MapFragment fragment;
    LinearLayout buttonBar;
    Button btnAddPoi;
    Button btnAddPath;

    @Override
    public void onStateEnter( final NFA nfa, MapFragment fragment ) {
        this.fragment = fragment;

        fragment.googleMap.setOnMapClickListener( this );
        fragment.googleMap.setOnMapLongClickListener( this );
        fragment.googleMap.setOnCameraChangeListener( this );
        fragment.googleMap.setOnMarkerClickListener( this );

        buttonBar = ( LinearLayout ) fragment.setCurrentMenuBar( R.layout.rest_buttons_layout );
        buttonBar.setVisibility( View.INVISIBLE );

        btnAddPath = ( Button ) buttonBar.findViewById( R.id.btnAddPath );
        btnAddPath.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                nfa.Transition( new AddPathState( ) );
            }
        } );

        btnAddPoi = ( Button ) buttonBar.findViewById( R.id.btnAddPoi );
        btnAddPoi.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                nfa.Transition( new AddPOIState( ) );
            }
        } );

        fragment.RefreshMapContent( );
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        fragment.googleMap.setOnMapClickListener( null );
        fragment.googleMap.setOnMapLongClickListener( null );
        fragment.googleMap.setOnCameraChangeListener( null );
        fragment.googleMap.setOnMarkerClickListener( null );

        fragment.removeMenuBar( );
    }

    @Override
    public boolean onMarkerClick( Marker m ) {
        PointOfInterest selected = fragment.shownPOIs.get( m );
        if ( selected != null ) {
            fragment.selectedPOI = selected;
        }

        return false;
    }

    @Override
    public void onMapClick( LatLng point ) {
        fragment.toggleMenuBar( );
        fragment.selectedPOI = null;
    }

    @Override
    public void onMapLongClick( final LatLng point ) {

    }

    @Override
    public void onCameraChange( final CameraPosition position ) {
        fragment.RefreshMapContent( );
    }
}
