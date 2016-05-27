package it.unitn.roadbuddy.app;


import com.github.clans.fab.FloatingActionButton;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;


public class RestState implements NFAState,
                                  OnMapClickListener,
                                  OnMapLongClickListener,
                                  GoogleMap.OnMarkerClickListener,
                                  GoogleMap.OnPolylineClickListener,
                                  OnCameraChangeListener {

    MapFragment fragment;
    LinearLayout buttonBar;
    FloatingActionButton btnAddPoi;
    FloatingActionButton btnAddPath;

    @Override
    public void onStateEnter( final NFA nfa, MapFragment fragment ) {
        this.fragment = fragment;

        fragment.googleMap.setOnMapClickListener( this );
        fragment.googleMap.setOnMapLongClickListener( this );
        fragment.googleMap.setOnCameraChangeListener( this );
        fragment.googleMap.setOnMarkerClickListener( this );
        fragment.googleMap.setOnPolylineClickListener( this );

        buttonBar = ( LinearLayout ) fragment.setCurrentMenuBar( R.layout.rest_buttons_layout );

        btnAddPath = (FloatingActionButton ) buttonBar.findViewById( R.id.btnAddPath );
        btnAddPath.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                nfa.Transition( new AddPathState( ) );
            }
        } );

        btnAddPoi = ( FloatingActionButton ) buttonBar.findViewById( R.id.btnAddPoi );
        btnAddPoi.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                nfa.Transition( new AddPOIState( ) );
            }
        } );

        fragment.RefreshMapContent( );
    }

    void onGraphicItemSelected( String itemId ) {
        Drawable selected = fragment.shownDrawables.get( itemId );
        Utils.Assert( selected != null, false );
        fragment.setSelectedDrawable( selected );
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        fragment.googleMap.setOnMapClickListener( null );
        fragment.googleMap.setOnMapLongClickListener( null );
        fragment.googleMap.setOnCameraChangeListener( null );
        fragment.googleMap.setOnMarkerClickListener( null );
        fragment.googleMap.setOnPolylineClickListener( null );

        fragment.removeMenuBar( );
    }

    @Override
    public boolean onMarkerClick( Marker m ) {
        onGraphicItemSelected( m.getId( ) );

        // prevent default map's behaviour, we will take care of it
        return true;
    }

    @Override
    public void onPolylineClick( Polyline p ) {
        onGraphicItemSelected( p.getId( ) );
    }

    @Override
    public void onMapClick( LatLng point ) {
        //fragment.toggleMenuBar( );
        fragment.setSelectedDrawable( null );
    }

    @Override
    public void onMapLongClick( final LatLng point ) {

    }

    @Override
    public void onCameraChange( final CameraPosition position ) {
        fragment.RefreshMapContent( );
    }
}
