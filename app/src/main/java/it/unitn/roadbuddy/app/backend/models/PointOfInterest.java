package it.unitn.roadbuddy.app.backend.models;


import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;

public abstract class PointOfInterest {
    private double latitude;
    private double longitude;
    private POIType type;
    private long id;

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

    public abstract MarkerOptions drawToMap( GoogleMap map );
}
