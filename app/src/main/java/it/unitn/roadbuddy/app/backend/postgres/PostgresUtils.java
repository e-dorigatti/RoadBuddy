package it.unitn.roadbuddy.app.backend.postgres;

import android.util.Log;
import com.google.android.gms.maps.model.LatLngBounds;
import com.zaxxer.hikari.HikariDataSource;
import it.unitn.roadbuddy.app.BuildConfig;
import it.unitn.roadbuddy.app.backend.BackendException;
import org.postgis.LinearRing;
import org.postgis.Point;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresUtils {

    private static final String USER = BuildConfig.USER;
    private static final String PASSWORD = BuildConfig.PASSWORD;
    private static final String HOST = BuildConfig.HOST;
    private static final String PORT = BuildConfig.PORT;
    private static final String DATABASE = BuildConfig.DATABASE;
    public static final String URL = String.format(
            "jdbc:postgresql://%s:%s/%s?user=%s&password=%s&ssl=false&loglevel=2",
            HOST, PORT, DATABASE, USER, PASSWORD
    );

    private static PostgresUtils instance = null;

    private HikariDataSource dataSource;

    private PostgresUtils( ) throws SQLException {

        dataSource = new HikariDataSource( );
        dataSource.setJdbcUrl( URL );
        dataSource.setUsername( USER );
        dataSource.setPassword( PASSWORD );

        getConnection( ).prepareStatement(
                "CREATE TABLE IF NOT EXISTS SchemaVersions(schema TEXT PRIMARY KEY, version INTEGER)"
        ).execute( );
    }

    public static void InitSchemas( ) throws BackendException {
        try {
            PostgresUserDAO.getInstance( );
            PostgresPathDAO.getInstance( );
            PostgresCommentPoiDAO.getInstance( );
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    public static PostgresUtils getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresUtils( );
        return instance;
    }

    public static boolean Init( ) {
        try {
            Class.forName( "org.postgresql.Driver" );

            PostgresUtils.getInstance( ).getConnection( ).close( );

            return true;
        }
        catch ( ClassNotFoundException e ) {
            Log.e( PostgresUtils.class.getName( ), "on init", e );
            return false;
        }
        catch ( SQLException e ) {
            Log.e( PostgresUtils.class.getName( ), "on init", e );
            return false;
        }
    }

    public static org.postgis.Polygon LatLngBoundsToPolygon( LatLngBounds bounds ) {
        return new org.postgis.Polygon( new LinearRing[] {
                new LinearRing( new Point[] {
                        new Point( bounds.northeast.latitude, bounds.northeast.longitude ),
                        new Point( bounds.northeast.latitude, bounds.southwest.longitude ),
                        new Point( bounds.southwest.latitude, bounds.southwest.longitude ),
                        new Point( bounds.southwest.latitude, bounds.northeast.longitude ),
                        new Point( bounds.northeast.latitude, bounds.northeast.longitude )
                } )
        } );
    }

    public void close( ) {
        try {
            dataSource.close( );
            instance = null;
        }
        catch ( Exception exc ) {
            Log.e( getClass( ).getName( ), "while closing pool", exc );
        }
    }

    public Connection getConnection( ) throws SQLException {
        return dataSource.getConnection( );
    }

    public int getSchemaVersion( String schema ) throws SQLException {
        PreparedStatement stmt = getConnection( ).prepareStatement(
                "SELECT version FROM SchemaVersions WHERE schema = ?"
        );
        stmt.setString( 1, schema );
        ResultSet res = stmt.executeQuery( );
        if ( res.next( ) ) {
            return res.getInt( 1 );
        }
        else {
            return -1;
        }
    }

    public boolean setSchemaVersion( String schema, int version ) throws SQLException {
        PreparedStatement stmt = getConnection( ).prepareStatement(
                "INSERT INTO SchemaVersions(schema, version) VALUES(?, ?) ON CONFLICT(schema) DO UPDATE SET version = ?"
        );
        stmt.setString( 1, schema );
        stmt.setInt( 2, version );
        stmt.setInt( 3, version );
        return stmt.execute( );
    }
}

