package it.unitn.roadbuddy.app.backend.postgres;


import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.PoiDAOFactory;

import java.sql.SQLException;

public class PostgresPoiDAOFactory extends PoiDAOFactory {
    private static PostgresPoiDAOFactory instance = null;

    private PostgresPoiDAOFactory( ) {

    }

    public static synchronized PoiDAOFactory getInstance( ) {
        if ( instance == null )
            instance = new PostgresPoiDAOFactory( );
        return instance;
    }

    public PostgresCommentPoiDAO getCommentPoiDAO( ) throws BackendException {
        try {
            return PostgresCommentPoiDAO.getInstance( );
        }
        catch ( SQLException e ) {
            throw new BackendException( e.getMessage( ), e );
        }
    }
}
