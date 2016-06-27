package it.unitn.roadbuddy.app.backend;


import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

import it.unitn.roadbuddy.app.backend.models.Path;

public interface PathDAO {
    void AddPath( Path p ) throws BackendException;

    List<Path> getPathsInside( Context c, LatLngBounds bounds ) throws BackendException;

    List<Path> getPathsFromPosition( Context c, LatLng pos, long distanceMeters ) throws BackendException;

    Path getPath( int pathId ) throws BackendException;
}
