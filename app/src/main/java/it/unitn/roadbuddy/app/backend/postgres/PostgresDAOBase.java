package it.unitn.roadbuddy.app.backend.postgres;


import java.sql.Connection;
import java.sql.SQLException;

public abstract class PostgresDAOBase {

    protected PostgresDAOBase( ) throws SQLException {
        createTable( );
    }

    protected abstract int getSchemaVersion( );

    protected abstract String getSchemaName( );

    protected abstract String getCreateTableStatement( );

    protected void createTable( ) throws SQLException {
        String schemaName = getSchemaName( );
        int localSchemaVersion = getSchemaVersion( );
        int remoteSchemaVersion = PostgresUtils.getInstance( ).getSchemaVersion( schemaName );

        if ( remoteSchemaVersion < localSchemaVersion ) {
            Connection conn = PostgresUtils.getInstance( ).getConnection( );

            conn.prepareStatement(
                    String.format( "DROP TABLE IF EXISTS %s", schemaName )
            ).execute( );

            conn.prepareStatement(
                    getCreateTableStatement( )
            ).execute( );

            conn.close( );

            PostgresUtils.getInstance( ).setSchemaVersion( schemaName, localSchemaVersion );
        }
    }
}
