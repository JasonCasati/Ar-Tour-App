package com.jasoncasati;

import com.jasoncasati.tracking.ObjectTrackingActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import okhttp3.Request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GoogleAPITests {

    ObjectTrackingActivity activity = new ObjectTrackingActivity();

    @Test
    public void buildsFindPlaceReqCorrectly(){

        Request request = activity.buildFindPlaceRequest("Capitol Shopping Center");

        assertEquals(
                "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?key=AIzaSyB4Z6lnN1dFthcuHLUMLhhP6YDiYqXtvYs&input=Capitol%20Shopping%20Center&inputtype=textquery&fields=place_id",
                request.url().toString());
    }

    @Test
    public void buildsPlaceDetailsReqCorrecty(){
        Request request = activity.buildPlaceDetailsRequest("ChIJHUF9j_u3yhQRCqJHRD3UjUg");

        assertEquals("https://maps.googleapis.com/maps/api/place/details/json?key=AIzaSyB4Z6lnN1dFthcuHLUMLhhP6YDiYqXtvYs&placeid=ChIJHUF9j_u3yhQRCqJHRD3UjUg&fields=name%2Copening_hours%2Crating",
                request.url().toString());
    }

    @Test
    public void runsRequest() throws JSONException {
        Request request = activity.buildFindPlaceRequest("Capitol Shopping Center");
        String result = activity.run(request);

        JSONObject jsobj = new JSONObject(result);
        JSONObject place_id = new JSONObject(jsobj.getJSONArray("candidates").get(0).toString());
        String placeID = place_id.getString("place_id");

        assertFalse(result == null);
        assertFalse(result.isEmpty());
        assertEquals("ChIJHUF9j_u3yhQRCqJHRD3UjUg", placeID);
    }
}
