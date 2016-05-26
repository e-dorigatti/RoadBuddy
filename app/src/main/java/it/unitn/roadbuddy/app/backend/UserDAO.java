package it.unitn.roadbuddy.app.backend;

import it.unitn.roadbuddy.app.backend.models.User;

public interface UserDAO {
    User createUser( User newUserData ) throws BackendException;

    User getUser( long id ) throws BackendException;
}
