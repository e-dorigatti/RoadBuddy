package it.unitn.roadbuddy.app;


import android.content.Context;
import com.google.android.gms.maps.GoogleMap;


/**
 * This interface bridges database objects with objects displayed on the map
 */
public interface Drawable {
    /**
     * Draws the object on the map.
     * This method will be called only once.
     * Draw the object as initially unselected.
     */
    String DrawToMap( Context context, GoogleMap map );

    /**
     * Set whether the object is selected by the user or not.
     * Use this method, for example, to change the appearance
     * of the drawn object.
     */
    void setSelected( Context context, GoogleMap map, boolean selected );

    /**
     * Delete the graphic object from the map
     */
    void RemoveFromMap( Context context );

    /**
     * Whether the two drawables share the same underlying object.
     * This should not depend on the appearance
     */
    boolean equals( Drawable other );

    /**
     * Additional details to be shown
     */
    SliderContentFragment getInfoFragment( );
}
