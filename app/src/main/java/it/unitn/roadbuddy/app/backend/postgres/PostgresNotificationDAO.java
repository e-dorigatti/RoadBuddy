package it.unitn.roadbuddy.app.backend.postgres;


import android.util.Log;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.NotificationDAO;
import it.unitn.roadbuddy.app.backend.models.Notification;
import it.unitn.roadbuddy.app.backend.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresNotificationDAO extends PostgresDAOBase implements NotificationDAO {

    public static final String
            TABLE_NAME = "notifications",
            COLUMN_NAME_ID = "id",
            COLUMN_NAME_USER = "userId",
            COLUMN_NAME_TRIP = "tripId ",
            COLUMN_NAME_TYPE = "type";

    private static PostgresNotificationDAO instance = null;

    private PostgresNotificationDAO( ) throws SQLException {
        super( );
    }

    public static synchronized PostgresNotificationDAO getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresNotificationDAO( );
        return instance;
    }

    @Override
    public List<Notification> getPings( int tripId ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format( "SELECT %s, %s, %s FROM %s WHERE %s = ?",
                                   COLUMN_NAME_ID, COLUMN_NAME_USER,
                                   COLUMN_NAME_TYPE, TABLE_NAME,
                                   COLUMN_NAME_TRIP
                    ) );

            stmt.setInt( 1, tripId );

            ResultSet res = stmt.executeQuery( );

            List<Notification> notifications = new ArrayList<>( );
            while ( res.next( ) ) {
                int id = res.getInt( 1 );
                int userId = res.getInt( 2 );
                int type = res.getInt( 3 );

                /**
                 * As usual, we could leverage the fact that users are stored in
                 * the same postgres db to perform a join and so on, but this
                 * method will be called from a background service so we can
                 * afford a couple more seconds of delay
                 */
                User sender = DAOFactory.getUserDAO( ).getUser( userId );

                notifications.add( new Notification( id, sender, type ) );
            }

            return notifications;
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "while sending ping", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    public void sendPing( int userId, int tripId, int type ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format( "INSERT INTO %s(%s, %s, %s) VALUES (?, ?, ?)",
                                   TABLE_NAME, COLUMN_NAME_USER,
                                   COLUMN_NAME_TRIP, COLUMN_NAME_TYPE )
            );

            stmt.setInt( 1, userId );
            stmt.setInt( 2, tripId );
            stmt.setInt( 3, type );

            stmt.execute( );
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "while sending ping", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    protected int getSchemaVersion( ) {
        return 0;  // TODO [ed] increment at every schema change
    }

    @Override
    protected String getSchemaName( ) {
        return TABLE_NAME;
    }

    @Override
    protected String getCreateTableStatement( ) {
        String cane = String.format(
                "CREATE TABLE %s(%s SERIAL PRIMARY KEY, %s INTEGER, " +
                        "%s INTEGER, %s INTEGER)",
                TABLE_NAME, COLUMN_NAME_ID, COLUMN_NAME_USER,
                COLUMN_NAME_TRIP, COLUMN_NAME_TYPE
        );

        return cane;
    }
}
