package it.unitn.roadbuddy.app.backend.models;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.internal.IPolylineDelegate;

public class CommentPOI extends PointOfInterest {
    private String text;

    public CommentPOI( int id, double latitude, double longitude, String text, int owner ) {
        super( POIType.COMMENT, id, latitude, longitude, owner );
        this.text = text;
    }

    public String getText( ) {
        return text;
    }
}
