package it.unitn.roadbuddy.app.backend.models;


import com.google.android.gms.maps.model.LatLng;

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
}

