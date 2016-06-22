package it.unitn.roadbuddy.app;


import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;

import java.util.ArrayList;
import java.util.List;

public class AddPathState implements NFAState,
                                     GoogleMap.OnMapClickListener,
                                     GoogleMap.OnMapLongClickListener,
                                     GoogleMap.OnMarkerClickListener,
                                     GoogleMap.OnCameraChangeListener {

    /**
     * This is used to keep track of the number of pending requests.
     * <p/>
     * In order to add a waypoint we need two, independent services:
     * the google direction apis (for road directions) and the google
     * reverse geocoding apis (to understand the name of the waypoint).
     * <p/>
     * In order to keep things simple, we force the user to add one
     * waypoint at a time and wait until all the necessary data
     * has been retrieved.
     */
    int pendingRequests = 0;

    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );
    List<WaypointInfo> path = new ArrayList<>( );

    GoogleMap map;
    MapFragment fragment;
    LinearLayout lyOkCancel;
    Marker selectedMarker;

    Handler animationHandler = new Handler( );
    MarkerBounceAnimation animation;

    EditedPathInfoFragment infoFragment;

    @Override
    public void onStateEnter( final NFA nfa, final MapFragment fragment, Bundle savedInstanceStat ) {
        this.fragment = fragment;
        this.map = fragment.googleMap;

        map.setOnMapLongClickListener( this );
        map.setOnCameraChangeListener( this );
        map.setOnMarkerClickListener( this );
        map.setOnMapClickListener( this );
        map.clear( );

        lyOkCancel = ( LinearLayout ) fragment.mainLayout.setView(
                R.layout.button_layout_ap
        );

        lyOkCancel.findViewById( R.id.fatto ).setOnClickListener(
                new View.OnClickListener( ) {
                    @Override
                    public void onClick( View v ) {
                        if ( path.size( ) > 0 ) {
                            if ( !taskManager.isTaskRunning( SavePathAsync.class ) ) {
                                taskManager.startRunningTask( new SavePathAsync(
                                        nfa
                                ), true, path );
                            }
                            else fragment.showToast( R.string.wait_for_async_op_completion );
                        }
                        else {
                            fragment.showToast( R.string.new_path_cancel );
                            nfa.Transition( new RestState( ), null );
                        }
                    }
                } );

        lyOkCancel.findViewById( R.id.elimina ).setOnClickListener(
                new View.OnClickListener( ) {
                    @Override
                    public void onClick( View v ) {
                        deleteSelectedWaypoint( );
                    }
                } );

        lyOkCancel.findViewById( R.id.annulla ).setOnClickListener(
                new View.OnClickListener( ) {
                    @Override
                    public void onClick( View v ) {
                        fragment.showToast( "No point added..." );
                        nfa.Transition( new RestState( ), null );
                    }
                } );

        infoFragment = EditedPathInfoFragment.newInstance( );
        fragment.sliderLayout.setFragment( infoFragment );
        fragment.slidingLayout.setPanelState(
                SlidingUpPanelLayout.PanelState.COLLAPSED
        );

        fragment.showToast( R.string.long_tap_to_add );
    }

    @Override
    public void onStateExit( NFA nfa, MapFragment fragment ) {
        map.setOnMapLongClickListener( null );
        map.setOnCameraChangeListener( null );
        map.setOnMapClickListener( null );
        map.setOnMarkerClickListener( null );

        fragment.slidingLayout.setPanelState(
                SlidingUpPanelLayout.PanelState.HIDDEN
        );

        taskManager.stopRunningTasksOfType( SavePathAsync.class );
        clearPath( );
    }

    @Override
    public void onRestoreInstanceState( Bundle savedInstanceState ) {

    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState ) {

    }

    MarkerOptions createMarker( LatLng point, int i ) {
        return new MarkerOptions( )
                .position( point )
                .title( Integer.toString( i ) );
    }

    void addNewWaypoint( WaypointInfo from, WaypointInfo to, DirectionCallback callback ) {
        // placeholder waypoint, will be filled with the right information
        infoFragment.appendWaypoint(
                to.point, 0, 0, fragment.getString( R.string.path_edit_geocoding_pending )
        );

        GoogleDirection
                .withServerKey( BuildConfig.APIKEY )
                .from( from.point )
                .to( to.point )
                .avoid( AvoidType.FERRIES )
                .avoid( AvoidType.HIGHWAYS )
                .avoid( AvoidType.INDOOR )
                .avoid( AvoidType.TOLLS )
                .transportMode( TransportMode.DRIVING )
                .unit( Unit.METRIC )
                .alternativeRoute( false )
                .execute( callback );

        taskManager.startRunningTask( new RetrieveWaypointDescriptionAsync( true ), true, to );
    }

    void deleteSelectedWaypoint( ) {
        if ( selectedMarker == null )
            return;

        if ( pendingRequests > 0 ) {
            fragment.showToast( R.string.wait_for_async_op_completion );
            return;
        }

        int position;
        for ( position = 0; position < path.size( ); position++ ) {
            if ( path.get( position ).marker.equals( selectedMarker ) ) {
                break;
            }
        }

        WaypointInfo waypoint = path.get( position );
        selectedMarker = null;

        if ( path.size( ) == 1 ) {
            waypoint.marker.remove( );
            path.remove( waypoint );
        }
        else if ( position == 0 ) {
            WaypointInfo next = path.get( position + 1 );
            next.polylineTo.remove( );
            waypoint.marker.remove( );
            path.remove( waypoint );
            infoFragment.deleteWaypoint( 0 );
        }
        else if ( position == path.size( ) - 1 ) {
            waypoint.polylineTo.remove( );
            waypoint.marker.remove( );
            path.remove( waypoint );
            infoFragment.deleteWaypoint( waypoint.point );
        }
        else if ( position > 0 && position < path.size( ) ) {
            WaypointInfo previous = path.get( position - 1 );
            WaypointInfo next = path.get( position + 1 );
            addNewWaypoint( previous, next,
                            new DeleteWaypointDirectionReceived( waypoint, next ) );
            startMarkerAnimation( waypoint.marker );
        }
    }

    void clearPath( ) {
        for ( WaypointInfo waypoint : path ) {
            waypoint.marker.remove( );
            if ( waypoint.polylineTo != null )
                waypoint.polylineTo.remove( );
        }
        path.clear( );
    }

    void startMarkerAnimation( Marker marker ) {
        if ( animation != null )
            animation.stop( );

        animation = new MarkerBounceAnimation( animationHandler, marker );
        animation.start( );
    }

    void stopMarkerAnimation( ) {
        if ( animation != null ) {
            animation.stop( );
            animation = null;
        }
    }

    WaypointInfo updateWaypoint( WaypointInfo waypoint, Direction direction ) {
        Utils.Assert( direction.isOK( ), true );

        waypoint.legTo = direction.getRouteList( ).get( 0 ).getLegList( ).get( 0 );
        ArrayList<LatLng> points = waypoint.legTo.getDirectionPoint( );
        PolylineOptions opts = DirectionConverter.createPolyline(
                fragment.getActivity( ).getApplicationContext( ),
                points, 5, Color.BLUE
        );
        waypoint.polylineTo = map.addPolyline( opts );

        return waypoint;
    }

    @Override
    public void onMapLongClick( final LatLng point ) {
        if ( pendingRequests > 0 ) {
            fragment.showToast( R.string.wait_for_async_op_completion );
            return;
        }

        Marker marker = map.addMarker( createMarker( point, path.size( ) + 1 ) );
        WaypointInfo waypoint = new WaypointInfo( path.size( ), point, null, marker, null, null );
        path.add( waypoint );

        startMarkerAnimation( waypoint.marker );

        if ( path.size( ) > 1 ) {
            WaypointInfo from = path.get( path.size( ) - 2 );
            addNewWaypoint( from, waypoint, new InsertWaypointDirectionReceived( ) );
        }
        else {
            taskManager.startRunningTask( new RetrieveWaypointDescriptionAsync( false ), true, waypoint );
        }
    }

    @Override
    public boolean onMarkerClick( Marker m ) {
        selectedMarker = m;

        return false;
    }

    @Override
    public void onMapClick( LatLng point ) {
        fragment.mainLayout.setView( lyOkCancel );
        selectedMarker = null;
    }

    @Override
    public void onCameraChange( final CameraPosition position ) {

    }

    class InsertWaypointDirectionReceived implements DirectionCallback {

        public InsertWaypointDirectionReceived( ) {
            pendingRequests += 1;
        }

        @Override
        public void onDirectionSuccess( Direction direction, String boh ) {
            WaypointInfo waypoint = path.get( path.size( ) - 1 );

            if ( direction.isOK( ) ) {
                updateWaypoint( waypoint, direction );

                infoFragment.popWaypoint( );
                infoFragment.appendWaypoint(
                        waypoint.point,
                        waypoint.getDistanceTo( ),
                        waypoint.getDurationTo( ),
                        waypoint.locationName
                );
            }
            else {
                fail( );
            }

            pendingRequests -= 1;
            if ( pendingRequests == 0 )
                stopMarkerAnimation( );
        }

        @Override
        public void onDirectionFailure( Throwable t ) {
            Log.d( "roadbuddy", "while getting directions", t );
            if ( t.getMessage( ) != null ) {
                fragment.showToast( t.getMessage( ) );
            }

            fail( );

            pendingRequests -= 1;
            if ( pendingRequests == 0 )
                stopMarkerAnimation( );
        }

        void fail( ) {
            WaypointInfo waypoint = path.get( path.size( ) - 1 );

            infoFragment.popWaypoint( );
            waypoint.marker.remove( );
            path.remove( waypoint );
            fragment.showToast( "NOK" );
        }
    }

    class DeleteWaypointDirectionReceived implements DirectionCallback {

        WaypointInfo next;
        WaypointInfo toDelete;

        public DeleteWaypointDirectionReceived( WaypointInfo toDelete,
                                                WaypointInfo next ) {
            this.next = next;
            this.toDelete = toDelete;
            pendingRequests += 1;
        }

        @Override
        public void onDirectionSuccess( Direction direction, String boh ) {
            if ( direction.isOK( ) ) {
                next.polylineTo.remove( );

                toDelete.polylineTo.remove( );
                toDelete.marker.remove( );

                path.remove( toDelete );
                updateWaypoint( next, direction );

                infoFragment.deleteWaypoint( toDelete.point );

                int position = infoFragment.deleteWaypoint( next.point );
                infoFragment.addWaypoint(
                        position,
                        next.point,
                        next.getDistanceTo( ),
                        next.getDurationTo( ),
                        next.locationName
                );
            }
            else {
                fragment.showToast( "NOK" );
            }

            pendingRequests -= 1;
            if ( pendingRequests == 0 )
                stopMarkerAnimation( );
        }

        @Override
        public void onDirectionFailure( Throwable t ) {
            Log.d( "roadbuddy", "while getting directions", t );
            if ( t.getMessage( ) != null ) {
                fragment.showToast( t.getMessage( ) );
            }

            pendingRequests -= 1;
        }
    }

    class RetrieveWaypointDescriptionAsync extends CancellableAsyncTask<WaypointInfo, Integer, String> {

        String exceptionMessage;
        boolean updateInfoFragment;

        public RetrieveWaypointDescriptionAsync( boolean updateInfoFragment ) {
            super( taskManager );
            this.updateInfoFragment = updateInfoFragment;
            pendingRequests += 1;
        }

        @Override
        protected String doInBackground( WaypointInfo... dest ) {
            GeocodingApiRequest req = new GeocodingApiRequest(
                    new GeoApiContext( ).setApiKey( BuildConfig.APIKEY )
            );
            req.latlng( new com.google.maps.model.LatLng( dest[ 0 ].point.latitude,
                                                          dest[ 0 ].point.longitude ) );

            GeocodingResult[] results;
            try {
                results = req.await( );
            }
            catch ( Exception e ) {
                Log.e( getClass( ).getName( ), "while reverse geocoding", e );
                return null;
            }

            return getWaypointName( results );
        }

        @Override
        protected void onPostExecute( String res ) {
            if ( res == null ) {
                fragment.showToast( R.string.path_edit_geocoding_error );
            }
            else {
                WaypointInfo waypoint = path.get( path.size( ) - 1 );

                waypoint.locationName = res;

                // The first waypoint is not listed in the details fragment
                if ( updateInfoFragment ) {
                    infoFragment.popWaypoint( );
                    infoFragment.appendWaypoint(
                            waypoint.point,
                            waypoint.getDistanceTo( ),
                            waypoint.getDurationTo( ),
                            waypoint.locationName == null ? "???" : waypoint.locationName

                    );
                }
            }

            pendingRequests -= 1;
            if ( pendingRequests == 0 )
                stopMarkerAnimation( );

            super.onPostExecute( res );
        }

        String getWaypointName( GeocodingResult[] results ) {
            if ( results.length == 0 || results[ 0 ].addressComponents.length == 0 )
                return null;

            List<AddressComponentType> priority = new ArrayList<>( );
            priority.add( AddressComponentType.POINT_OF_INTEREST );
            priority.add( AddressComponentType.COLLOQUIAL_AREA );
            priority.add( AddressComponentType.SUBLOCALITY );
            priority.add( AddressComponentType.LOCALITY );
            priority.add( AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_5 );
            priority.add( AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_4 );
            priority.add( AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_3 );

            String bestName = null;
            int bestPriority = priority.size( );

            // loop through all address components and pick the most descriptive one
            for ( AddressComponent component : results[ 0 ].addressComponents ) {

                // a component can have more than one type, so find the most important one
                int index = priority.size( );
                for ( AddressComponentType type : component.types ) {
                    int i = priority.indexOf( type );
                    if ( i >= 0 && i < index )
                        index = i;
                }

                if ( index >= 0 && index < bestPriority ) {
                    bestName = component.longName;
                    bestPriority = index;
                }
            }

            return bestName;
        }
    }

    class SavePathAsync extends CancellableAsyncTask<List<WaypointInfo>, Integer, Boolean> {

        NFA nfa;
        String errorMessage;

        public SavePathAsync( NFA nfa ) {
            super( taskManager );
            this.nfa = nfa;
        }

        @Override
        protected Boolean doInBackground( List<WaypointInfo>... waypoints ) {
            StringBuilder descriptionBuilder = new StringBuilder( );

            Path path = new Path( -1, fragment.getCurrentUserId( ), 0, 0, null );

            List<List<LatLng>> legs = new ArrayList<>( );
            long distance = 0;
            long duration = 0;

            int waypointCount = waypoints[ 0 ].size( );
            for ( int i = 0; i < waypointCount; i++ ) {
                WaypointInfo waypoint = waypoints[ 0 ].get( i );
                if ( i > 0 ) {
                    legs.add( waypoint.legTo.getDirectionPoint( ) );

                    distance += waypoint.getDistanceTo( );
                    duration += waypoint.getDurationTo( );
                }

                if ( waypoint.locationName != null ) {
                    descriptionBuilder.append( waypoint.locationName );
                    if ( i < waypointCount - 1 )
                        descriptionBuilder.append( " - " );
                }
            }

            path.setLegs( legs );
            path.setDistance( distance );
            path.setDuration( duration );
            path.setDescription( descriptionBuilder.toString( ) );

            try {
                DAOFactory.getPathDAO( ).AddPath( path );
                return true;
            }
            catch ( Exception exc ) {
                Log.e( getClass( ).getName( ), "save path async", exc );
                errorMessage = exc.getMessage( );
                return false;
            }
        }

        @Override
        protected void onPostExecute( Boolean success ) {
            if ( success ) {
                fragment.showToast( R.string.new_path_saved );
                nfa.Transition( new RestState( ), null );

            }
            else {
                if ( errorMessage != null ) {
                    fragment.showToast( errorMessage );
                }
                else {
                    fragment.showToast( R.string.generic_backend_error );
                }
            }

            super.onPostExecute( success );
        }
    }
}


class MarkerBounceAnimation implements Runnable {
    Handler handler;
    Marker marker;

    boolean stop = false;
    float bounceHeight = 0.75f;
    int bounceDuration = 40;
    int fps = 60;

    int frame = 0;

    public MarkerBounceAnimation( Handler handler, Marker marker ) {
        this.handler = handler;
        this.marker = marker;
    }

    public void start( ) {
        handler.postDelayed( this, 1000 / fps );
    }

    public void stop( ) {
        this.stop = true;
    }

    @Override
    public void run( ) {
        frame += 1;

        float height = ( float ) ( bounceHeight * Math.max(
                0.0, Math.sin( 2 * Math.PI * frame / bounceDuration )
        ) );

        marker.setAnchor( 0.5f, 1.0f + height );

        // when stopped keep running until marker lands, then reset its position
        if ( !stop || height > 0.001f )
            start( );
        else marker.setAnchor( 0.5f, 1.0f );
    }
}

class WaypointInfo {
    int index;

    LatLng point;
    Leg legTo;

    Marker marker;
    Polyline polylineTo;

    String locationName;

    public WaypointInfo( int index, LatLng point, Leg legTo, Marker marker,
                         Polyline polylineTo, String locationName ) {
        this.index = index;
        this.point = point;
        this.legTo = legTo;
        this.marker = marker;
        this.polylineTo = polylineTo;
        this.locationName = locationName;
    }

    public long getDistanceTo( ) {
        if ( legTo != null )
            return Long.parseLong( legTo.getDistance( ).getValue( ) );
        else return 0;
    }

    public long getDurationTo( ) {
        if ( legTo != null )
            return Long.parseLong( legTo.getDuration( ).getValue( ) );
        else return 0;
    }
}