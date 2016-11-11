package com.pokescanner.events;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Brian on 7/31/2016.
 */
public class ScanCircleEvent {
    public LatLng pos;
    public int color;
    public boolean isBanned;
    public String username;

    public ScanCircleEvent(LatLng pos,boolean isBanned,String username, int color) {
        this.isBanned = isBanned;
        this.username = username;
        this.pos = pos;
        this.color = color;
    }
}
