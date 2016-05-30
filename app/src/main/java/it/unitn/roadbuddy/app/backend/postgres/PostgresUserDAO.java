package it.unitn.roadbuddy.app.backend.postgres;


import it.unitn.roadbuddy.app.BuildConfig;
import it.unitn.roadbuddy.app.Utils;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.UserDAO;
import it.unitn.roadbuddy.app.backend.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresUserDAO extends PostgresDAOBase implements UserDAO {

    public static final String
            COLUMN_NAME_ID = "id",
            COLUMN_NAME_USERNAME = "username",
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
                    String.format( "INSERT INTO %s(%s) VALUES (?) RETURNING %s",
                                   getSchemaName( ), COLUMN_NAME_USERNAME, COLUMN_NAME_ID )

            );
            stmtInsertUser.setString( 1, newUserData.getUserName( ) );
            ResultSet res = stmtInsertUser.executeQuery( );
            Utils.Assert( res.next( ), true );

            long newUserID = res.getLong( COLUMN_NAME_ID );
            return new User( newUserID, newUserData.getUserName( ) );
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    public User getUser( long id ) throws BackendException {
        try ( Connection conn = PostgresUtils.getInstance( ).getConnection( ) ) {
            PreparedStatement stmt = conn.prepareStatement(
                    String.format( "SELECT %s FROM %s WHERE %s = ?",
                                   COLUMN_NAME_USERNAME, getSchemaName( ), COLUMN_NAME_ID )

            );
            stmt.setLong( 1, id );
            ResultSet res = stmt.executeQuery( );
            if ( res.next( ) ) {
                return new User( id, res.getString( COLUMN_NAME_USERNAME ) );
            }
            else return null;
        }
        catch ( SQLException exc ) {
            throw new BackendException( exc.getMessage( ), exc );
        }
    }

    @Override
    protected int getSchemaVersion( ) {
        return 1;  // TODO [ed] increment at every schema change
    }

    @Override
    public String getSchemaName( ) {
        return TABLE_NAME;
    }

    @Override
    protected String getCreateTableStatement( ) {
        return String.format( "CREATE TABLE %s(%s SERIAL PRIMARY KEY, %s VARCHAR(100))",
                              getSchemaName( ), COLUMN_NAME_ID, COLUMN_NAME_USERNAME );
    }
}
