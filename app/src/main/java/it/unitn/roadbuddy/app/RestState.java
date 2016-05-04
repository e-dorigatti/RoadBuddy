package it.unitn.roadbuddy.app;


import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;


public class RestState implements NFAState,
                                  OnMapClickListener,
                                  OnMapLongClickListener,
                                  OnMarkerClickListener,
                                  OnCameraChangeListener {

    MainActivity activity;
    LinearLayout buttonBar;
    Button btnAddPoi;
    Button btnAddPath;
    Marker markerShown;

    @Override
    public void onStateEnter( final NFA nfa, MainActivity activity ) {
        this.activity = activity;

        activity.map.setOnMapClickListener( this );
        activity.map.setOnMapLongClickListener( this );
        activity.map.setOnCameraChangeListener( this );
        activity.map.setOnMarkerClickListener( this );

        buttonBar = ( LinearLayout ) activity.setMenuBar( R.layout.rest_buttons_layout );
        buttonBar.setVisibility( View.INVISIBLE );

        btnAddPath = ( Button ) buttonBar.findViewById( R.id.btnAddPath );

        btnAddPoi = ( Button ) buttonBar.findViewById( R.id.btnAddPoi );
        btnAddPoi.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                nfa.Transition( new AddPOIState( ) );
            }
        } );

        activity.RefreshMapContent( );
    }

    @Override
    public void onStateExit( NFA nfa, MainActivity activity ) {
        activity.map.setOnMapClickListener( null );
        activity.map.setOnMapLongClickListener( null );
        activity.map.setOnCameraChangeListener( null );
        activity.map.setOnMarkerClickListener( null );

        activity.removeMenuBar( );
    }

    @Override
    public void onMapClick( LatLng point ) {
        activity.toggleMenuBar( );
        markerShown = null;
    }

    @Override
    public void onMapLongClick( final LatLng point ) {

    }

    @Override
    public boolean onMarkerClick( Marker m ) {
        markerShown = m;
        return false;
    }

    @Override
    public void onCameraChange( final CameraPosition position ) {
        activity.RefreshMapContent( );
        if ( markerShown != null )
            markerShown.showInfoWindow( );
    }
}
