package it.unitn.roadbuddy.app.backend.postgres;


import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.BuildConfig;
import it.unitn.roadbuddy.app.Utils;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.UserDAO;
import it.unitn.roadbuddy.app.backend.models.User;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class PostgresUserDAO extends PostgresDAOBase implements UserDAO {

    public static final String
            COLUMN_NAME_ID = "id",
            COLUMN_NAME_USERNAME = "username",
            COLUMN_NAME_LAST_POSITION = "lastPosition",
            COLUMN_NAME_LAST_POSITION_UPDATED = "lastPositionUpdated",
            TABLE_NAME = "Users";

    private static PostgresUserDAO instance;

    protected PostgresUserDAO( ) throws SQLException {
        super( );
    }

    public static synchronized PostgresUserDAO getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresUserDAO( );
        return instance;
    }

    @Override
    public User createUser( User newUserData ) throws BackendException {
        if ( !BuildConfig.DEBUG )
            throw new RuntimeException( "cannot change user in production settings" );

        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmtInsertUser = conn.prepareStatement(
                    String.format(
                            "INSERT INTO %s(%s, %s, %s) VALUES (?, ?, ?) RETURNING %s",
                            getSchemaName( ), COLUMN_NAME_USERNAME,
                            COLUMN_NAME_LAST_POSITION,
                            COLUMN_NAME_LAST_POSITION_UPDATED,
                            COLUMN_NAME_ID
                    )

            );
            stmtInsertUser.setString( 1, newUserData.getUserName( ) );

            LatLng lastPos = newUserData.getLastPosition( );
            if ( lastPos != null ) {
                Point p = new Point( lastPos.latitude, lastPos.longitude );
                stmtInsertUser.setObject( 2, new PGgeometry( p ) );
            }
            else stmtInsertUser.setObject( 2, null );

            java.util.Date lastPosUpdated = newUserData.getLastPositionUpdated( );
            if ( lastPosUpdated != null ) {
                stmtInsertUser.setTimestamp( 3, new Timestamp( lastPosUpdated.getTime( ) ),
                                             Calendar.getInstance( ) );
            }
            else stmtInsertUser.setObject( 3, null );

            ResultSet res = stmtInsertUser.executeQuery( );
            Utils.Assert( res.next( ), true );

            long newUserID = res.getLong( COLUMN_NAME_ID );
            return new User( newUserID, newUserData.getUserName( ),
                             newUserData.getLastPosition( ),
                             newUserData.getLastPositionUpdated( ) );
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    public User getUser( long id ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "SELECT %s, %s, %s FROM %s WHERE %s = ?",
                            COLUMN_NAME_USERNAME, COLUMN_NAME_LAST_POSITION,
                            COLUMN_NAME_LAST_POSITION_UPDATED, getSchemaName( ), COLUMN_NAME_ID
                    )
            );
            stmt.setLong( 1, id );
            ResultSet res = stmt.executeQuery( );
            if ( res.next( ) ) {
                return new User( id, res.getString( COLUMN_NAME_USERNAME ),
                                 readPosition( res ), readDate( res ) );
            }
            else return null;
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    public void setCurrentLocation( long id, LatLng location ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?",
                            TABLE_NAME, COLUMN_NAME_LAST_POSITION,
                            COLUMN_NAME_LAST_POSITION_UPDATED, COLUMN_NAME_ID
                    )
            );

            if ( location != null ) {
                Point p = new Point( location.latitude, location.longitude );
                stmt.setObject( 1, new PGgeometry( p ) );
            }
            else stmt.setObject( 2, null );

            stmt.setTimestamp(
                    2, new Timestamp( Calendar.getInstance( ).getTimeInMillis( ) ),
                    Calendar.getInstance( )
            );

            stmt.setLong( 3, id );

            stmt.execute( );
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    public List<User> getUsersInside( LatLngBounds bounds ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "SELECT %1$s, %2$s, %3$s, %4$s FROM %5$s WHERE ST_Contains(?, %3$s)",
                            COLUMN_NAME_ID, COLUMN_NAME_USERNAME, COLUMN_NAME_LAST_POSITION,
                            COLUMN_NAME_LAST_POSITION_UPDATED, TABLE_NAME
                    )
            );

            Polygon bounding_box = PostgresUtils.LatLngBoundsToPolygon( bounds );
            stmt.setObject( 1, new PGgeometry( bounding_box ) );

            ResultSet res = stmt.executeQuery( );
            List<User> users = new ArrayList<>( );

            while ( res.next( ) ) {
                User u = new User( res.getLong( COLUMN_NAME_ID ),
                                   res.getString( COLUMN_NAME_USERNAME ),
                                   readPosition( res ), readDate( res ) );
                users.add( u );
            }

            return users;
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "exception while retrieving comment pois", exc );
            throw new BackendException( "exception while retrieving comment pois", exc );
        }
    }

    LatLng readPosition( ResultSet res ) throws SQLException {
        PGgeometry geom = ( PGgeometry ) res.getObject( COLUMN_NAME_LAST_POSITION );
        if ( geom != null ) {
            Point point = ( Point ) geom.getGeometry( );
            return new LatLng( point.getX( ), point.getY( ) );
        }
        else return null;
    }

    Date readDate( ResultSet res ) throws SQLException {
        Timestamp ts = res.getTimestamp(
                COLUMN_NAME_LAST_POSITION_UPDATED,
                Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) )
        );

        if ( ts != null )
            return new Date( ts.getTime( ) );
        else return null;
    }

    @Override
    protected int getSchemaVersion( ) {
        return 2;  // TODO [ed] increment at every schema change
    }

    @Override
    public String getSchemaName( ) {
        return TABLE_NAME;
    }

    @Override
    protected String getCreateTableStatement( ) {
        return String.format(
                "CREATE TABLE %s(%s SERIAL PRIMARY KEY, " +
                        "%s VARCHAR(100), " +
                        "%s GEOMETRY(POINT), " +
                        "%s TIMESTAMPTZ)",
                getSchemaName( ), COLUMN_NAME_ID, COLUMN_NAME_USERNAME,
                COLUMN_NAME_LAST_POSITION, COLUMN_NAME_LAST_POSITION_UPDATED
        );
    }
}
