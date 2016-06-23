package it.unitn.roadbuddy.app;


import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.gms.maps.model.LatLng;
import it.unitn.roadbuddy.app.backend.models.Path;

import java.util.ArrayList;

public class EditedPathInfoFragment extends SliderContentFragment {

    ListView lstWaypoints;
    TextView txtTotalDistance;
    TextView txtTotalDuration;

    ArrayList<WaypointInfo> waypoints = new ArrayList<>( );
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

        for ( WaypointInfo waypoint : waypoints ) {
            waypoint.setContext( getContext( ) );
        }

        adapter.addAll( waypoints );

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
        WaypointInfo waypoint = new WaypointInfo( point, distanceTo, durationTo, name, getContext( ) );
        waypoints.add( position, waypoint );

        totalDistance += waypoint.getDistanceTo( );
        totalDuration += waypoint.getDurationTo( );

        if ( adapter != null ) {
            adapter.insert( waypoint, position );
            updateSummary( );
        }
    }

    public void deleteWaypoint( int position ) {
        WaypointInfo waypoint = waypoints.get( position );
        waypoints.remove( position );

        Utils.Assert( adapter.getPosition( waypoint ) >= 0, false );
        adapter.remove( waypoint );

        totalDistance -= waypoint.getDistanceTo( );
        totalDuration -= waypoint.getDurationTo( );

        updateSummary( );
    }

    public int deleteWaypoint( LatLng point ) {
        for ( int i = 0; i < waypoints.size( ); i++ ) {
            if ( waypoints.get( i ).getPoint( ).equals( point ) ) {
                deleteWaypoint( i );
                return i;
            }
        }

        return -1;
    }
}


class WaypointInfo implements DynamicViewArrayAdapter.Listable, Parcelable {

    public static final Parcelable.Creator<WaypointInfo> CREATOR
            = new Parcelable.Creator<WaypointInfo>( ) {

        public WaypointInfo createFromParcel( Parcel in ) {
            return new WaypointInfo( in );
        }

        public WaypointInfo[] newArray( int size ) {
            return new WaypointInfo[ size ];
        }
    };

    private LatLng point;
    private long distanceTo;
    private long durationTo;
    private String description;
    private Context context;

    public WaypointInfo( LatLng point, long distanceTo, long durationTo,
                         String description, Context context ) {

        this.point = point;
        this.distanceTo = distanceTo;
        this.durationTo = durationTo;
        this.description = description;
        this.context = context;
    }

    public WaypointInfo( Parcel parcel ) {
        point = parcel.readParcelable( ClassLoader.getSystemClassLoader( ) );
        distanceTo = parcel.readLong( );
        durationTo = parcel.readLong( );
        description = parcel.readString( );
    }

    @Override
    public void writeToParcel( Parcel parcel, int i ) {
        parcel.writeParcelable( point, i );
        parcel.writeLong( distanceTo );
        parcel.writeLong( durationTo );
        parcel.writeString( description );
    }

    @Override
    public int describeContents( ) {
        return 0;
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

    public void setContext( Context context ) {
        this.context = context;
    }

    public View getView( int position, View convertView, ViewGroup parent ) {
        TextView txt = new TextView( context );
        txt.setText( String.format(
                "%d) %s - Distance: %s, Duration %s", position + 1,
                getDescription( ),
                Path.formatDistance( getDistanceTo( ) ),
                Path.formatDuration( getDurationTo( ) )
        ) );

        return txt;
    }
}
