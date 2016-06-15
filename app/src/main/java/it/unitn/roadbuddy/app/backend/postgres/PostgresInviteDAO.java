package it.unitn.roadbuddy.app.backend.postgres;


import android.util.Log;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.InviteDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresInviteDAO extends PostgresDAOBase implements InviteDAO {

    public static final String
            COLUMN_NAME_INVITER = "inviter",
            COLUMN_NAME_INVITEE = "invitee",
            COLUMN_NAME_TRIP = "trip",
            TABLE_NAME = "Invites";

    private static PostgresInviteDAO instance;

    private PostgresInviteDAO( ) throws SQLException {
        super( );
    }

    public static synchronized PostgresInviteDAO getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresInviteDAO( );
        return instance;
    }

    @Override
    public boolean addInvite( int inviter, String invitee, int trip ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            if ( DAOFactory.getUserDAO( ) instanceof PostgresUserDAO )
                return addInviteWithinPostgres( conn, inviter, invitee, trip );
            else throw new RuntimeException( "not implemented yet" );
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "while adding an invite", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    protected boolean addInviteWithinPostgres( Connection conn, int inviter, String invitee, int trip )
            throws SQLException {

        PreparedStatement stmt = conn.prepareStatement(
                String.format(
                        "INSERT INTO %s(%s, %s, %s) SELECT ?, ?, %s FROM %s WHERE %s = ?",
                        TABLE_NAME, COLUMN_NAME_INVITER, COLUMN_NAME_TRIP, COLUMN_NAME_INVITEE,
                        PostgresUserDAO.COLUMN_NAME_ID, PostgresUserDAO.TABLE_NAME,
                        PostgresUserDAO.COLUMN_NAME_USERNAME
                )
        );

        stmt.setInt( 1, inviter );
        stmt.setInt( 2, trip );
        stmt.setString( 3, invitee );

        int updated = stmt.executeUpdate( );
        return updated == 1;
    }

    @Override
    public List<Integer> retrieveInvites( int user ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "SELECT %s FROM %s WHERE %s = ?",
                            TABLE_NAME, COLUMN_NAME_TRIP, COLUMN_NAME_INVITEE
                    )
            );

            stmt.setInt( 1, user );


            ResultSet res = stmt.executeQuery( );
            List<Integer> invites = new ArrayList<>( );
            while ( res.next( ) ) {
                int trip = res.getInt( COLUMN_NAME_TRIP );
                invites.add( trip );
            }

            return invites;
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "while retrieving invites", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    protected int getSchemaVersion( ) {
        return 0;  // TODO [ed] increment at every schema change
    }

    @Override
    protected String getCreateTableStatement( ) {
        return String.format(
                "CREATE TABLE %s(%s INTEGER, %s INTEGER, %s INTEGER)",
                TABLE_NAME, COLUMN_NAME_INVITEE, COLUMN_NAME_INVITER, COLUMN_NAME_TRIP
        );
    }

    @Override
    protected String getSchemaName( ) {
        return TABLE_NAME;
    }
}
