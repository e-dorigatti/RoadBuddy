package it.unitn.roadbuddy.app;


import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

public class SerializablePoint implements Serializable {
    double latitude;
    double longitude;

    public SerializablePoint( LatLng point ) {
        latitude = point.latitude;
        longitude = point.longitude;
    }

    public LatLng toLatLng( ) {
        return new LatLng( latitude, longitude );
    }
}
