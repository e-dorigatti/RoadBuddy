package it.unitn.roadbuddy.app.backend;

import android.content.Context;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;

import java.util.List;

public interface CommentPoiDAO {
    void AddCommentPOI( Context c, CommentPOI p ) throws BackendException;

    List<CommentPOI> getCommentPOIsInside( Context c, LatLngBounds bounds ) throws BackendException;
}
