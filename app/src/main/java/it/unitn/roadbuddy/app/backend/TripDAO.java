package it.unitn.roadbuddy.app.backend;


import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.Trip;
import it.unitn.roadbuddy.app.backend.models.User;

public interface TripDAO {

    Trip createTrip( Path path, User creator ) throws BackendException;

    Trip getTrip( int id ) throws BackendException;

}
