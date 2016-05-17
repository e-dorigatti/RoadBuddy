package it.unitn.roadbuddy.app;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.github.clans.fab.FloatingActionMenu;
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


public class MapFragment extends Fragment implements OnMapReadyCallback {


    FrameLayout mainFrameLayout;
    View currentMenuBar;
    GoogleMap googleMap;
    NFA nfa;
    Map<String, Drawable> shownDrawables = new HashMap<>( );
    Drawable selectedDrawable;

    RefreshMapAsync asyncRefresh;

    public MapFragment( ) {
        // Required empty public constructor
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        // Inflate the layout for this fragment
        return inflater.inflate( R.layout.fragment_map, container, false );
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        super.onViewCreated( view, savedInstanceState );

        this.mainFrameLayout = ( FrameLayout ) view.findViewById( R.id.mainFrameLayout );

        SupportMapFragment mapFragment = ( SupportMapFragment ) getChildFragmentManager( ).findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );
    }

    @Override
    public void onPause( ) {
        if ( asyncRefresh != null )
            asyncRefresh.cancel( true );

        if ( nfa != null )
            nfa.Pause( );

        super.onPause( );
    }

    @Override
    public void onResume( ) {
        if ( nfa != null )
            nfa.Resume( );

        super.onResume( );
    }

    @Override
    public void onMapReady( GoogleMap map ) {
        this.googleMap = map;
        nfa = new NFA( this, new RestState( ) );
    }

    public void RefreshMapContent( ) {
        if ( asyncRefresh != null )
            asyncRefresh.cancel( true );

        LatLngBounds bounds = googleMap.getProjection( ).getVisibleRegion( ).latLngBounds;
        asyncRefresh = new RefreshMapAsync( getActivity( ).getApplicationContext( ) );
        asyncRefresh.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, bounds );
    }


    public View setCurrentMenuBar( int view ) {
        View v = getActivity( ).getLayoutInflater( ).inflate( view, mainFrameLayout, false );
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
                currentMenuBar.setVisibility( View.INVISIBLE );
            else
                currentMenuBar.setVisibility( View.VISIBLE );
        }
    }

    public void showToast( String text ) {
        Toast.makeText( getActivity( ).getApplicationContext( ), text, Toast.LENGTH_LONG ).show( );
    }

    public void showToast( int textId ) {
        String msg = getString( textId );
        showToast( msg );
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
        Context context;

        public RefreshMapAsync( Context context ) {
            this.context = context;
        }

        protected List<Drawable> doInBackground( LatLngBounds... bounds ) {
            try {
                List<Drawable> results = new ArrayList<>( );

                List<Path> paths = DAOFactory.getPathDAO( ).getPathsInside(
                        context, bounds[ 0 ]
                );

                List<CommentPOI> commentPOIs =
                        DAOFactory.getPoiDAOFactory( )
                                  .getCommentPoiDAO( )
                                  .getCommentPOIsInside(
                                          context,
                                          bounds[ 0 ]
                                  );

                for ( Path p : paths )
                    results.add( new DrawablePath( p ) );

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
                    String displayed = d.DrawToMap( googleMap );
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

            asyncRefresh = null;
        }
    }
}


