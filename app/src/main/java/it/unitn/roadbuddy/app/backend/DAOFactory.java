package it.unitn.roadbuddy.app.backend;

import it.unitn.roadbuddy.app.backend.postgres.PostgresPoiDAOFactory;
import it.unitn.roadbuddy.app.backend.sqlite.SQLitePoiDAOFactory;

public class DAOFactory {
    private static final DAOSource source = DAOSource.POSTGRESQL;
    private static boolean initialized = false;

    public static PoiDAOFactory getPoiDAOFactory( ) {
        if ( source == DAOSource.SQLITE )
            return SQLitePoiDAOFactory.getInstance( );
        else return PostgresPoiDAOFactory.getInstance( );
    }

    public enum DAOSource {
        SQLITE,
        POSTGRESQL
    }
}
