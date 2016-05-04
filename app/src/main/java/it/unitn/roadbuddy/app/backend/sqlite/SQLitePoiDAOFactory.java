package it.unitn.roadbuddy.app.backend.sqlite;

import it.unitn.roadbuddy.app.backend.PoiDAOFactory;


public class SQLitePoiDAOFactory extends PoiDAOFactory {
    private static SQLitePoiDAOFactory instance = null;

    private SQLitePoiDAOFactory( ) {

    }

    public static PoiDAOFactory getInstance( ) {
        if ( instance == null )
            instance = new SQLitePoiDAOFactory( );
        return instance;
    }

    public SQLiteCommentPoiDAO getCommentPoiDAO( ) {
        return SQLiteCommentPoiDAO.getInstance( );
    }
}
