package it.unitn.roadbuddy.app;

import android.content.Context;
import android.graphics.Color;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import it.unitn.roadbuddy.app.backend.models.Path;

import java.util.ArrayList;
import java.util.List;

public class DrawablePath implements Drawable {
    protected Path path;
    protected Polyline polyline;

    protected List<Marker> waypoints;

    public DrawablePath( Path path ) {
        this.path = path;
    }

    public Path getPath( ) {
        return path;
    }

    Marker addMarker( GoogleMap map, LatLng point ) {
        MarkerOptions opts = new MarkerOptions( )
                .position( point );
        return map.addMarker( opts );
    }

    @Override
    public int getModelId( ) {
        return path.getId( );
    }

    @Override
    public String getMapId( ) {
        if ( polyline != null )
            return polyline.getId( );
        else return null;
    }

    @Override
    public String DrawToMap( Context context, GoogleMap map ) {
        PolylineOptions opts = new PolylineOptions( )
                .clickable( true );

        for ( List<LatLng> leg : path.getLegs( ) )
            opts.addAll( leg );

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

        if ( waypoints != null ) {
            for ( Marker m : waypoints )
                m.remove( );
            waypoints = null;
        }
    }

    @Override
    public void setSelected( Context context, GoogleMap map, boolean selected ) {
        if ( polyline == null )
            return;

        if ( selected ) {
            polyline.setColor( Color.RED );

            waypoints = new ArrayList<>( );
            List<List<LatLng>> legs = path.getLegs( );
            for ( int i = 0; i < legs.size( ); i++ ) {
                List<LatLng> leg = legs.get( i );

                if ( i == 0 ) {
                    waypoints.add( addMarker( map, leg.get( 0 ) ) );
                }
                waypoints.add( addMarker( map, leg.get( leg.size( ) - 1 ) ) );
            }
        }
        else {
            polyline.setColor( Color.BLUE );
            if ( waypoints != null ) {
                for ( Marker m : waypoints )
                    m.remove( );
                waypoints = null;
            }
        }
    }

    @Override
    public boolean equals( Drawable other ) {
        return other != null && other instanceof DrawablePath &&
                this.path.equals( ( ( DrawablePath ) other ).path );
    }

    @Override
    public SliderContentFragment getInfoFragment( ) {
        return DrawablePathInfoFragment.newInstance( this );
    }
}
