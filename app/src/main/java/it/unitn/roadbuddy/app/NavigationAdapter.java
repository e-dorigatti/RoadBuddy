package it.unitn.roadbuddy.app;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import it.unitn.roadbuddy.app.backend.models.User;

public class NavigationAdapter extends RecyclerView.Adapter<NavigationAdapter.UserViewHolder> {

    private List<User> userList;

    public NavigationAdapter(List<User> userList) {
        this.userList = userList;
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.
                from(parent.getContext()).
                inflate(R.layout.trip_list_row, parent, false);

        return new UserViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(NavigationAdapter.UserViewHolder holder, int position) {
        User pi = userList.get(position);
        holder.vId.setText( Long.toString(pi.getId()) );
        holder.vUsername.setText( pi.getUserName() );

    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        protected TextView vId;
        protected TextView vUsername;

        public UserViewHolder(View itemView) {
            super(itemView);
            vId = (TextView) itemView.findViewById(R.id.user_id);
            vUsername = (TextView) itemView.findViewById(R.id.user_name);

        }
    }
}