package it.unitn.roadbuddy.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.PointOfInterest;

import java.util.List;


public class MainActivity
        extends AppCompatActivity
        implements OnMapClickListener,
                   OnMapLongClickListener,
                   OnCameraChangeListener,
                   OnMapReadyCallback {

    TextView mTapTextView;

    TextView mCameraTextView;

    GoogleMap map;

    LinearLayout lyMapButtons;

    FrameLayout mainFrameLayout;

    NFA nfa;

    View currentMenuBar;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

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

    @Override
    public void onMapClick( LatLng point ) {

    }

    @Override
    public void onMapLongClick( final LatLng point ) {

    }

    @Override
    public void onCameraChange( final CameraPosition position ) {

    }

    public void RefreshMapContent( ) {
        LatLngBounds bounds = map.getProjection( ).getVisibleRegion( ).latLngBounds;
        List<PointOfInterest> points = DAOFactory.getPoiDAOFactory( ).getPOIsInside(
                getApplicationContext( ), bounds
        );

        map.clear( );
        for ( PointOfInterest p : points ) {
            map.addMarker( p.drawToMap( map ) );
        }
    }

    public View setMenuBar( int view ) {
        removeMenuBar( );

        currentMenuBar = getLayoutInflater( ).inflate( view, mainFrameLayout, false );
        mainFrameLayout.addView( currentMenuBar );

        return currentMenuBar;
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
        Toast.makeText( getApplicationContext( ), text, Toast.LENGTH_LONG ).show( );
    }
}