package it.unitn.roadbuddy.app.backend.postgres;


import java.sql.Connection;
import java.sql.SQLException;

public abstract class PostgresDAOBase {

    private Connection connection;

    protected PostgresDAOBase( ) throws SQLException {
        createTable( );
    }

    protected Connection getConnection( ) throws SQLException {
        if ( connection == null || connection.isClosed( ) )
            connection = PostgresUtils.getNewConnection( );
        return connection;
    }

    protected abstract int getSchemaVersion( );

    protected abstract String getSchemaName( );

    protected abstract String getCreateTableStatement( );

    protected void createTable( ) throws SQLException {
        String schemaName = getSchemaName( );
        int localSchemaVersion = getSchemaVersion( );
        int remoteSchemaVersion = PostgresUtils.getInstance( ).getSchemaVersion( schemaName );

        if ( remoteSchemaVersion < localSchemaVersion ) {
            getConnection( ).prepareStatement(
                    String.format( "DROP TABLE IF EXISTS %s", schemaName )
            ).execute( );

            getConnection( ).prepareStatement(
                    getCreateTableStatement( )
            ).execute( );

            PostgresUtils.getInstance( ).setSchemaVersion( schemaName, localSchemaVersion );
        }
    }
}
