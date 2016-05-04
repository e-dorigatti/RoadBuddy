package it.unitn.roadbuddy.app.backend;

import it.unitn.roadbuddy.app.backend.sqlite.SQLitePoiDAOFactory;

public class DAOFactory {
    private static final DAOSource source = DAOSource.SQLITE;

    public static PoiDAOFactory getPoiDAOFactory( ) {
        //if ( source == DAOSource.SQLITE )
        return SQLitePoiDAOFactory.getInstance( );
        //else return null;  // stupid java
    }
}
