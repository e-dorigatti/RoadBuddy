package it.unitn.roadbuddy.app;


import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.AvoidType;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.constant.Unit;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.List;

public class AddPathState implements NFAState,
                                     GoogleMap.OnMapClickListener,
                                     GoogleMap.OnMapLongClickListener,
                                     GoogleMap.OnMarkerClickListener,
                                     GoogleMap.OnCameraChangeListener {

    private static final String APIKEY = BuildConfig.APIKEY;

    boolean requestPending = false;

    List<WaypointInfo> path = new ArrayList<>( );
    GoogleMap map;
    MapFragment fragment;
    LinearLayout lyWaypointControl;
    LinearLayout lyOkCancel;
    Marker selectedMarker;

    @Override
    public void onStateEnter( final NFA nfa, final MapFragment fragment ) {
        this.fragment = fragment;
        this.map = fragment.googleMap;

        map.setOnMapLongClickListener( this );
        map.setOnCameraChangeListener( this );
        map.setOnMarkerClickListener( this );
        map.setOnMapClickListener( this );
        map.clear( );

        lyWaypointControl = ( LinearLayout ) fragment.getActivity().getLayoutInflater( ).inflate(
                R.layout.waypoint_control, fragment.mainFrameLayout, false
        );
        lyWaypointControl.findViewById( R.id.btnDeleteWaypoint ).setOnClickListener(
                new View.OnClickListener( ) {
                    @Override
                    public void onClick( View v ) {
                        deleteSelectedWaypoint( );
                    }
                } );

        lyOkCancel = ( LinearLayout ) fragment.setCurrentMenuBar( R.layout.ok_cancel_layout );
        lyOkCancel.findViewById( R.id.btnOk ).setOnClickListener(
                new View.OnClickListener( ) {
                    @Override
                    public void onClick( View v ) {
                        savePath( );
                    }
                } );

        lyOkCancel.findViewById( R.id.btnCancel ).setOnClickListener(
                new View.OnClickListener( ) {
                    @Override
                    public void onClick( View v ) {
                        fragment.showToast( "No point added..." );
                        nfa.Transition( new RestState( ) );
                    }
                } );

        fragment.showToast( R.string.long_tap_to_add );
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        map.setOnMapLongClickListener( null );
        map.setOnCameraChangeListener( null );
        map.setOnMapClickListener( null );
        map.setOnMarkerClickListener( null );
    }

    void savePath( ) {
        // TODO
    }

    MarkerOptions createMarker( LatLng point, int i ) {
        return new MarkerOptions( )
                .position( point )
                .title( Integer.toString( i ) );
    }

    void getDirections( LatLng from, LatLng to, DirectionCallback callback ) {
        GoogleDirection
                .withServerKey( APIKEY )
                .from( from )
                .to( to )
                .avoid( AvoidType.FERRIES )
                .avoid( AvoidType.HIGHWAYS )
                .avoid( AvoidType.INDOOR )
                .avoid( AvoidType.TOLLS )
                .transportMode( TransportMode.DRIVING )
                .unit( Unit.METRIC )
                .alternativeRoute( false )
                .execute( callback );
    }

    void deleteSelectedWaypoint( ) {
        Utils.Assert( selectedMarker != null, true );

        int position;
        for ( position = 0; position < path.size( ); position++ ) {
            if ( path.get( position ).marker.equals( selectedMarker ) ) {
                break;
            }
        }

        Utils.Assert( position < path.size( ), true );

        WaypointInfo waypoint = path.get( position );
        if ( path.size( ) == 1 ) {
            waypoint.marker.remove( );
            path.remove( waypoint );
        }
        else if ( position == 0 ) {
            WaypointInfo next = path.get( position + 1 );
            next.polylineTo.remove( );
            waypoint.marker.remove( );
            path.remove( waypoint );
        }
        else if ( position == path.size( ) - 1 ) {
            waypoint.polylineTo.remove( );
            waypoint.marker.remove( );
            path.remove( waypoint );
        }
        else {
            WaypointInfo previous = path.get( position - 1 );
            WaypointInfo next = path.get( position + 1 );
            getDirections( previous.point, next.point,
                           new DeleteWaypointDirectionReceived( waypoint, next ) );
        }
    }

    WaypointInfo updateWaypoint( WaypointInfo waypoint, Direction direction ) {
        Utils.Assert( direction.isOK( ), true );

        waypoint.legTo = direction.getRouteList( ).get( 0 ).getLegList( ).get( 0 );
        ArrayList<LatLng> points = waypoint.legTo.getDirectionPoint( );
        PolylineOptions opts = DirectionConverter.createPolyline(
                fragment.getActivity().getApplicationContext( ),
                points, 5, Color.BLUE
        );
        waypoint.polylineTo = map.addPolyline( opts );

        return waypoint;
    }

    @Override
    public void onMapLongClick( final LatLng point ) {
        if ( requestPending ) {
            fragment.showToast( R.string.wait_for_network );
            return;
        }

        Marker marker = map.addMarker( createMarker( point, path.size( ) + 1 ) );
        path.add( new WaypointInfo( path.size( ), point, null, marker, null ) );

        if ( path.size( ) > 1 ) {
            Marker from = path.get( path.size( ) - 2 ).marker;

            getDirections( from.getPosition( ), point, new InsertWaypointDirectionReceived( ) );

            requestPending = true;
        }
    }

    @Override
    public boolean onMarkerClick( Marker m ) {
        fragment.setCurrentMenuBar( lyWaypointControl );
        selectedMarker = m;

        return false;
    }

    @Override
    public void onMapClick( LatLng point ) {
        fragment.setCurrentMenuBar( lyOkCancel );
        selectedMarker = null;
    }

    @Override
    public void onCameraChange( final CameraPosition position ) {

    }

    class WaypointInfo {
        int index;

        LatLng point;
        Leg legTo;

        Marker marker;
        Polyline polylineTo;

        public WaypointInfo( int index, LatLng point, Leg legTo, Marker marker, Polyline polylineTo ) {
            this.index = index;
            this.point = point;
            this.legTo = legTo;
            this.marker = marker;
            this.polylineTo = polylineTo;
        }
    }

    class InsertWaypointDirectionReceived implements DirectionCallback {

        @Override
        public void onDirectionSuccess( Direction direction, String boh ) {
            WaypointInfo waypoint = path.get( path.size( ) - 1 );

            if ( direction.isOK( ) ) {
                updateWaypoint( waypoint, direction );
            }
            else {
                waypoint.marker.remove( );
                path.remove( waypoint );
                fragment.showToast( "NOK" );
            }

            requestPending = false;
        }

        @Override
        public void onDirectionFailure( Throwable t ) {
            Log.d( "roadbuddy", "while getting directions", t );
            if ( t.getMessage( ) != null ) {
                fragment.showToast( t.getMessage( ) );
            }

            requestPending = false;
        }
    }

    class DeleteWaypointDirectionReceived implements DirectionCallback {

        WaypointInfo next;
        WaypointInfo toDelete;

        public DeleteWaypointDirectionReceived( WaypointInfo toDelete,
                                                WaypointInfo next ) {
            this.next = next;
            this.toDelete = toDelete;
        }

        @Override
        public void onDirectionSuccess( Direction direction, String boh ) {
            if ( direction.isOK( ) ) {
                next.polylineTo.remove( );

                toDelete.polylineTo.remove( );
                toDelete.marker.remove( );
                path.remove( toDelete );

                updateWaypoint( next, direction );
            }
            else {
                fragment.showToast( "NOK" );
            }

            requestPending = false;
        }

        @Override
        public void onDirectionFailure( Throwable t ) {
            Log.d( "roadbuddy", "while getting directions", t );
            if ( t.getMessage( ) != null ) {
                fragment.showToast( t.getMessage( ) );
            }

            requestPending = false;
        }
    }
}
