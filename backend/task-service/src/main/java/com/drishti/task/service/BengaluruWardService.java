package com.drishti.task.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class BengaluruWardService {

    private static final double MIN_LAT = 12.7342;
    private static final double MAX_LAT = 13.1739;
    private static final double MIN_LNG = 77.3791;
    private static final double MAX_LNG = 77.7840;

    public static class WardInfo {
        private int    wardNumber;
        private String wardName;
        private String zone;

        public WardInfo(int wardNumber, String wardName, String zone) {
            this.wardNumber = wardNumber;
            this.wardName   = wardName;
            this.zone       = zone;
        }
        public int    getWardNumber() { return wardNumber; }
        public String getWardName()   { return wardName;   }
        public String getZone()       { return zone;       }
        public int    wardNumber()    { return wardNumber; }
        public String wardName()      { return wardName;   }
        public String zone()          { return zone;       }
    }

    private static class WardEntry {
        int    number;
        String name;
        String zone;
        double lat;
        double lng;

        WardEntry(int number, String name, String zone, double lat, double lng) {
            this.number = number;
            this.name   = name;
            this.zone   = zone;
            this.lat    = lat;
            this.lng    = lng;
        }
    }

    private static final List<WardEntry> WARDS = new ArrayList<>();

    static {
        WARDS.add(new WardEntry(150, "Koramangala 1st Block", "South",  12.9279, 77.6271));
        WARDS.add(new WardEntry(151, "Koramangala 4th Block", "South",  12.9347, 77.6205));
        WARDS.add(new WardEntry(81,  "Indiranagar",           "East",   12.9716, 77.6408));
        WARDS.add(new WardEntry(82,  "HAL 2nd Stage",         "East",   12.9630, 77.6484));
        WARDS.add(new WardEntry(198, "Whitefield",            "East",   12.9698, 77.7499));
        WARDS.add(new WardEntry(174, "HSR Layout",            "South",  12.9116, 77.6389));
        WARDS.add(new WardEntry(173, "Bommanahalli",          "South",  12.8997, 77.6271));
        WARDS.add(new WardEntry(20,  "Jayanagar 4th Block",   "South",  12.9308, 77.5831));
        WARDS.add(new WardEntry(19,  "Jayanagar 1st Block",   "South",  12.9382, 77.5820));
        WARDS.add(new WardEntry(63,  "MG Road",               "Central",12.9756, 77.6097));
        WARDS.add(new WardEntry(64,  "Brigade Road",          "Central",12.9716, 77.6101));
        WARDS.add(new WardEntry(110, "Malleshwaram",          "North",  13.0035, 77.5673));
        WARDS.add(new WardEntry(111, "Rajajinagar",           "North",  12.9902, 77.5557));
        WARDS.add(new WardEntry(55,  "Yelahanka",             "North",  13.1007, 77.5963));
        WARDS.add(new WardEntry(195, "Marathahalli",          "East",   12.9591, 77.6974));
        WARDS.add(new WardEntry(196, "Bellandur",             "East",   12.9256, 77.6784));
        WARDS.add(new WardEntry(33,  "BTM Layout",            "South",  12.9166, 77.6101));
        WARDS.add(new WardEntry(34,  "JP Nagar",              "South",  12.9063, 77.5857));
        WARDS.add(new WardEntry(92,  "Domlur",                "East",   12.9609, 77.6387));
        WARDS.add(new WardEntry(70,  "Shivajinagar",          "Central",12.9849, 77.6002));
    }

    public boolean isInBengaluru(double lat, double lng) {
        return lat >= MIN_LAT && lat <= MAX_LAT &&
               lng >= MIN_LNG && lng <= MAX_LNG;
    }

    public WardInfo getWardInfo(double lat, double lng) {
        if (!isInBengaluru(lat, lng)) {
            return new WardInfo(0, "Outside BBMP Limits", "Unknown");
        }

        WardEntry nearest = null;
        double    minDist = Double.MAX_VALUE;

        for (WardEntry ward : WARDS) {
            double dist = haversine(lat, lng, ward.lat, ward.lng);
            if (dist < minDist) {
                minDist = dist;
                nearest = ward;
            }
        }

        if (nearest != null) {
            return new WardInfo(nearest.number, nearest.name, nearest.zone);
        }
        return new WardInfo(0, "Bengaluru (Unclassified)", "Central");
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a    = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1))
                    * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
