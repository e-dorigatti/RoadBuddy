package it.unitn.roadbuddy.app.backend.models;


import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class Path {

    protected List<Leg> legs = new ArrayList<>( );
    protected long owner;
    private long id;

    public Path( long id, long owner ) {
        this.id = id;
        this.owner = owner;
    }

    public long getId( ) {
        return id;
    }

    public long getOwner( ) {
        return owner;
    }

    public void addLeg( List<LatLng> points ) {
        Leg leg = new Leg( points );
        this.legs.add( leg );
    }

    public List<Leg> getLegs( ) {
        return new ArrayList<>( legs );
    }

    public Polyline drawToMap( GoogleMap map ) {
        PolylineOptions opts = new PolylineOptions( );
        for ( Leg leg : legs )
            opts.addAll( leg.getPoints( ) );
        return map.addPolyline( opts );
    }

    @Override
    public boolean equals( Object other ) {
        return other != null && other instanceof Path &&
                this.id == ( ( Path ) other ).id;
    }

    public class Leg {
        protected List<LatLng> points = new ArrayList<>( );

        public Leg( List<LatLng> points ) {
            this.points.addAll( points );
        }

        public List<LatLng> getPoints( ) {
            return new ArrayList<>( points );
        }
    }
}
