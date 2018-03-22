package Utils;

/**
 * Created by geogeeks on 19/01/2018.
 */

import android.location.Location;

public class ARPoint {
    Location location;
    String name;
    String type;

    public ARPoint(String name, String tipo, double lat, double lon, double altitude) {
        this.name = name;
        this.type = tipo;
        location = new Location("ARPoint");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAltitude(altitude);
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String getType(){
        return type;
    }
}

