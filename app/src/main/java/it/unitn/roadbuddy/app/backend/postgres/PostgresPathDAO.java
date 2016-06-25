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
            COLUMN_NAME_DISTANCE = "distance",
            COLUMN_NAME_DURATION = "duration",
            COLUMN_NAME_DESCRIPTION = "description",
            TABLE_NAME = "Paths";

    private static PostgresPathDAO instance;

    private PostgresPathDAO( ) throws SQLException {
        super( );
    }

    public static synchronized PostgresPathDAO getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresPathDAO( );
        return instance;
    }

    static List<LatLng> lineStringToList( LineString ls ) {
        List<LatLng> leg = new ArrayList<>( );
        for ( Point p : ls.getPoints( ) ) {
            leg.add( new LatLng( p.getX( ), p.getY( ) ) );
        }
        return leg;
    }

    static List<List<LatLng>> multiLineStringToLegs( MultiLineString mls ) {
        List<List<LatLng>> legs = new ArrayList<>( );
        for ( LineString ls : mls.getLines( ) ) {
            List<LatLng> leg = lineStringToList( ls );
            legs.add( leg );
        }

        return legs;
    }

    static MultiLineString legsToMultiLineString( List<List<LatLng>> legs ) {
        LineString[] strings = new LineString[ legs.size( ) ];

        for ( int i = 0; i < legs.size( ); i++ ) {
            List<LatLng> legPoints = legs.get( i );
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

    public static Path readPath( ResultSet res, String tableAlias ) throws SQLException {
        int id = res.getInt( tableAlias + COLUMN_NAME_ID );
        int owner = res.getInt( tableAlias + COLUMN_NAME_OWNER );
        long distance = res.getLong( tableAlias + COLUMN_NAME_DISTANCE );
        long duration = res.getLong( tableAlias + COLUMN_NAME_DURATION );
        String description = res.getString( tableAlias + COLUMN_NAME_DESCRIPTION );

        Path path = new Path( id, owner, distance, duration, description );

        PGgeometry geom = ( PGgeometry ) res.getObject( tableAlias + COLUMN_NAME_PATH );
        switch ( geom.getGeoType( ) ) {

            case Geometry.MULTILINESTRING: {
                MultiLineString mls = ( MultiLineString ) geom.getGeometry( );
                path.setLegs( multiLineStringToLegs( mls ) );
                break;
            }

            case Geometry.LINESTRING: {
                LineString ls = ( LineString ) geom.getGeometry( );
                List<List<LatLng>> legs = new ArrayList<>( );
                legs.add( lineStringToList( ls ) );
                path.setLegs( legs );
                break;
            }

            default:
                throw new RuntimeException( "DEBUG" );
        }

        return path;
    }

    @Override
    public void AddPath( Path path ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "INSERT INTO %s(%s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?)",
                            getSchemaName( ), COLUMN_NAME_PATH, COLUMN_NAME_OWNER,
                            COLUMN_NAME_DISTANCE, COLUMN_NAME_DURATION,
                            COLUMN_NAME_DESCRIPTION
                    )
            );

            MultiLineString mls = legsToMultiLineString( path.getLegs( ) );
            stmt.setObject( 1, new PGgeometry( mls ) );
            stmt.setInt( 2, path.getOwner( ) );
            stmt.setLong( 3, path.getDistance( ) );
            stmt.setLong( 4, path.getDuration( ) );
            stmt.setString( 5, path.getDescription( ) );

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
            double tolerance = 0.0025 * Math.max(
                    Math.abs( bounds.northeast.latitude - bounds.southwest.latitude ),
                    Math.abs( bounds.northeast.longitude - bounds.southwest.longitude )
            );

            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "SELECT %1$s, ST_SimplifyPreserveTopology(%2$s, %8$s) AS %2$s, %4$s, %5$s, %6$s, %7$s " +
                                    "FROM %3$s WHERE ST_Intersects(?, %2$s)",
                            COLUMN_NAME_ID, COLUMN_NAME_PATH, getSchemaName( ), COLUMN_NAME_OWNER,
                            COLUMN_NAME_DISTANCE, COLUMN_NAME_DURATION, COLUMN_NAME_DESCRIPTION,
                            tolerance
                    )
            );

            Polygon bounding_box = PostgresUtils.LatLngBoundsToPolygon( bounds );
            stmt.setObject( 1, new PGgeometry( bounding_box ) );

            ResultSet res = stmt.executeQuery( );
            List<Path> paths = new ArrayList<>( );

            while ( res.next( ) ) {
                Path path = readPath( res, "" );
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
    public List<Path> getPathsFromPosition( Context c, LatLng pos, long distanceMeters ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "SELECT %1$s, ST_SimplifyPreserveTopology(%2$s, 0.01) AS %2$s, %4$s, " +
                                    "%5$s, %6$s, %7$s FROM %3$s WHERE ST_DWithin(%2$s, ?, ?)",
                            COLUMN_NAME_ID, COLUMN_NAME_PATH, getSchemaName( ), COLUMN_NAME_OWNER,
                            COLUMN_NAME_DISTANCE, COLUMN_NAME_DURATION, COLUMN_NAME_DESCRIPTION
                    )
            );

            double earth_radius = 6371 * 1000; // metres
            double distanceDegrees = 180.0 * distanceMeters / ( Math.PI * earth_radius );

            Point p = new Point( pos.latitude, pos.longitude );
            stmt.setObject( 1, new PGgeometry( p ) );
            stmt.setDouble( 2, distanceDegrees );

            ResultSet res = stmt.executeQuery( );
            List<Path> paths = new ArrayList<>( );

            while ( res.next( ) ) {
                Path path = readPath( res, "" );
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
    public Path getPath( int pathId ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "SELECT %s, %s, %s, %s, %s, %s FROM %s WHERE %s = ?",
                            COLUMN_NAME_ID, COLUMN_NAME_PATH, COLUMN_NAME_OWNER,
                            COLUMN_NAME_DISTANCE, COLUMN_NAME_DURATION,
                            COLUMN_NAME_DESCRIPTION, TABLE_NAME, COLUMN_NAME_ID
                    )
            );
            stmt.setInt( 1, pathId );

            ResultSet res = stmt.executeQuery( );
            if ( res.next( ) ) {
                return readPath( res, "" );
            }
            else return null;
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "while retrieving paths", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    protected int getSchemaVersion( ) {
        return 9;  // TODO [ed] increment at every schema change
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
                        "%s INTEGER NOT NULL, " +
                        "%s INTEGER NOT NULL, " +
                        "%s INTEGER NOT NULL, " +
                        "%s TEXT)",
                getSchemaName( ), COLUMN_NAME_ID, COLUMN_NAME_PATH,
                COLUMN_NAME_OWNER, COLUMN_NAME_DISTANCE,
                COLUMN_NAME_DURATION, COLUMN_NAME_DESCRIPTION
        );
    }
}