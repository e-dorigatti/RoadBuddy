package it.unitn.roadbuddy.app.backend.models;


import com.google.android.gms.maps.model.Marker;

public abstract class PointOfInterest {
    protected double latitude;
    protected double longitude;
    protected POIType type;
    protected long id;
    protected Marker marker;

    public PointOfInterest( POIType type, long id, double latitude, double longitude ) {
        this.type = type;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public long getId( ) {
        return id;
    }

    public Marker getMarker( ) {
        return marker;
    }

    public void setMarker( Marker marker ) {
        this.marker = marker;
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
