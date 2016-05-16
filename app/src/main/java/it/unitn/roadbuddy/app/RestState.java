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
import com.google.android.gms.maps.model.Polyline;


public class RestState implements NFAState,
                                  OnMapClickListener,
                                  OnMapLongClickListener,
                                  GoogleMap.OnMarkerClickListener,
                                  GoogleMap.OnPolylineClickListener,
                                  OnCameraChangeListener {

    MainActivity activity;
    LinearLayout buttonBar;
    Button btnAddPoi;
    Button btnAddPath;

    @Override
    public void onStateEnter( final NFA nfa, MainActivity activity ) {
        this.activity = activity;

        activity.map.setOnMapClickListener( this );
        activity.map.setOnMapLongClickListener( this );
        activity.map.setOnCameraChangeListener( this );
        activity.map.setOnMarkerClickListener( this );
        activity.map.setOnPolylineClickListener( this );

        buttonBar = ( LinearLayout ) activity.setCurrentMenuBar( R.layout.rest_buttons_layout );
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

        activity.RefreshMapContent( );
    }

    void onGraphicItemSelected( String itemId ) {
        Drawable selected = activity.shownDrawables.get( itemId );
        Utils.Assert( selected != null, false );
        activity.setSelectedDrawable( selected );
    }

    @Override
    public void onStateExit( NFA nfa, MainActivity activity ) {
        activity.map.setOnMapClickListener( null );
        activity.map.setOnMapLongClickListener( null );
        activity.map.setOnCameraChangeListener( null );
        activity.map.setOnMarkerClickListener( null );
        activity.map.setOnPolylineClickListener( null );

        activity.removeMenuBar( );
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
        activity.toggleMenuBar( );
        activity.setSelectedDrawable( null );
    }

    @Override
    public void onMapLongClick( final LatLng point ) {

    }

    @Override
    public void onCameraChange( final CameraPosition position ) {
        activity.RefreshMapContent( );
    }
}
