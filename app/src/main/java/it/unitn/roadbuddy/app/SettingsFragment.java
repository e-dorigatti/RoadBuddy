package it.unitn.roadbuddy.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class SettingsFragment extends Fragment {

    TextView txtUserName;
    ListView listSettings;

    public SettingsFragment( ) {

    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {

        return inflater.inflate( R.layout.fragment_settings, container, false );
    }

    @Override
    public void onViewCreated( View view, Bundle savedInstanceState ) {
        txtUserName = ( TextView ) view.findViewById( R.id.txtUserName );
        listSettings = ( ListView ) view.findViewById( R.id.listSettings );

        listSettings.setAdapter( new ArrayAdapter<>(
                getActivity( ).getApplicationContext( ),
                android.R.layout.simple_list_item_1,
                new String[] {
                        "Lunedì",
                        "Martedì",
                        "Mercoledì",
                        "Giovedì",
                        "Venerdì",
                        "Sabato",
                        "Domentica"
                }
        ) );
    }
}
