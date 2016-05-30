package it.unitn.roadbuddy.app;

import android.content.Context;
import android.graphics.Color;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import it.unitn.roadbuddy.app.backend.models.Path;

public class DrawablePath implements Drawable {
    protected Path path;
    protected Polyline polyline;

    public DrawablePath( Path path ) {
        this.path = path;
    }

    public Path getPath( ) {
        return path;
    }

    @Override
    public String DrawToMap( Context context, GoogleMap map ) {
        PolylineOptions opts = new PolylineOptions( )
                .clickable( true );

        for ( Path.Leg leg : path.getLegs( ) )
            opts.addAll( leg.getPoints( ) );

        polyline = map.addPolyline( opts );
        setSelected( context, map, false );

        return polyline.getId( );
    }

    @Override
    public void RemoveFromMap( Context context ) {
        if ( polyline != null ) {
            polyline.remove( );
            polyline = null;
        }
    }

    @Override
    public void setSelected( Context context, GoogleMap map, boolean selected ) {
        if ( polyline == null )
            return;

        if ( selected ) {
            polyline.setColor( Color.RED );
        }
        else {
            polyline.setColor( Color.BLUE );
        }
    }

    @Override
    public boolean equals( Drawable other ) {
        return other != null && other instanceof DrawablePath &&
                this.path.equals( ( ( DrawablePath ) other ).path );
    }

    @Override
    public DrawableInfoFragment getInfoFragment( ) {
        return DrawablePathInfoFragment.newInstance( this );
    }
}
