package com.jasoncasati;

import com.google.gson.Gson;
import com.jasoncasati.tracking.ObjectTrackingActivity;
import com.jasoncasati.places.Building;

import org.json.JSONException;
import org.junit.Test;

import okhttp3.Request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BuildingTests {

    ObjectTrackingActivity activity = new ObjectTrackingActivity();

    @Test
    public void ableToCreateBuildingFromJson() throws JSONException {
        Gson g = new Gson();

        Request request = activity.buildPlaceDetailsRequest("ChIJHUF9j_u3yhQRCqJHRD3UjUg");
        String response = activity.run(request);

        Building building = new Building(response);

        assertEquals("Capitol Shopping Center", building.getBuildingName());
        assertEquals(4.2, building.getRating(), 0);
        assertEquals(7, building.getOpeningTime().length);
        assertEquals("Monday: 10:00 AM – 10:00 PM", building.getOpeningTime()[0]);
        assertEquals("Tuesday: 10:00 AM – 10:00 PM", building.getOpeningTime()[1]);
        assertEquals("Wednesday: 10:00 AM – 10:00 PM", building.getOpeningTime()[2]);
        assertEquals("Thursday: 10:00 AM – 10:00 PM", building.getOpeningTime()[3]);
        assertEquals("Friday: 10:00 AM – 10:00 PM", building.getOpeningTime()[4]);
        assertEquals("Saturday: 10:00 AM – 10:00 PM", building.getOpeningTime()[5]);
        assertEquals("Sunday: 10:00 AM – 10:00 PM", building.getOpeningTime()[6]);
    }
}
