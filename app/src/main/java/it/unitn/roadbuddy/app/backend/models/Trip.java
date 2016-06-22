package it.unitn.roadbuddy.app.backend.models;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Trip implements Parcelable {
    public static final Parcelable.Creator<Trip> CREATOR
            = new Parcelable.Creator<Trip>( ) {

        public Trip createFromParcel( Parcel in ) {
            return new Trip( in );
        }

        public Trip[] newArray( int size ) {
            return new Trip[ size ];
        }
    };

    private int id;
    private List<User> participants;
    private Path path;

    public Trip( int id, List<User> participants, Path path ) {
        this.id = id;
        this.participants = participants;
        this.path = path;
    }

    public Trip( Parcel parcel ) {
        id = parcel.readInt( );

        participants = new ArrayList<>( );
        parcel.readTypedList( participants, User.CREATOR );

        path = ( Path ) parcel.readSerializable( );
    }

    @Override
    public void writeToParcel( Parcel parcel, int i ) {
        parcel.writeInt( id );
        parcel.writeTypedList( participants );
        parcel.writeSerializable( path );
    }

    @Override
    public int describeContents( ) {
        return 0;
    }

    public int getId( ) {
        return id;
    }

    public List<User> getParticipants( ) {
        return participants;
    }

    public Path getPath( ) {
        return path;
    }
}
