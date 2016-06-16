package it.unitn.roadbuddy.app.backend.models;


import java.util.List;

public class Trip {
    private int id;
    private List<User> participants;
    private Path path;

    public Trip( int id, List<User> participants, Path path ) {
        this.id = id;
        this.participants = participants;
        this.path = path;
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
