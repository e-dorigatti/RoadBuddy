package it.unitn.roadbuddy.app.backend;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.models.User;

import java.util.List;

public interface UserDAO {
    User createUser( User newUserData ) throws BackendException;

    User getUser( int id ) throws BackendException;

    User getUser( String userName ) throws BackendException;

    void setCurrentLocation( int id, LatLng location ) throws BackendException;

    List<User> getUsersInside( LatLngBounds bounds ) throws BackendException;

    List<User> getUsersOfTrip( int trip ) throws BackendException;
}
