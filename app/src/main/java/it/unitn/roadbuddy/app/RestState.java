package it.unitn.roadbuddy.app;


import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import com.github.clans.fab.FloatingActionButton;
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
    RelativeLayout buttonBar;
    FloatingActionButton btnAddPoi;
    FloatingActionButton btnAddPath;
    FloatingActionButton btnStartRiding;

    @Override
    public void onStateEnter( final NFA nfa, MapFragment fragment, Bundle savedInstanceState ) {
        this.fragment = fragment;

        fragment.googleMap.setOnMapClickListener( this );
        fragment.googleMap.setOnMapLongClickListener( this );
        fragment.googleMap.setOnCameraChangeListener( this );
        fragment.googleMap.setOnMarkerClickListener( this );
        fragment.googleMap.setOnPolylineClickListener( this );

        buttonBar = ( RelativeLayout ) fragment.mainLayout.setView( R.layout.buttons_layout_rs );

        btnAddPath = ( FloatingActionButton ) buttonBar.findViewById( R.id.btnAddPath );
        btnAddPath.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                nfa.Transition( new AddPathState( ), null );
            }
        } );

        btnAddPoi = ( FloatingActionButton ) buttonBar.findViewById( R.id.btnAddPoi );
        btnAddPoi.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                nfa.Transition( new AddPOIState( ), null );
            }
        } );

        btnStartRiding = ( FloatingActionButton ) buttonBar.findViewById( R.id.btnStartRiding );
        btnStartRiding.setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View v ) {
                nfa.Transition( new NavigationState( null, null ), null );
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
        fragment.googleMap.setOnPolylineClickListener( null );

        fragment.mainLayout.removeView( );
        fragment.taskManager.stopRunningTasksOfType( MapFragment.RefreshMapAsync.class );
    }

    @Override
    public void onRestoreInstanceState( Bundle savedInstanceState ) {

    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState ) {

    }

    void onGraphicItemSelected( String itemId ) {
        Drawable selected = fragment.shownDrawablesByMapId.get( itemId );
        if ( selected != null )
            fragment.setSelectedDrawable( selected );
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
