package it.unitn.roadbuddy.app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.model.LatLng;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.Path;

import java.util.ArrayList;
import java.util.List;


public class TripsFragment extends Fragment {

    ViewPager mPager;
    PagerAdapter mPagerAdapter;
    CancellableAsyncTaskManager taskManager;
    View rootView;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<Path> pathList = new ArrayList<>( );

    public TripsFragment( ) {
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
        rootView = inflater.inflate( R.layout.fragment_trips, container, false );
        this.mPager = ( ViewPager ) getActivity( ).findViewById( R.id.pager );
        this.mPagerAdapter = ( PagerAdapter ) mPager.getAdapter( );

        mRecyclerView = ( RecyclerView ) rootView.findViewById( R.id.recycler_view );
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager( getContext( ) );
        mRecyclerView.setLayoutManager( mLayoutManager );

        this.taskManager = new CancellableAsyncTaskManager( );

        // Inflate the layout for this fragment
        inflater.inflate( R.layout.fragment_trips, container, false );
        LatLng myPos = new LatLng( 46.0829800, 11.1155410 );

        taskManager.startRunningTask( new getTrips( getContext( ) ), true, myPos );

        //fake data
        for ( int i = 0; i < 3; i++ ) {
            Path path = new Path( i, i * 3, i * 4, i * 5, Integer.toString( i ) );
            pathList.add( path );
        }

        return rootView;
    }

    public void updateList( ) {
        LatLng myPos = new LatLng( 46.0829800, 11.1155410 );
        taskManager.startRunningTask( new getTrips( getContext( ) ), true, myPos );
    }

    @Override
    public void onViewCreated( View view, @Nullable Bundle savedInstanceState ) {

        super.onViewCreated( view, savedInstanceState );

    }

    @Override
    public void onPause( ) {
        super.onPause( );
        taskManager.stopRunningTasksOfType( getTrips.class );
    }

    @Override
    public void onAttach( Context context ) {
        super.onAttach( context );
    }

    @Override
    public void onDetach( ) {
        super.onDetach( );
    }

    class getTrips extends CancellableAsyncTask<LatLng, Integer, List<Path>> {

        String exceptionMessage;
        Context context;

        public getTrips( Context context ) {
            super( taskManager );
            this.context = context;
        }

        @Override
        protected List<Path> doInBackground( LatLng... pos ) {

            try {
                List<Path> paths = DAOFactory.getPathDAO( ).getPathsFromPosition(
                        context, pos[ 0 ]
                );
                return paths;
            }
            catch ( BackendException e ) {
                Log.e( getClass( ).getName( ), "while getting trips from position", e );
                exceptionMessage = e.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( List<Path> res ) {

            Log.v( "res", Long.toString( res.size( ) ) );

            mAdapter = new TripsAdapter( res );
            mRecyclerView.setAdapter( mAdapter );


            super.onPostExecute( res );
        }
    }
}

