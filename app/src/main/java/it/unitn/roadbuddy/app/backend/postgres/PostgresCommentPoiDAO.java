package it.unitn.roadbuddy.app.backend.postgres;


import android.content.Context;
import android.util.Log;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.CommentPoiDAO;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresCommentPoiDAO implements CommentPoiDAO {
    private static final int TABLE_VERSION = 3;  // TODO [ed] increment at every schema change
    private static final String
            TABLE_NAME = "CommentPOIs",
            COLUMN_NAME_ID = "id",
            COLUMN_NAME_LOCATION = "location",
            COLUMN_NAME_TEXT = "text";
    private static PostgresCommentPoiDAO instance = null;
    private Connection dbConnection;

    private PostgresCommentPoiDAO( ) throws SQLException {
        dbConnection = PostgresUtils.getConnection( );
        createTable( );
    }

    public static PostgresCommentPoiDAO getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresCommentPoiDAO( );
        return instance;
    }

    void createTable( ) throws SQLException {
        if ( PostgresUtils.getInstance( ).getSchemaVersion( TABLE_NAME ) < TABLE_VERSION ) {
            dbConnection.prepareStatement(
                    String.format( "DROP TABLE IF EXISTS %s", TABLE_NAME )
            ).execute( );

            dbConnection.prepareStatement(
                    String.format( "CREATE TABLE %s(%s SERIAL PRIMARY KEY, %s GEOMETRY(POINT), %s TEXT)",
                                   TABLE_NAME, COLUMN_NAME_ID, COLUMN_NAME_LOCATION, COLUMN_NAME_TEXT )
            ).execute( );

            PostgresUtils.getInstance( ).setSchemaVersion( TABLE_NAME, TABLE_VERSION );
        }
    }

    @Override
    public void AddCommentPOI( Context c, CommentPOI poi ) throws BackendException {
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                    String.format( "INSERT INTO %s(%s, %s) VALUES(?, ?)",
                                   TABLE_NAME, COLUMN_NAME_LOCATION, COLUMN_NAME_TEXT )
            );

            Point p = new Point( poi.getLatitude( ), poi.getLongitude( ) );
            stmt.setObject( 1, new PGgeometry( p ) );
            stmt.setString( 2, poi.getText( ) );

            stmt.execute( );
        }
        catch ( SQLException exc ) {
            Log.e( "roadbuddy", "exception while saving comment poi", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    public List<CommentPOI> getCommentPOIsInside( Context c, LatLngBounds bounds ) throws BackendException {
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                    String.format( "SELECT %1$s, %2$s, %3$s FROM %4$s WHERE ST_Contains(?, %2$s)",
                                   COLUMN_NAME_ID, COLUMN_NAME_LOCATION, COLUMN_NAME_TEXT, TABLE_NAME )
            );

            Polygon bounding_box = new Polygon( new LinearRing[] {
                    new LinearRing( new Point[] {
                            new Point( bounds.northeast.latitude, bounds.northeast.longitude ),
                            new Point( bounds.northeast.latitude, bounds.southwest.longitude ),
                            new Point( bounds.southwest.latitude, bounds.southwest.longitude ),
                            new Point( bounds.southwest.latitude, bounds.northeast.longitude ),
                            new Point( bounds.northeast.latitude, bounds.northeast.longitude )
                    } )
            } );

            stmt.setObject( 1, new PGgeometry( bounding_box ) );
            ResultSet res = stmt.executeQuery( );
            ArrayList<CommentPOI> pois = new ArrayList<>( );

            while ( res.next( ) ) {
                long id = res.getLong( COLUMN_NAME_ID );
                PGgeometry geom = ( PGgeometry ) res.getObject( COLUMN_NAME_LOCATION );
                Point point = ( Point ) geom.getGeometry( );
                String text = res.getString( COLUMN_NAME_TEXT );
                CommentPOI poi = new CommentPOI( id, point.x, point.y, text );
                pois.add( poi );
            }

            return pois;
        }
        catch ( SQLException exc ) {
            Log.e( "roadbuddy", "exception while retrieving comment pois", exc );
            throw new BackendException( "exception while retrieving comment pois", exc );
        }
    }
}
