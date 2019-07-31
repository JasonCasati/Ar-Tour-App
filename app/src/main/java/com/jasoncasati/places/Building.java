package com.jasoncasati.places;

import org.json.JSONException;
import org.json.JSONObject;


public class Building {

    private String name;
    private String[] openingTimes;
    private double rating;

    public Building (String jsonString){
        try {
            JSONObject response = new JSONObject(jsonString).getJSONObject("result");
            JSONObject opening_times = null;
            try {
                if (response.getJSONObject("opening_hours") != null) {
                    opening_times = response.getJSONObject("opening_hours");
                }
            } catch (JSONException e){
                    e.printStackTrace();
            }

            if (response.getString("name") != null) {
                this.name = response.getString("name");
            }

            if(response.getDouble("rating") < 0){
                this.rating = response.getDouble("rating");
            }

            if (opening_times.getJSONArray("weekday_text") != null){
                this.openingTimes = new String[opening_times.getJSONArray("weekday_text").length()];
                for (int i = 0; i < openingTimes.length; i++){
                    openingTimes[i] = opening_times.getJSONArray("weekday_text").get(i).toString();
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String[] getOpeningTime() {
        return openingTimes;
    }

    public void setOpeningTime(String[] openingTime) {
        this.openingTimes = openingTime;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getBuildingName() {
        return name;
    }

    public void setBuildingName(String name) {
        this.name = name;
    }
}
