package it.unitn.roadbuddy.app.backend.postgres;


import android.content.Context;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.PathDAO;
import it.unitn.roadbuddy.app.backend.models.Path;
import org.postgis.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresPathDAO extends PostgresDAOBase implements PathDAO {
    protected static final String
            COLUMN_NAME_ID = "id",
            COLUMN_NAME_PATH = "path",
            COLUMN_NAME_OWNER = "owner",
            TABLE_NAME = "Paths";

    private static PostgresPathDAO instance;

    private PostgresPathDAO( ) throws SQLException {
        super( );
    }

    public static PostgresPathDAO getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresPathDAO( );
        return instance;
    }

    static Path appendMultiLineStringToPath( Path path, MultiLineString mls ) {
        for ( LineString ls : mls.getLines( ) ) {
            List<LatLng> leg = new ArrayList<>( );
            for ( Point p : ls.getPoints( ) ) {
                leg.add( new LatLng( p.getX( ), p.getY( ) ) );
            }
            path.addLeg( leg );
        }

        return path;
    }

    static MultiLineString pathToMultiLineString( Path path ) {
        List<Path.Leg> legs = path.getLegs( );
        LineString[] strings = new LineString[ legs.size( ) ];

        for ( int i = 0; i < legs.size( ); i++ ) {
            List<LatLng> legPoints = legs.get( i ).getPoints( );
            Point[] lineStringPoints = new Point[ legPoints.size( ) ];

            for ( int j = 0; j < legPoints.size( ); j++ ) {
                LatLng point = legPoints.get( j );
                lineStringPoints[ j ] = new Point( point.latitude, point.longitude );
            }

            LineString ls = new LineString(
                    lineStringPoints
            );

            strings[ i ] = ls;
        }

        return new MultiLineString( strings );
    }

    @Override
    public void AddPath( Path path ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "INSERT INTO %s(%s, %s) VALUES (?, ?)",
                            getSchemaName( ), COLUMN_NAME_PATH, COLUMN_NAME_OWNER
                    )
            );

            MultiLineString mls = pathToMultiLineString( path );
            stmt.setObject( 1, new PGgeometry( mls ) );
            stmt.setLong( 2, path.getOwner( ) );

            stmt.execute( );
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "while saving path", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    public List<Path> getPathsInside( Context c, LatLngBounds bounds ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "SELECT %1$s, %2$s, %4$s FROM %3$s WHERE ST_Intersects(?, %2$s)",
                            COLUMN_NAME_ID, COLUMN_NAME_PATH, getSchemaName( ), COLUMN_NAME_OWNER
                    )
            );

            Polygon bounding_box = PostgresUtils.LatLngBoundsToPolygon( bounds );
            stmt.setObject( 1, new PGgeometry( bounding_box ) );

            ResultSet res = stmt.executeQuery( );
            List<Path> paths = new ArrayList<>( );

            while ( res.next( ) ) {
                long id = res.getLong( COLUMN_NAME_ID );
                long owner = res.getLong( COLUMN_NAME_OWNER );

                PGgeometry geom = ( PGgeometry ) res.getObject( COLUMN_NAME_PATH );
                MultiLineString mls = ( MultiLineString ) geom.getGeometry( );

                Path path = new Path( id, owner );
                appendMultiLineStringToPath( path, mls );
                paths.add( path );
            }

            return paths;
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "while retrieving paths", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    protected int getSchemaVersion( ) {
        return 4;  // TODO [ed] increment at every schema change
    }

    @Override
    protected String getSchemaName( ) {
        return TABLE_NAME;
    }

    @Override
    protected String getCreateTableStatement( ) {
        return String.format(
                "CREATE TABLE %s(%s SERIAL PRIMARY KEY, " +
                        "%s GEOMETRY(MULTILINESTRING), " +
                        "%s INTEGER REFERENCES %s(%s) NOT NULL)",
                getSchemaName( ), COLUMN_NAME_ID, COLUMN_NAME_PATH,
                COLUMN_NAME_OWNER, PostgresUserDAO.TABLE_NAME,
                PostgresUserDAO.COLUMN_NAME_ID
        );
    }
}
