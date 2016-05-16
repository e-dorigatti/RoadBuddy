package it.unitn.roadbuddy.app.backend.postgres;


import android.content.Context;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.PathDAO;
import it.unitn.roadbuddy.app.backend.models.Path;
import org.postgis.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresPathDAO extends PostgresDAOBase implements PathDAO {
    private static final String
            COLUMN_NAME_ID = "id",
            COLUMN_NAME_PATH = "path";
    private static PostgresPathDAO instance;

    private PostgresPathDAO( ) throws SQLException {
        super( );
    }

    public static PostgresPathDAO getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresPathDAO( );
        return instance;
    }

    Path multiLineStringToPath( long id, MultiLineString mls ) {
        Path path = new Path( id );

        for ( LineString ls : mls.getLines( ) ) {
            List<LatLng> leg = new ArrayList<>( );
            for ( Point p : ls.getPoints( ) ) {
                leg.add( new LatLng( p.getX( ), p.getY( ) ) );
            }
            path.addLeg( leg );
        }

        return path;
    }

    MultiLineString pathToMultiLineString( Path path ) {
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
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                    String.format( "INSERT INTO %s(%s) VALUES (?)",
                                   getSchemaName( ), COLUMN_NAME_PATH )
            );

            MultiLineString mls = pathToMultiLineString( path );
            stmt.setObject( 1, new PGgeometry( mls ) );

            stmt.execute( );
        }
        catch ( SQLException exc ) {
            Log.e( "roadbuddy", "while saving path", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    public List<Path> getPathsInside( Context c, LatLngBounds bounds ) throws BackendException {
        try {
            PreparedStatement stmt = dbConnection.prepareStatement(
                    String.format( "SELECT %1$s, %2$s FROM %3$s WHERE ST_Intersects(?, %2$s)",
                                   COLUMN_NAME_ID, COLUMN_NAME_PATH, getSchemaName( ) )
            );

            Polygon bounding_box = PostgresUtils.LatLngBoundsToPolygon( bounds );
            stmt.setObject( 1, new PGgeometry( bounding_box ) );

            ResultSet res = stmt.executeQuery( );
            List<Path> paths = new ArrayList<>( );

            while ( res.next( ) ) {
                long id = res.getLong( COLUMN_NAME_ID );
                PGgeometry geom = ( PGgeometry ) res.getObject( COLUMN_NAME_PATH );
                MultiLineString mls = ( MultiLineString ) geom.getGeometry( );

                Path path = multiLineStringToPath( id, mls );
                paths.add( path );
            }

            return paths;
        }
        catch ( SQLException exc ) {
            Log.e( "roadbuddy", "while retrieving paths", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    protected int getSchemaVersion( ) {
        return 1;  // TODO [ed] increment at every schema change
    }

    @Override
    protected String getSchemaName( ) {
        return "Paths";
    }

    @Override
    protected String getCreateTableStatement( ) {
        return String.format( "CREATE TABLE %s(%s SERIAL PRIMARY KEY, %s GEOMETRY(MULTILINESTRING))",
                              getSchemaName( ), COLUMN_NAME_ID, COLUMN_NAME_PATH );
    }
}
