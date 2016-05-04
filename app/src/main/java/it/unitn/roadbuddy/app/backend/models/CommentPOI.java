package it.unitn.roadbuddy.app.backend.models;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
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

    public MarkerOptions drawToMap( GoogleMap map ) {
        MarkerOptions opts = new MarkerOptions( )
                .position( new LatLng( getLatitude( ), getLongitude( ) ) )
                .title( "Created by an user" )
                .snippet( getText( ) );

        return opts;
    }
}
