package it.unitn.roadbuddy.app;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.Fragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.User;

public class DrawableUser implements Drawable {

    protected User user;
    protected Marker marker;

    public DrawableUser( User user ) {
        this.user = user;
    }

    @Override
    public Fragment getInfoFragment( ) {
        return null;
    }

    @Override
    public boolean equals( Drawable other ) {
        if ( other != null && other instanceof DrawableUser )
            return getModelId( ) == other.getModelId( );
        else return false;
    }

    @Override
    public void RemoveFromMap( Context context ) {
        if ( marker != null )
            marker.remove( );
    }

    @Override
    public void setSelected( Context context, GoogleMap map, boolean selected ) {

    }

    @Override
    public String DrawToMap( Context context, GoogleMap map ) {
        if ( user.getLastPosition( ) == null )
            return null;

        float scaleFactor = 0.275f;
        Bitmap orig = BitmapFactory.decodeResource( context.getResources( ), R.mipmap.yellow_circle );
        Bitmap bmp = Bitmap.createScaledBitmap( orig, ( int ) ( orig.getWidth( ) * scaleFactor ),
                                                ( int ) ( orig.getHeight( ) * scaleFactor ), true );
        orig.recycle( );

        MarkerOptions opts = new MarkerOptions( )
                .icon( BitmapDescriptorFactory.fromBitmap( bmp ) )
                .position( user.getLastPosition( ) )
                .title( user.getUserName( ) );

        marker = map.addMarker( opts );

        return marker.getId( );
    }

    @Override
    public String getMapId( ) {
        return marker != null ? marker.getId( ) : null;
    }

    @Override
    public int getModelId( ) {
        return user.getId( );
    }

    public User getUser( ) {
        return user;
    }
}
