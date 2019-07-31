package com.jasoncasati;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.jasoncasati.tracking.ObjectTrackingActivity;
import com.jasoncasati.util.SampleCategory;
import com.jasoncasati.util.SampleData;
import com.jasoncasati.util.ExpendableListAdapter;
import com.wikitude.WikitudeSDK;
import com.wikitude.common.CallStatus;
import com.wikitude.common.devicesupport.Feature;
import com.wikitude.common.permission.PermissionManager;
import com.wikitude.common.util.SDKBuildInformation;
import com.wikitude.tracker.ImageTrackerConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ExpandableListView.OnChildClickListener {

    private static final String TAG = "MainActivity";

    private static final int EXPANDABLE_INDICATOR_START_OFFSET = 60;
    private static final int EXPANDABLE_INDICATOR_END_OFFSET = 30;

    private ExpandableListView listView;

    private final List<SampleCategory> sampleCategories = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WikitudeSDK.deleteRootCacheDirectory(this);


        sampleCategories.add(new SampleCategory(getString(R.string.object_tracking), getSampleDataListFromNamesForTracking(getResources().getStringArray(R.array.objectTracking_samples),
                EnumSet.of(Feature.OBJECT_TRACKING))));


        ExpendableListAdapter adapter = new ExpendableListAdapter(this, sampleCategories);

        listView = findViewById(R.id.listView);
        moveExpandableIndicatorToRight();
        listView.setOnChildClickListener(this);
        listView.setAdapter(adapter);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showSdkBuildInformation();
                return false;
            }
        });
        setSupportActionBar(toolbar);
    }

    public boolean onChildClick(ExpandableListView expandableListView, View view, final int groupPosition, final int childPosition, long id) {
        final SampleData sampleData = sampleCategories.get(groupPosition).getSamples().get(childPosition);

        if (!sampleData.getIsDeviceSupporting()) {
            showDeviceMissingFeatures(sampleData.getIsDeviceSupportingError());
        } else {
            WikitudeSDK.getPermissionManager().checkPermissions(this, new String[]{Manifest.permission.CAMERA}, PermissionManager.WIKITUDE_PERMISSION_REQUEST, new PermissionManager.PermissionManagerCallback() {
                @Override
                public void permissionsGranted(int requestCode) {

                    SampleCategory sampleCategory = sampleCategories.get(groupPosition);
                    final String buildingName = sampleCategory.getSamples().get(childPosition).getName();
                    Log.d(TAG, "buildName - gets samples name");
                    Intent intent = new Intent(MainActivity.this, ObjectTrackingActivity.class);

                    switch (buildingName) {
                        case "Cardiff Queens Building":
                            intent.putExtra("buildingChosen", "Cardiff Queens Building");
                            break;
                        case "Cardiff Capitol Shopping Center":
                            intent.putExtra("buildingChosen", "Cardiff Capitol Shopping Center");
                            break;
                        case "Cardiff Natural History Museum":
                            intent.putExtra("buildingChosen", "Cardiff Natural History Museum");
                            break;
                        case "Cardiff City Hall":
                            intent.putExtra("buildingChosen", "Cardiff City Hall");
                            break;
                        case "Cardiff Central Library":
                            intent.putExtra("buildingChosen", "Cardiff Central Library");
                            break;
                        case "Occluder Cube Example":
                            intent.putExtra("buildingChosen", "firetruck");
                            break;
                    }
                    startActivity(intent);
                }

                @Override
                public void permissionsDenied(String[] deniedPermissions) {
                    Toast.makeText(MainActivity.this, "The Wikitude SDK needs the following permissions to enable an AR experience: " + Arrays.toString(deniedPermissions), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void showPermissionRationale(final int requestCode, final String[] permissions) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Wikitude Permissions");
                    alertBuilder.setMessage("The Wikitude SDK needs the following permissions to enable an AR experience: " + Arrays.toString(permissions));
                    alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            WikitudeSDK.getPermissionManager().positiveRationaleResult(requestCode, permissions);
                        }
                    });

                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                }
            });
        }
        return false;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        WikitudeSDK.getPermissionManager().onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void moveExpandableIndicatorToRight() {
        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        listView.setIndicatorBoundsRelative(width - dpToPx(EXPANDABLE_INDICATOR_START_OFFSET), width - dpToPx(EXPANDABLE_INDICATOR_END_OFFSET));
        listView.setIndicatorBoundsRelative(width - dpToPx(EXPANDABLE_INDICATOR_START_OFFSET), width - dpToPx(EXPANDABLE_INDICATOR_END_OFFSET));
    }

    private int dpToPx(int dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private List<SampleData> getSampleDataListFromNamesForTracking(String[] arrayNames, EnumSet<Feature> features) {
        List<SampleData> sampleDataImageTracking = new ArrayList<>();
        for (String arrayName : arrayNames) {
            boolean isDeviceSupported;
            String isDeviceSupportedMessage;

            CallStatus callStatus = WikitudeSDK.isDeviceSupporting(this, features);
            if (callStatus.isSuccess()) {
                isDeviceSupported = true;
                isDeviceSupportedMessage = "";
            } else {
                isDeviceSupported = false;
                isDeviceSupportedMessage = callStatus.getError().getMessage();
            }

            SampleData item = new SampleData(arrayName, isDeviceSupported, isDeviceSupportedMessage);
            sampleDataImageTracking.add(item);
        }

        return sampleDataImageTracking;
    }


    public void showSdkBuildInformation() {
        final SDKBuildInformation sdkBuildInformation = WikitudeSDK.getSDKBuildInformation();
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.build_information_title)
                .setMessage(
                        getString(R.string.build_information_config) + sdkBuildInformation.getBuildConfiguration() + "\n" +
                                getString(R.string.build_information_date) + sdkBuildInformation.getBuildDate() + "\n" +
                                getString(R.string.build_information_number) + sdkBuildInformation.getBuildNumber() + "\n" +
                                getString(R.string.build_information_version) + WikitudeSDK.getSDKVersion()
                )
                .show();
    }


    public void showDeviceMissingFeatures(String errorMessage) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.device_missing_features)
                .setMessage(errorMessage)
                .show();
    }

}
