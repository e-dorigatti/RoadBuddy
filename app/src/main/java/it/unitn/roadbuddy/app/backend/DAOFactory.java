package it.unitn.roadbuddy.app.backend;

import it.unitn.roadbuddy.app.backend.postgres.PostgresPathDAO;
import it.unitn.roadbuddy.app.backend.postgres.PostgresPoiDAOFactory;
import it.unitn.roadbuddy.app.backend.postgres.PostgresUserDAO;

import java.sql.SQLException;

public class DAOFactory {
    private static final DAOSource source = DAOSource.POSTGRESQL;
    private static boolean initialized = false;

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

    public enum DAOSource {
        POSTGRESQL
    }
}
