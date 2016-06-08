package it.unitn.roadbuddy.app.backend.postgres;


import android.util.Log;
import it.unitn.roadbuddy.app.Utils;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.TripDAO;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.Trip;
import it.unitn.roadbuddy.app.backend.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresTripDAO extends PostgresDAOBase implements TripDAO {

    /**
     * This DAO needs to interact with others in order to carry out
     * some of its tasks.
     * <p/>
     * Technically, since there is one DAO for every model, we cannot
     * assume that other models are stored in postgres too.
     * <p/>
     * The baseline for implementing this is to explicitly use the
     * DAO factory to get the proper DAO and use it to perform the
     * operation.
     * <p/>
     * On the other hand, if the other model is stored in postgres we
     * can (and should) take advantage of it and let postgres perform
     * joins and updates for a *considerable* efficiency gain.
     */

    public static final String
            COLUMN_NAME_ID = "id",
            COLUMN_NAME_PATH = "path",
            TABLE_NAME = "trips";

    private static PostgresTripDAO instance;

    protected PostgresTripDAO( ) throws SQLException {
        super( );
    }

    public static synchronized PostgresTripDAO getInstance( ) throws SQLException {
        if ( instance == null )
            instance = new PostgresTripDAO( );
        return instance;
    }

    @Override
    public Trip getTrip( long id ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {

            if ( DAOFactory.getPathDAO( ).getClass( ) == PostgresPathDAO.class &&
                    DAOFactory.getUserDAO( ).getClass( ) == PostgresUserDAO.class ) {

                return getTripWithinPostgres( conn, id );
            }
            else {
                throw new RuntimeException( "not implemented yet" );
            }
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "exception while retrieving comment pois", exc );
            throw new BackendException( "exception while retrieving comment pois", exc );
        }
    }

    @Override
    public Trip createTrip( Path path, User creator ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            if ( DAOFactory.getUserDAO( ).getClass( ) == PostgresUserDAO.class ) {
                return createTripWithinPostgres( conn, path, creator );
            }
            else {
                throw new RuntimeException( "not implemented yet" );
            }
        }
        catch ( SQLException exc ) {
            Log.e( getClass( ).getName( ), "exception while creating a trip", exc );
            throw new BackendException( "exception while creating a trip", exc );
        }
    }

    protected Trip createTripWithinPostgres( Connection conn, Path path, User creator )
            throws SQLException, BackendException {

        conn.setAutoCommit( false );

        PreparedStatement stmtCreateTrip = conn.prepareStatement(
                String.format(
                        "INSERT INTO %s(%s) VALUES (?) RETURNING %s",
                        getSchemaName( ), COLUMN_NAME_PATH, COLUMN_NAME_ID
                )
        );

        if ( path != null )
            stmtCreateTrip.setLong( 1, path.getId( ) );
        else stmtCreateTrip.setObject( 1, null );

        ResultSet res = stmtCreateTrip.executeQuery( );
        Utils.Assert( res.next( ), true );

        long tripId = res.getLong( COLUMN_NAME_ID );

        if ( creator != null ) {
            PreparedStatement stmtUpateCreator = conn.prepareStatement(
                    String.format(
                            "UPDATE %s SET %s = ? WHERE %s =?",
                            PostgresUserDAO.TABLE_NAME,
                            PostgresUserDAO.COLUMN_NAME_TRIP,
                            PostgresUserDAO.COLUMN_NAME_ID
                    )
            );
            stmtUpateCreator.setLong( 1, creator.getId( ) );

            int updatedCount = stmtUpateCreator.executeUpdate( );
            if ( updatedCount != 1 ) {  // definitely should not happen
                conn.rollback( );
                Utils.Assert( false, true );
            }
        }

        conn.commit( );

        return getTrip( tripId );
    }


    protected Trip getTripWithinPostgres( Connection conn, long id ) throws SQLException {
        String usersAlias = "u.", pathsAlias = "p.", tripsAlias = "t.";

        PreparedStatement stmt = conn.prepareStatement(
                String.format(
                        "SELECT %s%s, %s%s, %s%s, %s%s, %s%s, %s%s, %s%s, %s%s, %s%s, %s%s, %s%s" +
                                "FROM %s %s, %s %s, %s %s" +
                                "WHERE %s%s = %s%s AND %s%s = %s%s AND %s%s = ?",

                        // selected user data
                        usersAlias, PostgresUserDAO.COLUMN_NAME_ID,
                        usersAlias, PostgresUserDAO.COLUMN_NAME_USERNAME,
                        usersAlias, PostgresUserDAO.COLUMN_NAME_LAST_POSITION,
                        usersAlias, PostgresUserDAO.COLUMN_NAME_LAST_POSITION_UPDATED,
                        usersAlias, PostgresUserDAO.COLUMN_NAME_TRIP,

                        // selected path data
                        pathsAlias, PostgresPathDAO.COLUMN_NAME_ID,
                        pathsAlias, PostgresPathDAO.COLUMN_NAME_OWNER,
                        pathsAlias, PostgresPathDAO.COLUMN_NAME_DISTANCE,
                        pathsAlias, PostgresPathDAO.COLUMN_NAME_DURATION,
                        pathsAlias, PostgresPathDAO.COLUMN_NAME_DESCRIPTION,

                        // selected trip data
                        tripsAlias, PostgresTripDAO.COLUMN_NAME_ID,

                        // from clause
                        PostgresUserDAO.TABLE_NAME, usersAlias.substring( 0, usersAlias.length( ) - 1 ),
                        PostgresPathDAO.TABLE_NAME, pathsAlias.substring( 0, pathsAlias.length( ) - 1 ),
                        PostgresTripDAO.TABLE_NAME, tripsAlias.substring( 0, tripsAlias.length( ) - 1 ),

                        // join users and trips
                        usersAlias, PostgresUserDAO.COLUMN_NAME_TRIP,
                        tripsAlias, PostgresTripDAO.COLUMN_NAME_ID,

                        // join paths and trips
                        pathsAlias, PostgresPathDAO.COLUMN_NAME_ID,
                        tripsAlias, PostgresTripDAO.COLUMN_NAME_PATH,

                        // select only the requested trip
                        tripsAlias, PostgresTripDAO.COLUMN_NAME_ID
                )
        );

        stmt.setLong( 1, id );

        ResultSet res = stmt.executeQuery( );

        // the data about the trip and the path will be repeated for every user
        List<User> participants = null;
        Path path = null;
        long tripId = -1;

        while ( res.next( ) ) {
            if ( participants == null ) {
                path = new Path(
                        res.getLong( pathsAlias + PostgresPathDAO.COLUMN_NAME_ID ),
                        res.getLong( pathsAlias + PostgresPathDAO.COLUMN_NAME_OWNER ),
                        res.getLong( pathsAlias + PostgresPathDAO.COLUMN_NAME_DISTANCE ),
                        res.getLong( pathsAlias + PostgresPathDAO.COLUMN_NAME_DURATION ),
                        res.getString( pathsAlias + PostgresPathDAO.COLUMN_NAME_DESCRIPTION )
                );

                tripId = res.getLong( tripsAlias + PostgresTripDAO.COLUMN_NAME_ID );
                participants = new ArrayList<>( );
            }

            User u = new User(
                    res.getLong( usersAlias + PostgresUserDAO.COLUMN_NAME_ID ),
                    res.getString( usersAlias + PostgresUserDAO.COLUMN_NAME_USERNAME ),
                    PostgresUserDAO.readPosition( res, usersAlias ),
                    PostgresUserDAO.readDate( res, usersAlias ),
                    tripId
            );

            participants.add( u );
        }

        if ( tripId >= 0 )
            return new Trip( tripId, participants, path );
        else return null;
    }

    @Override
    protected String getCreateTableStatement( ) {
        return String.format(
                "CREATE TABLE %s(%s SERIAL PRIMARY KEY, " +
                        "%s INTEGER)",
                getSchemaName( ), COLUMN_NAME_ID, COLUMN_NAME_PATH
        );
    }

    @Override
    protected String getSchemaName( ) {
        return TABLE_NAME;
    }

    @Override
    protected int getSchemaVersion( ) {
        return 1; // TODO [ed] increment at every schema change
    }
}
