package org.tum.idp.fcd;

public class GeoPoint {
    private double latitude = 0.0;
    private double longitude = 0.0;

    public GeoPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

}
