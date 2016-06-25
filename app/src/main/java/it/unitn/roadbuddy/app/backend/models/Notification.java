package it.unitn.roadbuddy.app.backend.models;


import android.os.Parcel;
import android.os.Parcelable;

public class Notification implements Parcelable {
    public static final Parcelable.Creator<Notification> CREATOR
            = new Parcelable.Creator<Notification>( ) {

        public Notification createFromParcel( Parcel in ) {
            return new Notification( in );
        }

        public Notification[] newArray( int size ) {
            return new Notification[ size ];
        }
    };

    public static final int
            NOTIFICATION_PING = 1;

    private int id;
    private int type;
    private User sender;

    public Notification( int id, User sender, int type ) {
        this.id = id;
        this.type = type;
        this.sender = sender;
    }

    public Notification( Parcel parcel ) {
        id = parcel.readInt( );
        type = parcel.readInt( );
        sender = parcel.readParcelable( ClassLoader.getSystemClassLoader( ) );
    }

    public int getId( ) {
        return id;
    }

    public int getType( ) {
        return type;
    }

    public User getSender( ) {
        return sender;
    }

    @Override
    public void writeToParcel( Parcel parcel, int i ) {
        parcel.writeInt( id );
        parcel.writeInt( type );
        parcel.writeParcelable( sender, i );
    }

    @Override
    public int describeContents( ) {
        return 0;
    }
}
