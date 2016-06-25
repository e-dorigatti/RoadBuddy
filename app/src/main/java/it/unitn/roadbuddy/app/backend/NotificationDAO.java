package it.unitn.roadbuddy.app.backend;


import it.unitn.roadbuddy.app.backend.models.Notification;
import it.unitn.roadbuddy.app.backend.models.User;

import java.util.List;

public interface NotificationDAO {

    void sendPing( int userId, int tripId, int type ) throws BackendException;

    List<Notification> getPings( int tripId ) throws BackendException;

}
