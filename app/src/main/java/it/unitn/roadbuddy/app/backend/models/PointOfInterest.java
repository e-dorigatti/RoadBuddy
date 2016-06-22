package it.unitn.roadbuddy.app.backend.models;


import java.io.Serializable;

public abstract class PointOfInterest implements Serializable {
    protected double latitude;
    protected double longitude;
    protected POIType type;
    protected int id;
    protected int owner;

    public PointOfInterest( POIType type, int id, double latitude, double longitude, int owner ) {
        this.type = type;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.owner = owner;
    }

    public int getOwner( ) {
        return owner;
    }

    public double getLatitude( ) {
        return latitude;
    }

    public double getLongitude( ) {
        return longitude;
    }

    public POIType getType( ) {
        return type;
    }

    public int getId( ) {
        return id;
    }

    @Override
    public boolean equals( Object other ) {
        if ( other != null && other instanceof PointOfInterest ) {
            return this.id == ( ( PointOfInterest ) other ).id;
        }
        else {
            return false;
        }
    }
}
