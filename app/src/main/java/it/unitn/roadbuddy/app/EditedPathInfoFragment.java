package it.unitn.roadbuddy.app;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
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
                               formatDistance( totalDistance )
                ) );

        txtTotalDuration.setText(
                String.format( getString( R.string.path_edit_total_duration ),
                               formatDuration( totalDuration )
                ) );
    }

    public void appendWaypoint( LatLng point, long distanceTo, long durationTo ) {
        addWaypoint( waypoints.size( ), point, distanceTo, durationTo );
    }

    public void addWaypoint( int position, LatLng point, long distanceTo, long durationTo ) {
        WaypointInfo waypoint = new WaypointInfo( point, distanceTo, durationTo );
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

    String formatDistance( long distance ) {
        if ( distance <= 0 )
            return "-";
        if ( distance < 1000 )
            return String.format( "%d m", distance );
        else return String.format( "%s km", new DecimalFormat( "#.#" ).format( distance / 1000f ) );
    }

    String formatDuration( long duration ) {
        long totalSeconds = duration % 60;
        long totalMinutes = duration / 60;
        long totalHours = totalMinutes / 60;

        if ( duration <= 0 )
            return "-";
        else if ( totalHours > 0 )
            return String.format( "%d h %d mim", totalHours, totalMinutes % 60 );
        else if ( totalMinutes > 0 )
            return String.format( "%d min", totalMinutes );
        else return String.format( "%d sec", totalSeconds );
    }

    class WaypointInfo implements DynamicViewArrayAdapter.Listable {
        private LatLng point;
        private long distanceTo;
        private long durationTo;

        public WaypointInfo( LatLng point, long distanceTo, long durationTo ) {
            this.point = point;
            this.distanceTo = distanceTo;
            this.durationTo = durationTo;
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

        public View getView( int position, View convertView, ViewGroup parent ) {
            WaypointInfo waypoint = waypoints.get( position );

            TextView txt = new TextView( getContext( ) );
            txt.setText( String.format(
                    "Waypoint %d - Distance: %s, Duration %s", position + 1,
                    formatDistance( waypoint.getDistanceTo( ) ),
                    formatDuration( waypoint.getDurationTo( ) )
            ) );

            return txt;
        }
    }
}
