package it.unitn.roadbuddy.app.backend.models;


import com.google.android.gms.maps.model.LatLng;
import it.unitn.roadbuddy.app.SerializablePoint;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Path implements Serializable {

    protected int owner;
    private int id;

    private long duration;
    private long distance;

    private String description;

    // LatLng is not serializable, but we need to serialize paths
    // so store a copy of them in a format suitable for serialization
    private transient List<List<LatLng>> legs = new ArrayList<>( );
    private List<List<SerializablePoint>> serializableLegs;

    public Path( int id, int owner, long distance, long duration, String description ) {
        this.id = id;
        this.owner = owner;

        setDescription( description );
        setDistance( distance );
        setDuration( duration );
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

    public int getId( ) {
        return id;
    }

    public int getOwner( ) {
        return owner;
    }

    public List<List<LatLng>> getLegs( ) {
        if ( legs == null && serializableLegs != null ) {
            // first access after de-serialization: restore the legs
            legs = new ArrayList<>( );
            for ( List<SerializablePoint> serializableLeg : serializableLegs ) {
                List<LatLng> leg = new ArrayList<>( );
                for ( SerializablePoint point : serializableLeg )
                    leg.add( point.toLatLng( ) );
                legs.add( leg );
            }

            serializableLegs = null;  // free up some memory
        }

        return new ArrayList<>( legs );
    }

    public void setLegs( List<List<LatLng>> legs ) {
        this.legs = legs;

        if ( legs != null ) {
            serializableLegs = new ArrayList<>( );
            for ( List<LatLng> leg : legs ) {
                List<SerializablePoint> serializableLeg = new ArrayList<>( );
                for ( LatLng point : leg )
                    serializableLeg.add( new SerializablePoint( point ) );
                serializableLegs.add( serializableLeg );
            }
        }
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

    public String getDescription( ) {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }
}

