package it.unitn.roadbuddy.app.backend.postgres;


import android.util.Log;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.InviteDAO;
import it.unitn.roadbuddy.app.backend.models.Invite;
import it.unitn.roadbuddy.app.backend.models.Trip;
import it.unitn.roadbuddy.app.backend.models.User;

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
            COLUMN_NAME_ID = "id",
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
    public void removeInvite( int invite ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "DELETE FROM %s WHERE %s = ?",
                            TABLE_NAME, COLUMN_NAME_ID
                    )
            );

            stmt.setInt( 1, invite );
            stmt.execute( );
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "while retrieving invites", exc );
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    public List<Invite> retrieveInvites( int user ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format(
                            "SELECT %s, %s, %s FROM %s WHERE %s = ?",
                            COLUMN_NAME_ID, COLUMN_NAME_TRIP, COLUMN_NAME_INVITER,
                            TABLE_NAME, COLUMN_NAME_INVITEE
                    )
            );

            stmt.setInt( 1, user );

            ResultSet res = stmt.executeQuery( );
            List<Invite> invites = new ArrayList<>( );
            while ( res.next( ) ) {

                /**
                 * For performance reasons we should add an explicit case for when the trip dao
                 * and/or the user dao use the postgres backend and retrieve this information with
                 * an explicit join (just like we do in the trip dao, for example)
                 *
                 * On the other hand, currently the invites are retrieved periodically in a background
                 * thread so we can afford a couple of seconds of delay. We could not do this (probably)
                 * if we left the user waiting for this operation to complete, as the UI should be
                 * fast and responsive.
                 */
                Trip trip = DAOFactory.getTripDAO( ).getTrip( res.getInt( COLUMN_NAME_TRIP ) );
                User inviter = DAOFactory.getUserDAO( ).getUser( res.getInt( COLUMN_NAME_INVITER ) );
                int id = res.getInt( COLUMN_NAME_ID );

                Invite invite = new Invite( id, inviter, null, trip );
                invites.add( invite );
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
        return 1;  // TODO [ed] increment at every schema change
    }

    @Override
    protected String getCreateTableStatement( ) {
        return String.format(
                "CREATE TABLE %s(%s SERIAL PRIMARY KEY, %s INTEGER, %s INTEGER, %s INTEGER)",
                TABLE_NAME, COLUMN_NAME_ID, COLUMN_NAME_INVITEE, COLUMN_NAME_INVITER, COLUMN_NAME_TRIP
        );
    }

    @Override
    protected String getSchemaName( ) {
        return TABLE_NAME;
    }
}
