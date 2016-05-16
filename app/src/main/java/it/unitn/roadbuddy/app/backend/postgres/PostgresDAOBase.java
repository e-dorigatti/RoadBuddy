package it.unitn.roadbuddy.app.backend.postgres;


import it.unitn.roadbuddy.app.backend.postgres.PostgresUtils;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class PostgresDAOBase {

    protected Connection dbConnection;

    protected PostgresDAOBase( ) throws SQLException {
        dbConnection = PostgresUtils.getConnection( );
        createTable( );
    }

    protected abstract int getSchemaVersion( );

    protected abstract String getSchemaName( );

    protected abstract String getCreateTableStatement( );

    protected void createTable( ) throws SQLException {
        String schemaName = getSchemaName( );
        int schemaVersion = getSchemaVersion( );

        if ( PostgresUtils.getInstance( ).getSchemaVersion( schemaName ) < schemaVersion ) {
            dbConnection.prepareStatement(
                    String.format( "DROP TABLE IF EXISTS %s", schemaName )
            ).execute( );

            dbConnection.prepareStatement(
                    getCreateTableStatement( )
            ).execute( );

            PostgresUtils.getInstance( ).setSchemaVersion( schemaName, schemaVersion );
        }
    }
}
