package it.unitn.roadbuddy.app.backend.models;


import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Path implements Parcelable {
    public static final Parcelable.Creator<Path> CREATOR
            = new Parcelable.Creator<Path>( ) {

        public Path createFromParcel( Parcel in ) {
            return new Path( in );
        }

        public Path[] newArray( int size ) {
            return new Path[ size ];
        }
    };

    protected int owner;
    private int id;

    private long duration;
    private long distance;

    private String description;
    private List<List<LatLng>> legs = new ArrayList<>( );

    public Path( int id, int owner, long distance, long duration, String description ) {
        this.id = id;
        this.owner = owner;

        setDescription( description );
        setDistance( distance );
        setDuration( duration );
    }

    public Path( Parcel parcel ) {
        owner = parcel.readInt( );
        id = parcel.readInt( );
        duration = parcel.readLong( );
        distance = parcel.readLong( );
        description = parcel.readString( );

        legs = new ArrayList<>( );
        for ( int i = parcel.readInt( ); i > 0; i++ ) {
            List<LatLng> leg = new ArrayList<>( );
            parcel.readTypedList( leg, LatLng.CREATOR );
            legs.add( leg );
        }
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

    public String getDescription( ) {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    @Override
    public void writeToParcel( Parcel parcel, int i ) {
        parcel.writeInt( owner );
        parcel.writeInt( id );
        parcel.writeLong( duration );
        parcel.writeLong( distance );
        parcel.writeString( description );

        parcel.writeInt( legs.size( ) );
        for ( List<LatLng> leg : legs )
            parcel.writeTypedList( leg );
    }

    @Override
    public int describeContents( ) {
        return 0;
    }
}

