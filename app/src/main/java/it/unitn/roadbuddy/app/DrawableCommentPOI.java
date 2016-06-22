package it.unitn.roadbuddy.app;

import android.content.Context;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;

public class DrawableCommentPOI implements Drawable {
    protected transient Marker marker;
    protected CommentPOI poi;

    public DrawableCommentPOI( CommentPOI poi ) {
        this.poi = poi;
    }

    public String DrawToMap( Context context, GoogleMap map ) {
        MarkerOptions opts = new MarkerOptions( )
                .position( new LatLng( poi.getLatitude( ), poi.getLongitude( ) ) );

        marker = map.addMarker( opts );
        return marker.getId( );
    }

    public CommentPOI getPOI( ) {
        return poi;
    }

    @Override
    public int getModelId( ) {
        return poi.getId( );
    }

    @Override
    public String getMapId( ) {
        if ( marker != null )
            return marker.getId( );
        else return null;
    }

    @Override
    public void RemoveFromMap( Context context ) {
        if ( marker != null ) {
            marker.remove( );
            marker = null;
        }
    }

    @Override
    public void setSelected( Context context, GoogleMap map, boolean selected ) {
        if ( marker == null )
            return;

        if ( selected )
            marker.showInfoWindow( );
        else
            marker.hideInfoWindow( );
    }


    @Override
    public boolean equals( Drawable other ) {
        return other instanceof DrawableCommentPOI &&
                this.poi.equals( ( ( DrawableCommentPOI ) other ).poi );
    }

    @Override
    public SliderContentFragment getInfoFragment( ) {
        return DrawableCommentPOIInfoFragment.newInstance( this );
    }
}
