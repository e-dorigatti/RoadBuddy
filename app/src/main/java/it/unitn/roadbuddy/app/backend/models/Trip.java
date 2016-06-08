package it.unitn.roadbuddy.app.backend.models;


import java.util.List;

public class Trip {
    private long id;
    private List<User> participants;
    private Path path;

    public Trip( long id, List<User> participants, Path path ) {
        this.id = id;
        this.participants = participants;
        this.path = path;
    }

    public long getId( ) {
        return id;
    }

    public List<User> getParticipants( ) {
        return participants;
    }

    public Path getPath( ) {
        return path;
    }
}
