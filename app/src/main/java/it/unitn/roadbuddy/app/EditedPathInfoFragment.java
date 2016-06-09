package it.unitn.roadbuddy.app;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;
import it.unitn.roadbuddy.app.backend.models.Path;

import java.util.ArrayList;
import java.util.List;

public class EditedPathInfoFragment extends SliderContentFragment {

    ListView lstWaypoints;
    TextView txtTotalDistance;
    TextView txtTotalDuration;

    List<WaypointInfo> waypoints = new ArrayList<>( );
    DynamicViewArrayAdapter adapter;

    long totalDistance = 0;
    long totalDuration = 0;

    public static EditedPathInfoFragment newInstance( ) {
        EditedPathInfoFragment fragment = new EditedPathInfoFragment( );
        fragment.largeViewId = R.layout.fragment_edited_path_info_large;
        return fragment;
    }

    @Override
    public void onViewExpand( ) {
        super.onViewExpand( );
        lstWaypoints.setVisibility( View.VISIBLE );
    }

    @Override
    public void onViewShrink( ) {
        super.onViewShrink( );
        lstWaypoints.setVisibility( View.INVISIBLE );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        View view = super.onCreateView( inflater, container, savedInstanceState );

        lstWaypoints = ( ListView ) view.findViewById( R.id.lstWaypoints );
        txtTotalDistance = ( TextView ) view.findViewById( R.id.txtTotalDistance );
        txtTotalDuration = ( TextView ) view.findViewById( R.id.txtTotalDuration );

        adapter = new DynamicViewArrayAdapter( getContext( ) );
        lstWaypoints.setAdapter( adapter );

        updateSummary( );

        return view;
    }

    void updateSummary( ) {
        txtTotalDistance.setText(
                String.format( getString( R.string.path_edit_total_distance ),
                               Path.formatDistance( totalDistance )
                ) );

        txtTotalDuration.setText(
                String.format( getString( R.string.path_edit_total_duration ),
                               Path.formatDuration( totalDuration )
                ) );
    }

    public void popWaypoint( ) {
        deleteWaypoint( waypoints.size( ) - 1 );
    }

    public void appendWaypoint( LatLng point, long distanceTo, long durationTo, String name ) {
        addWaypoint( waypoints.size( ), point, distanceTo, durationTo, name );
    }

    public void addWaypoint( int position, LatLng point, long distanceTo, long durationTo, String name ) {
        WaypointInfo waypoint = new WaypointInfo( point, distanceTo, durationTo, name );
        waypoints.add( position, waypoint );
        adapter.insert( waypoint, position );

        totalDistance += waypoint.getDistanceTo( );
        totalDuration += waypoint.getDurationTo( );

        updateSummary( );
    }

    public void deleteWaypoint( int position ) {
        WaypointInfo waypoint = waypoints.get( position );
        waypoints.remove( position );
        adapter.remove( waypoint );

        totalDistance -= waypoint.getDistanceTo( );
        totalDuration -= waypoint.getDurationTo( );

        updateSummary( );
    }

    public int deleteWaypoint( LatLng point ) {
        for ( int i = 0; i < waypoints.size( ); i++ ) {
            if ( waypoints.get( i ).point.equals( point ) ) {
                deleteWaypoint( i );
                return i;
            }
        }

        return -1;
    }

    class WaypointInfo implements DynamicViewArrayAdapter.Listable {
        private LatLng point;
        private long distanceTo;
        private long durationTo;
        private String description;

        public WaypointInfo( LatLng point, long distanceTo, long durationTo, String description ) {
            this.point = point;
            this.distanceTo = distanceTo;
            this.durationTo = durationTo;
            this.description = description;
        }

        public LatLng getPoint( ) {
            return point;
        }

        public long getDistanceTo( ) {
            return distanceTo;
        }

        public long getDurationTo( ) {
            return durationTo;
        }

        public String getDescription( ) {
            return description;
        }

        public View getView( int position, View convertView, ViewGroup parent ) {
            WaypointInfo waypoint = waypoints.get( position );

            TextView txt = new TextView( getContext( ) );
            txt.setText( String.format(
                    "%d) %s - Distance: %s, Duration %s", position + 1,
                    waypoint.getDescription( ),
                    Path.formatDistance( waypoint.getDistanceTo( ) ),
                    Path.formatDuration( waypoint.getDurationTo( ) )
            ) );

            return txt;
        }
    }
}
