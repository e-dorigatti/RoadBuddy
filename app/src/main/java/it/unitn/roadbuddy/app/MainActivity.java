package it.unitn.roadbuddy.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;
import it.unitn.roadbuddy.app.backend.models.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity
        extends AppCompatActivity
        implements OnMapReadyCallback {

    TextView mTapTextView;
    TextView mCameraTextView;
    GoogleMap map;
    LinearLayout lyMapButtons;
    FrameLayout mainFrameLayout;
    NFA nfa;

    View currentMenuBar;

    Map<String, Drawable> shownDrawables = new HashMap<>( );
    Drawable selectedDrawable;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        try {
            Class.forName( "org.postgresql.Driver" );  // FIXME [ed] find a better place
        }
        catch ( ClassNotFoundException e ) {
            Log.e( getClass( ).getName( ), "backend exception", e );
            showToast( "could not load postgres jdbc driver" );
            finish( );
        }

        mTapTextView = ( TextView ) findViewById( R.id.tap_text );
        mCameraTextView = ( TextView ) findViewById( R.id.camera_text );
        lyMapButtons = ( LinearLayout ) findViewById( R.id.lyMapButtons );
        mainFrameLayout = ( FrameLayout ) findViewById( R.id.mainFrameLayout );

        SupportMapFragment mapFragment =
                ( SupportMapFragment ) getSupportFragmentManager( ).findFragmentById( R.id.map );

        mapFragment.getMapAsync( this );
    }

    @Override
    public void onMapReady( GoogleMap map ) {
        this.map = map;
        nfa = new NFA( this, new RestState( ) );
    }

    public void RefreshMapContent( ) {
        LatLngBounds bounds = map.getProjection( ).getVisibleRegion( ).latLngBounds;
        new RefreshMapAsync( ).executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, bounds );
    }

    public View setCurrentMenuBar( int view ) {
        View v = getLayoutInflater( ).inflate( view, mainFrameLayout, false );
        setCurrentMenuBar( v );

        return currentMenuBar;
    }

    public void setCurrentMenuBar( View v ) {
        removeMenuBar( );
        currentMenuBar = v;
        mainFrameLayout.addView( v );
    }

    public void removeMenuBar( ) {
        if ( currentMenuBar != null )
            mainFrameLayout.removeView( currentMenuBar );
        currentMenuBar = null;
    }

    public void showMenuBar( ) {
        if ( currentMenuBar != null )
            currentMenuBar.setVisibility( View.VISIBLE );
    }

    public void hideMenuBar( ) {
        if ( currentMenuBar != null )
            currentMenuBar.setVisibility( View.INVISIBLE );
    }

    public void toggleMenuBar( ) {
        if ( currentMenuBar != null ) {
            if ( currentMenuBar.getVisibility( ) == View.VISIBLE )
                hideMenuBar( );
            else
                showMenuBar( );
        }
    }

    public void showToast( String text ) {
        Toast.makeText( getApplicationContext( ), text, Toast.LENGTH_LONG ).show( );
    }

    public void showToast( int textId ) {
        String msg = getString( textId );
        showToast( msg );
    }

    public Drawable getSelectedDrawable( ) {
        return selectedDrawable;
    }

    public void setSelectedDrawable( Drawable d ) {
        if ( selectedDrawable != null )
            selectedDrawable.setSelected( false );

        selectedDrawable = d;

        if ( selectedDrawable != null )
            selectedDrawable.setSelected( true );
    }

    class RefreshMapAsync extends AsyncTask<LatLngBounds, Integer, List<Drawable>> {

        String exceptionMessage;

        protected List<Drawable> doInBackground( LatLngBounds... bounds ) {
            try {
                List<Drawable> results = new ArrayList<>( );

                List<Path> paths = DAOFactory.getPathDAO( ).getPathsInside(
                        getApplicationContext( ), bounds[ 0 ]
                );

                for ( Path p : paths )
                    results.add( new DrawablePath( p ) );

                List<CommentPOI> commentPOIs =
                        DAOFactory.getPoiDAOFactory( ).getCommentPoiDAO( )
                                  .getCommentPOIsInside( getApplicationContext( ),
                                                         bounds[ 0 ] );

                for ( CommentPOI p : commentPOIs )
                    results.add( new DrawableCommentPOI( p ) );

                return results;
            }
            catch ( BackendException e ) {
                Log.e( "roadbuddy", "backend exception", e );
                exceptionMessage = e.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( List<Drawable> drawables ) {
            if ( drawables != null ) {
                for ( Map.Entry<String, Drawable> entry : shownDrawables.entrySet( ) ) {
                    entry.getValue( ).RemoveFromMap( );
                }

                shownDrawables.clear( );
                for ( Drawable d : drawables ) {
                    String displayed = d.DrawToMap( map );
                    shownDrawables.put( displayed, d );

                    if ( d.equals( selectedDrawable ) ) {
                        // they represent the same database object but are actually different references
                        selectedDrawable = d;
                        d.setSelected( true );
                    }
                    else d.setSelected( false );
                }
            }
            else if ( exceptionMessage != null ) {
                showToast( exceptionMessage );
            }
            else {
                showToast( R.string.generic_backend_error );
            }
        }
    }
}
