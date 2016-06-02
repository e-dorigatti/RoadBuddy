package it.unitn.roadbuddy.app.backend.models;


import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Path {

    protected List<List<LatLng>> legs = new ArrayList<>( );
    protected long owner;
    private long id;

    private long duration;
    private long distance;

    public Path( long id, long owner, long distance, long duration ) {
        this.id = id;
        this.owner = owner;

        setDistance( distance );
        setDuration( duration );
    }

    public long getId( ) {
        return id;
    }

    public long getOwner( ) {
        return owner;
    }

    public List<List<LatLng>> getLegs( ) {
        return new ArrayList<>( legs );
    }

    public void setLegs( List<List<LatLng>> legs ) {
        this.legs = legs;
    }

    @Override
    public boolean equals( Object other ) {
        return other != null && other instanceof Path &&
                this.id == ( ( Path ) other ).id;
    }

    public long getDuration( ) {
        return duration;
    }

    public void setDuration( long duration ) {
        this.duration = duration;
    }

    public long getDistance( ) {
        return distance;
    }

    public void setDistance( long distance ) {
        this.distance = distance;
    }

    public static String formatDistance( long distance ) {
        if ( distance <= 0 )
            return "-";
        if ( distance < 1000 )
            return String.format( "%d m", distance );
        else return String.format( "%s km", new DecimalFormat( "#.#" ).format( distance / 1000f ) );
    }

    public static String formatDuration( long duration ) {
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
}

