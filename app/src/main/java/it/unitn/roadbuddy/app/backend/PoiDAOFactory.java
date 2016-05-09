package it.unitn.roadbuddy.app.backend;


import android.content.Context;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.models.PointOfInterest;

import java.util.ArrayList;
import java.util.List;

public abstract class PoiDAOFactory {
    public abstract CommentPoiDAO getCommentPoiDAO( ) throws BackendException;

    public List<PointOfInterest> getPOIsInside( Context c, LatLngBounds bounds ) throws BackendException {
        List<PointOfInterest> points = new ArrayList<PointOfInterest>( );

        // TODO [ed] add points from all POI DAOs
        points.addAll( getCommentPoiDAO( ).getCommentPOIsInside( c, bounds ) );

        return points;
    }
}
