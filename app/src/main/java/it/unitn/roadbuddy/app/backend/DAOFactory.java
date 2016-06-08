package it.unitn.roadbuddy.app.backend;

import it.unitn.roadbuddy.app.backend.postgres.PostgresPathDAO;
import it.unitn.roadbuddy.app.backend.postgres.PostgresPoiDAOFactory;
import it.unitn.roadbuddy.app.backend.postgres.PostgresTripDAO;
import it.unitn.roadbuddy.app.backend.postgres.PostgresUserDAO;

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

}
