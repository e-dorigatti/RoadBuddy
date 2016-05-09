package it.unitn.roadbuddy.app.backend.models;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class CommentPOI extends PointOfInterest {
    private String text;

    public CommentPOI( long id, double latitude, double longitude, String text ) {
        super( POIType.COMMENT, id, latitude, longitude );
        this.text = text;
    }

    public String getText( ) {
        return text;
    }

    public Marker drawToMap( GoogleMap map ) {
        MarkerOptions opts = new MarkerOptions( )
                .position( new LatLng( getLatitude( ), getLongitude( ) ) )
                .title( "Created by an user" )  // FIXME [ed] when we add support for multiple users
                .snippet( getText( ) );

        marker = map.addMarker( opts );
        return marker;
    }
}
