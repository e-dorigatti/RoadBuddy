package it.unitn.roadbuddy.app;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;


/**
 * Array adapter which allows each item to customize its view
 */
public class DynamicViewArrayAdapter extends ArrayAdapter<DynamicViewArrayAdapter.Listable> {
    public DynamicViewArrayAdapter( Context context ) {
        super( context, -1 );
    }

    public View getView( int position, View convertView, ViewGroup parent ) {
        return getItem( position ).getView( position, convertView, parent );
    }

    public interface Listable {
        View getView( int position, View convertView, ViewGroup parent );
    }
}
