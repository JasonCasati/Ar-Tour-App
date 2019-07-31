package com.jasoncasati.tracking;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jasoncasati.Constants;
import com.jasoncasati.R;
import com.jasoncasati.rendering.CustomSurfaceView;
import com.jasoncasati.rendering.GLRenderer;
import com.jasoncasati.rendering.OccluderCube;
import com.jasoncasati.rendering.StrokedCube;
import com.jasoncasati.rendering.Driver;
import com.jasoncasati.places.Building;
import com.jasoncasati.util.DropDownAlert;
import com.wikitude.NativeStartupConfiguration;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.WikitudeError;
import com.wikitude.common.camera.CameraSettings;
import com.wikitude.common.rendering.RenderExtension;
import com.wikitude.rendering.ExternalRendering;
import com.wikitude.tracker.ObjectTarget;
import com.wikitude.tracker.ObjectTracker;
import com.wikitude.tracker.ObjectTrackerListener;
import com.wikitude.tracker.TargetCollectionResource;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ObjectTrackingActivity extends Activity implements ObjectTrackerListener, ExternalRendering {

    private static final String TAG = "ObjectTracking";

    private WikitudeSDK wikitudeSDK;
    private CustomSurfaceView customSurfaceView;
    private Driver driver;
    private GLRenderer glRenderer;
    private TargetCollectionResource targetCollectionResource;
    private DropDownAlert dropDownAlert;

    private Building buildingFoundFromGoogle;
    private String buildingChosenByUser;
    private Button detailsButton;
    private OkHttpClient client = new OkHttpClient();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.more_details_button);

        Intent intent = getIntent();
        buildingChosenByUser = intent.getStringExtra("buildingChosen");


        this.detailsButton = findViewById(R.id.building_information_button);
        this.detailsButton.setOnClickListener(v -> showBuildingInformation());

        wikitudeSDK = new WikitudeSDK(this);
        NativeStartupConfiguration startupConfiguration = new NativeStartupConfiguration();
        startupConfiguration.setLicenseKey(Constants.WIKITUDE_SDK_KEY);
        startupConfiguration.setCameraPosition(CameraSettings.CameraPosition.BACK);
        startupConfiguration.setCameraResolution(CameraSettings.CameraResolution.AUTO);

        wikitudeSDK.onCreate(getApplicationContext(), this, startupConfiguration);

        String trackerFilePath;
        switch(buildingChosenByUser){
            case "Cardiff Capitol Shopping Center":
                trackerFilePath = "file:///android_asset/cardiffcapitolcenter.wto";
                break;
            case "Cardiff Queens Building":
                trackerFilePath = "file:///android_asset/cardiffqueensbuilding.wto";
                break;
            case "Cardiff City Hall":
                trackerFilePath = "file:///android_asset/cardiffcityhall.wto";
                break;
            case "Cardiff Central Library":
                trackerFilePath = "file:///android_asset/cardifflibrary.wto";
                break;
            case "Cardiff Natural History Museum":
                trackerFilePath = "file:///android_asset/cardiffcapitolcenter.wto";
                break;
            case "Occluder Cube Example":
                trackerFilePath = "file:///android_asset/firetruck.wto";
                break;

            default: {trackerFilePath = null;}
        }

        if (trackerFilePath != null){
            this.targetCollectionResource = wikitudeSDK.getTrackerManager().createTargetCollectionResource(trackerFilePath);
        }

        wikitudeSDK.getTrackerManager().createObjectTracker(targetCollectionResource, ObjectTrackingActivity.this, null);

        dropDownAlert = new DropDownAlert(this);
        dropDownAlert.setText("Point the camera towards the building");
        dropDownAlert.setTextWeight(1);
        dropDownAlert.show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        wikitudeSDK.onResume();
        customSurfaceView.onResume();
        driver.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        customSurfaceView.onPause();
        driver.stop();
        wikitudeSDK.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        wikitudeSDK.onDestroy();
    }

    @Override
    public void onRenderExtensionCreated(final RenderExtension renderExtension) {
        glRenderer = new GLRenderer(renderExtension);
        wikitudeSDK.getCameraManager().setRenderingCorrectedFovChangedListener(glRenderer);
        customSurfaceView = new CustomSurfaceView(getApplicationContext(), glRenderer);
        driver = new Driver(customSurfaceView, 30);
        setContentView(customSurfaceView);
    }

    @Override
    public void onTargetsLoaded(ObjectTracker tracker) {
        Log.v(TAG, "Object tracker loaded");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onErrorLoadingTargets(ObjectTracker tracker, WikitudeError error) {
        Log.v(TAG, "Unable to load image tracker. Reason: " + error.getMessage());
    }

    @Override
    public void onObjectRecognized(ObjectTracker tracker, final ObjectTarget target) {
        Log.v(TAG, "Recognized target: " + target.getName());

        detailsButton.setVisibility(View.VISIBLE);

        StrokedCube strokedCube = new StrokedCube();
        OccluderCube occluderCube = new OccluderCube();

        glRenderer.setRenderablesForKey(target.getName(), strokedCube, occluderCube);

        Request request = buildFindPlaceRequest(target.getName());

        String result = run(request);
        String placeID = null;
        try {
            JSONObject jsobj = new JSONObject(result);
            JSONObject place_id = new JSONObject(jsobj.getJSONArray("candidates").get(0).toString());
            placeID = place_id.getString("place_id");
        } catch (JSONException e) {
            e.printStackTrace();
        }


        if(placeID != null){
            request = buildPlaceDetailsRequest(placeID);
            String response = run(request);
            buildingFoundFromGoogle = new Building(response);
        }
    }

    public Request buildPlaceDetailsRequest(String placeID) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("maps.googleapis.com")
                .addPathSegment("maps")
                .addPathSegment("api")
                .addPathSegment("place")
                .addPathSegment("details")
                .addPathSegment("json")
                .addQueryParameter("key", Constants.GOOGLE_API_KEY)
                .addQueryParameter("placeid", placeID)
                .addQueryParameter("fields", "name,opening_hours,rating")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        return request;
    }


    public String run(Request request) {
        try (Response response = client.newCall(request).execute()) {
            if(response.isSuccessful()){
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Request buildFindPlaceRequest(String name) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("maps.googleapis.com")
                .addPathSegment("maps")
                .addPathSegment("api")
                .addPathSegment("place")
                .addPathSegment("findplacefromtext")
                .addPathSegment("json")
                .addQueryParameter("key", Constants.GOOGLE_API_KEY)
                .addQueryParameter("input", name)
                .addQueryParameter("inputtype", "textquery")
                .addQueryParameter("fields", "place_id")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        return request;
    }
    public void showBuildingInformation() {

        new AlertDialog.Builder(ObjectTrackingActivity.this)
                .setTitle(R.string.building_information_title)
                .setMessage(
                        getString(R.string.building_information_name) + buildingFoundFromGoogle.getBuildingName() + "\n" +
                        getString(R.string.building_information_time_table) + buildingFoundFromGoogle.getOpeningTime()[0] + "\n" +
                                buildingFoundFromGoogle.getOpeningTime()[1] + "\n" +
                                buildingFoundFromGoogle.getOpeningTime()[2] + "\n" +
                                buildingFoundFromGoogle.getOpeningTime()[3] + "\n" +
                                buildingFoundFromGoogle.getOpeningTime()[4] + "\n" +
                                buildingFoundFromGoogle.getOpeningTime()[5] + "\n" +
                                buildingFoundFromGoogle.getOpeningTime()[6] + "\n" +
                        getString(R.string.building_information_rating) + buildingFoundFromGoogle.getRating() + "\n"
                )
                .show();
    }

    @Override
    public void onObjectTracked(ObjectTracker tracker, final ObjectTarget target) {
        StrokedCube strokedCube = (StrokedCube) glRenderer.getRenderableForKey(target.getName());
        if (strokedCube != null) {
            strokedCube.viewMatrix = target.getViewMatrix();

            strokedCube.setXScale(target.getTargetScale().x);
            strokedCube.setYScale(target.getTargetScale().y);
            strokedCube.setZScale(target.getTargetScale().z);
        }

        OccluderCube occluderCube = (OccluderCube) glRenderer.getOccluderForKey(target.getName());
        if (occluderCube != null) {
            occluderCube.viewMatrix = target.getViewMatrix();

            occluderCube.setXScale(target.getTargetScale().x);
            occluderCube.setYScale(target.getTargetScale().y);
            occluderCube.setZScale(target.getTargetScale().z);
        }
    }

    @Override
    public void onObjectLost(ObjectTracker tracker, final ObjectTarget target) {
        Log.v(TAG, "Lost target: " + target.getName());
        glRenderer.removeRenderablesForKey(target.getName());
    }

    @Override
    public void onExtendedTrackingQualityChanged(final ObjectTracker tracker, final ObjectTarget target, final int oldTrackingQuality, final int newTrackingQuality) {

    }
}

