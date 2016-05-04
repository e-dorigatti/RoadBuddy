package it.unitn.roadbuddy.app.backend.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class SQLiteDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "RoadBuddy.db";

    public SQLiteDbHelper( Context context ) {
        super( context, DATABASE_NAME, null, DATABASE_VERSION );
    }

    public void onCreate( SQLiteDatabase db ) {
        // TODO [ed] call this for each and every sqlite dao
        SQLiteCommentPoiDAO.getInstance( ).createTable( db );
    }

    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {

    }

    public void onDowngrade( SQLiteDatabase db, int oldVersion, int newVersion ) {

    }

    void clear( SQLiteDatabase db ) {
        SQLiteCommentPoiDAO.getInstance( ).dropTable( db );
    }
}