package it.unitn.roadbuddy.app.backend;

import it.unitn.roadbuddy.app.backend.postgres.*;

import java.sql.SQLException;

public class DAOFactory {

    public static PoiDAOFactory getPoiDAOFactory( ) {
        return PostgresPoiDAOFactory.getInstance( );
    }

    public static PathDAO getPathDAO( ) throws BackendException {
        try {
            return PostgresPathDAO.getInstance( );
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    public static UserDAO getUserDAO( ) throws BackendException {
        try {
            return PostgresUserDAO.getInstance( );
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    public static TripDAO getTripDAO( ) throws BackendException {
        try {
            return PostgresTripDAO.getInstance( );
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    public static InviteDAO getInviteDAO( ) throws BackendException {
        try {
            return PostgresInviteDAO.getInstance( );
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    public static NotificationDAO getNotificationDAO( ) throws BackendException {
        try {
            return PostgresNotificationDAO.getInstance( );
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }
}
