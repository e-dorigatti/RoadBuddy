package it.unitn.roadbuddy.app.backend;


import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.models.Path;

import java.util.List;

public interface PathDAO {
    void AddPath( Path p ) throws BackendException;

    List<Path> getPathsInside( Context c, LatLngBounds bounds ) throws BackendException;
    List<Path> getPathsFromPosition(Context c, LatLng pos ) throws BackendException;
}
