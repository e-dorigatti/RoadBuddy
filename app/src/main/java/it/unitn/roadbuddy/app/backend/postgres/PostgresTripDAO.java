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
    public Trip getTrip( int id ) throws BackendException {
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
            stmtCreateTrip.setInt( 1, path.getId( ) );
        else stmtCreateTrip.setObject( 1, null );

        ResultSet res = stmtCreateTrip.executeQuery( );
        Utils.Assert( res.next( ), true );

        int tripId = res.getInt( COLUMN_NAME_ID );

        if ( creator != null ) {
            PreparedStatement stmtUpateCreator = conn.prepareStatement(
                    String.format(
                            "UPDATE %s SET %s = ? WHERE %s =?",
                            PostgresUserDAO.TABLE_NAME,
                            PostgresUserDAO.COLUMN_NAME_TRIP,
                            PostgresUserDAO.COLUMN_NAME_ID
                    )
            );
            stmtUpateCreator.setInt( 1, tripId );
            stmtUpateCreator.setInt( 2, creator.getId( ) );

            int updatedCount = stmtUpateCreator.executeUpdate( );
            if ( updatedCount != 1 ) {  // definitely should not happen
                conn.rollback( );
                Utils.Assert( false, true );
            }
        }

        conn.commit( );

        return getTrip( tripId );
    }


    protected Trip getTripWithinPostgres( Connection conn, int id ) throws SQLException {
        String usersAlias = "u", pathsAlias = "p", tripsAlias = "t";

        PreparedStatement stmt = conn.prepareStatement(
                String.format(
                        "SELECT %1$s.%4$s AS %1$s_%4$s, %1$s.%5$s AS %1$s_%5$s, %1$s.%6$s AS %1$s_%6$s, " +
                                "%1$s.%7$s AS %1$s_%7$s, %1$s.%8$s AS %1$s_%8$s, %2$s.%9$s AS %2$s_%9$s, " +
                                "%2$s.%10$s AS %2$s_%10$s, %2$s.%11$s AS %2$s_%11$s, %2$s.%12$s AS %2$s_%12$s, " +
                                "%2$s.%13$s AS %2$s_%13$s, %2$s.%14$s AS %2$s_%14$s, %3$s.%15$s AS %3$s_%15$s " +

                                "FROM %19$s AS %3$s LEFT OUTER JOIN %17$s AS %1$s ON %1$s.%8$s = %3$s.%15$s " +
                                "LEFT OUTER JOIN %18$s AS %2$s ON %2$s.%9$s = %3$s.%16$s " +
                                "WHERE %3$s.%15$s = ?",

                        // aliases (1-3)
                        usersAlias, pathsAlias, tripsAlias,

                        // user columns (4-8)
                        PostgresUserDAO.COLUMN_NAME_ID,
                        PostgresUserDAO.COLUMN_NAME_USERNAME,
                        PostgresUserDAO.COLUMN_NAME_LAST_POSITION,
                        PostgresUserDAO.COLUMN_NAME_LAST_POSITION_UPDATED,
                        PostgresUserDAO.COLUMN_NAME_TRIP,

                        // path columns (9-14)
                        PostgresPathDAO.COLUMN_NAME_ID,
                        PostgresPathDAO.COLUMN_NAME_OWNER,
                        PostgresPathDAO.COLUMN_NAME_PATH,
                        PostgresPathDAO.COLUMN_NAME_DISTANCE,
                        PostgresPathDAO.COLUMN_NAME_DURATION,
                        PostgresPathDAO.COLUMN_NAME_DESCRIPTION,

                        // trips column (15-16)
                        PostgresTripDAO.COLUMN_NAME_ID,
                        PostgresTripDAO.COLUMN_NAME_PATH,

                        // table names (17-19)
                        PostgresUserDAO.TABLE_NAME,
                        PostgresPathDAO.TABLE_NAME,
                        PostgresTripDAO.TABLE_NAME
                )
        );

        stmt.setInt( 1, id );

        ResultSet res = stmt.executeQuery( );

        // the data about the trip and the path will be repeated for every user
        List<User> participants = null;
        Path path = null;
        int tripId = -1;

        usersAlias = usersAlias + "_";
        pathsAlias = pathsAlias + "_";
        tripsAlias = tripsAlias + "_";

        while ( res.next( ) ) {
            if ( participants == null ) {
                Object pathId = res.getObject( pathsAlias + PostgresPathDAO.COLUMN_NAME_ID );
                if ( pathId != null )
                    path = PostgresPathDAO.readPath( res, pathsAlias );

                tripId = res.getInt( tripsAlias + PostgresTripDAO.COLUMN_NAME_ID );
                participants = new ArrayList<>( );
            }

            User u = new User(
                    res.getInt( usersAlias + PostgresUserDAO.COLUMN_NAME_ID ),
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
