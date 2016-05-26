package it.unitn.roadbuddy.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.CommentPOI;
import it.unitn.roadbuddy.app.backend.models.Path;
import it.unitn.roadbuddy.app.backend.models.User;

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
    User currentUser;

    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );


    public MapFragment( ) {
        // Required empty public constructor
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( getActivity( ) );
        long user_id = pref.getLong( SettingsFragment.KEY_PREF_USER_ID, -1 );

        taskManager.startRunningTask( new GetCurrentUserAsync( ), true, user_id );
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
        taskManager.stopRunningTask( RefreshMapAsync.class );

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
        LatLngBounds bounds = googleMap.getProjection( ).getVisibleRegion( ).latLngBounds;
        taskManager.startRunningTask( new RefreshMapAsync( getContext( ) ), true, bounds );
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
            selectedDrawable.setSelected( getContext( ), false );

        selectedDrawable = d;

        if ( selectedDrawable != null )
            selectedDrawable.setSelected( getContext( ), true );
    }

    class RefreshMapAsync extends CancellableAsyncTask<LatLngBounds, Integer, List<Drawable>> {

        String exceptionMessage;
        Context context;

        public RefreshMapAsync( Context context ) {
            super( taskManager );
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
                Log.e( getClass( ).getName( ), "while refreshing map", e );
                exceptionMessage = e.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( List<Drawable> drawables ) {
            if ( drawables != null ) {
                for ( Map.Entry<String, Drawable> entry : shownDrawables.entrySet( ) ) {
                    entry.getValue( ).RemoveFromMap( context );
                }

                shownDrawables.clear( );
                for ( Drawable d : drawables ) {
                    String displayed = d.DrawToMap( context, googleMap );
                    shownDrawables.put( displayed, d );

                    if ( d.equals( selectedDrawable ) ) {
                        // they represent the same database object but are actually different references
                        selectedDrawable = d;
                        d.setSelected( context, true );
                    }
                    else d.setSelected( context, false );
                }
            }
            else if ( exceptionMessage != null ) {
                showToast( exceptionMessage );
            }
            else {
                showToast( R.string.generic_backend_error );
            }

            super.onPostExecute( drawables );
        }
    }

    class GetCurrentUserAsync extends CancellableAsyncTask<Long, Integer, User> {

        String exceptionMessage;

        public GetCurrentUserAsync( ) {
            super( taskManager );
        }

        @Override
        protected User doInBackground( Long... userID ) {
            try {
                return DAOFactory.getUserDAO( ).getUser( userID[ 0 ] );
            }
            catch ( BackendException exc ) {
                exceptionMessage = exc.getMessage( );
                Log.e( getClass( ).getName( ), "while retrieving current user", exc );
                return null;
            }
        }

        @Override
        protected void onPostExecute( User user ) {
            if ( user != null ) {
                currentUser = user;
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
