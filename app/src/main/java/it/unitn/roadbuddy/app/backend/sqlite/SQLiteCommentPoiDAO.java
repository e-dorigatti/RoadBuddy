package it.unitn.roadbuddy.app.backend.sqlite;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.CommentPoiDAO;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SQLiteCommentPoiDAO implements CommentPoiDAO {
    private static SQLiteCommentPoiDAO instance = null;

    private SQLiteCommentPoiDAO( ) {

    }

    public static SQLiteCommentPoiDAO getInstance( ) {
        if ( instance == null )
            instance = new SQLiteCommentPoiDAO( );
        return instance;
    }

    public void AddCommentPOI( Context c, CommentPOI p ) {
        SQLiteDbHelper helper = new SQLiteDbHelper( c );
        SQLiteDatabase db = helper.getWritableDatabase( );

        ContentValues values = new ContentValues( );
        values.put( CommentPoiContract.CommentPoiEntry.COLUMN_NAME_LATITUDE, p.getLatitude( ) );
        values.put( CommentPoiContract.CommentPoiEntry.COLUMN_NAME_LONGITUDE, p.getLongitude( ) );
        values.put( CommentPoiContract.CommentPoiEntry.COLUMN_NAME_TEXT, p.getText( ) );

        long rowId;
        rowId = db.insert( CommentPoiContract.CommentPoiEntry.TABLE_NAME, null, values );
    }

    public List<CommentPOI> getCommentPOIsInside( Context c, LatLngBounds bounds ) {
        SQLiteDbHelper helper = new SQLiteDbHelper( c );
        SQLiteDatabase db = helper.getReadableDatabase( );

        String[] projection = {
                CommentPoiContract.CommentPoiEntry.COLUMN_NAME_LATITUDE,
                CommentPoiContract.CommentPoiEntry.COLUMN_NAME_LONGITUDE,
                CommentPoiContract.CommentPoiEntry.COLUMN_NAME_TEXT
                //CommentPoiContract.CommentPoiEntry._ID,
        };

        String selection = String.format(
                "%1$s > ? AND %1$s < ? AND %2$s > ? AND %2$s < ?",
                CommentPoiContract.CommentPoiEntry.COLUMN_NAME_LATITUDE,
                CommentPoiContract.CommentPoiEntry.COLUMN_NAME_LONGITUDE
        );

        String[] selectionArgs = {
                Double.toString( bounds.southwest.latitude ),
                Double.toString( bounds.northeast.latitude ),
                Double.toString( bounds.southwest.longitude ),
                Double.toString( bounds.northeast.latitude )
        };

        Cursor cur = db.query(
                CommentPoiContract.CommentPoiEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        List<CommentPOI> points = new ArrayList<CommentPOI>( );
        if ( cur.moveToFirst( ) ) {
            do {
                CommentPOI p = new CommentPOI( 0, //cur.getLong( 3 ),
                                               cur.getDouble( 0 ),
                                               cur.getDouble( 1 ),
                                               cur.getString( 2 ) );
                points.add( p );
            } while ( cur.moveToNext( ) );
        }

        cur.close( );
        return points;
    }

    public void createTable( SQLiteDatabase db ) {
        db.execSQL( CommentPoiContract.SQL_CREATE );
    }

    public void dropTable( SQLiteDatabase db ) {
        db.execSQL( CommentPoiContract.SQL_DELETE );
    }
}

class CommentPoiContract {
    public static final String SQL_CREATE = String.format(
            Locale.getDefault( ),
            "CREATE TABLE IF NOT EXISTS %s(%s REAL, %s REAL, %s TEXT)",
            CommentPoiEntry.TABLE_NAME,
            CommentPoiEntry.COLUMN_NAME_LATITUDE,
            CommentPoiEntry.COLUMN_NAME_LONGITUDE,
            CommentPoiEntry.COLUMN_NAME_TEXT
    );

    public static final String SQL_DELETE = String.format(
            Locale.getDefault( ),
            "CREATE TABLE IF EXISTS %s",
            CommentPoiEntry.TABLE_NAME
    );

    public static abstract class CommentPoiEntry implements BaseColumns {
        public static final String
                TABLE_NAME = "CommentPOIs",
                COLUMN_NAME_LATITUDE = "latitude",
                COLUMN_NAME_LONGITUDE = "longitude",
                COLUMN_NAME_TEXT = "text";
    }
}