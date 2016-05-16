package it.unitn.roadbuddy.app.backend.postgres;

import com.google.android.gms.maps.model.LatLngBounds;
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgresql.PGConnection;

import java.sql.*;

import it.unitn.roadbuddy.app.BuildConfig;

public class PostgresUtils {

    private static final String USER = BuildConfig.USER;
    private static final String PASSWORD = BuildConfig.PASSWORD;
    private static final String HOST = BuildConfig.HOST;
    private static final String PORT = BuildConfig.PORT;
    private static final String DATABASE = BuildConfig.DATABASE;
    public static final String URL = String.format(
            "jdbc:postgresql://%s:%s/%s?user=%s&password=%s&ssl=false",
            HOST, PORT, DATABASE, USER, PASSWORD
    );
    private static PostgresUtils instance = null;
    private Connection conn;

    private PostgresUtils( ) throws SQLException {
        conn = getConnection( );
        conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS SchemaVersions(schema TEXT PRIMARY KEY, version INTEGER)"
        ).execute( );
    }

    public static Connection getConnection( ) throws SQLException {
        PGConnection conn = ( PGConnection ) DriverManager.getConnection( URL );
        conn.addDataType( "geometry", PGgeometry.class );
        return ( Connection ) conn;
    }

    public static PostgresUtils getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresUtils( );
        return instance;
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

    public int getSchemaVersion( String schema ) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement( "SELECT version FROM SchemaVersions WHERE schema = ?" );
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
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO SchemaVersions(schema, version) VALUES(?, ?) ON CONFLICT(schema) DO UPDATE SET version = ?"
        );
        stmt.setString( 1, schema );
        stmt.setInt( 2, version );
        stmt.setInt( 3, version );
        return stmt.execute( );
    }
}

