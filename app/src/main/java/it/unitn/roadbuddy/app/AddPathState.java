package it.unitn.roadbuddy.app;


import android.graphics.Color;
import android.os.AsyncTask;
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
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;

import java.util.ArrayList;
import java.util.List;

public class AddPathState implements NFAState,
                                     GoogleMap.OnMapClickListener,
                                     GoogleMap.OnMapLongClickListener,
                                     GoogleMap.OnMarkerClickListener,
                                     GoogleMap.OnCameraChangeListener {

    private static final String APIKEY = "AIzaSyDIbDr0ZKOgQomCZtKmLKUmL4W5pPFyRoU";

    boolean requestPending = false;

    List<WaypointInfo> path = new ArrayList<>( );
    GoogleMap map;
    MainActivity activity;
    LinearLayout lyWaypointControl;
    LinearLayout lyOkCancel;
    Marker selectedMarker;

    @Override
    public void onStateEnter( final NFA nfa, final MainActivity activity ) {
        this.activity = activity;
        this.map = activity.map;

        map.setOnMapLongClickListener( this );
        map.setOnCameraChangeListener( this );
        map.setOnMarkerClickListener( this );
        map.setOnMapClickListener( this );
        map.clear( );

        lyWaypointControl = ( LinearLayout ) activity.getLayoutInflater( ).inflate(
                R.layout.waypoint_control, activity.mainFrameLayout, false
        );
        lyWaypointControl.findViewById( R.id.btnDeleteWaypoint ).setOnClickListener(
                new View.OnClickListener( ) {
                    @Override
                    public void onClick( View v ) {
                        deleteSelectedWaypoint( );
                    }
                } );

        lyOkCancel = ( LinearLayout ) activity.setCurrentMenuBar( R.layout.ok_cancel_layout );
        lyOkCancel.findViewById( R.id.btnOk ).setOnClickListener(
                new View.OnClickListener( ) {
                    @Override
                    public void onClick( View v ) {
                        if ( path.size( ) > 0 ) {
                            new SavePathAsync( nfa ).executeOnExecutor(
                                    AsyncTask.THREAD_POOL_EXECUTOR,
                                    path
                            );
                        }
                        else {
                            activity.showToast( R.string.new_path_cancel );
                            nfa.Transition( new RestState( ) );
                        }
                    }
                } );

        lyOkCancel.findViewById( R.id.btnCancel ).setOnClickListener(
                new View.OnClickListener( ) {
                    @Override
                    public void onClick( View v ) {
                        activity.showToast( "No point added..." );
                        nfa.Transition( new RestState( ) );
                    }
                } );

        activity.showToast( R.string.long_tap_to_add );
    }

    @Override
    public void onStateExit( NFA nfa, MainActivity activity ) {
        map.setOnMapLongClickListener( null );
        map.setOnCameraChangeListener( null );
        map.setOnMapClickListener( null );
        map.setOnMarkerClickListener( null );
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
                activity.getApplicationContext( ),
                points, 5, Color.BLUE
        );
        waypoint.polylineTo = map.addPolyline( opts );

        return waypoint;
    }

    @Override
    public void onMapLongClick( final LatLng point ) {
        if ( requestPending ) {
            activity.showToast( R.string.wait_for_network );
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
        activity.setCurrentMenuBar( lyWaypointControl );
        selectedMarker = m;

        return false;
    }

    @Override
    public void onMapClick( LatLng point ) {
        activity.setCurrentMenuBar( lyOkCancel );
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
                fail( );
            }

            requestPending = false;
        }

        @Override
        public void onDirectionFailure( Throwable t ) {
            Log.d( "roadbuddy", "while getting directions", t );
            if ( t.getMessage( ) != null ) {
                activity.showToast( t.getMessage( ) );
            }

            fail( );
            requestPending = false;
        }

        void fail( ) {
            WaypointInfo waypoint = path.get( path.size( ) - 1 );

            waypoint.marker.remove( );
            path.remove( waypoint );
            activity.showToast( "NOK" );
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
                activity.showToast( "NOK" );
            }

            requestPending = false;
        }

        @Override
        public void onDirectionFailure( Throwable t ) {
            Log.d( "roadbuddy", "while getting directions", t );
            if ( t.getMessage( ) != null ) {
                activity.showToast( t.getMessage( ) );
            }

            requestPending = false;
        }
    }

    class SavePathAsync extends AsyncTask<List<WaypointInfo>, Integer, Boolean> {
        NFA nfa;
        String errorMessage;

        public SavePathAsync( NFA nfa ) {
            this.nfa = nfa;
        }

        @Override
        protected Boolean doInBackground( List<WaypointInfo>... waypoints ) {
            Path path = new Path( -1 );
            for ( int i = 1; i < waypoints[ 0 ].size( ); i++ ) {
                WaypointInfo waypoint = waypoints[ 0 ].get( i );
                path.addLeg( waypoint.legTo.getDirectionPoint( ) );
            }

            try {
                DAOFactory.getPathDAO( ).AddPath( path );
                return true;
            }
            catch ( Exception exc ) {
                Log.e( "roadbuddy", "save path async", exc );
                errorMessage = exc.getMessage( );
                return false;
            }
        }

        @Override
        protected void onPostExecute( Boolean success ) {
            if ( success ) {
                for ( WaypointInfo waypoint : path ) {
                    if ( waypoint.polylineTo != null )
                        waypoint.polylineTo.remove( );
                }

                activity.showToast( R.string.new_path_saved );
                nfa.Transition( new RestState( ) );
            }
            else {
                if ( errorMessage != null ) {
                    activity.showToast( errorMessage );
                }
                else {
                    activity.showToast( R.string.generic_backend_error );
                }
            }
        }
    }
}
