package com.example.inventorymanager.camera;

import static androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.inventorymanager.R;
import com.example.inventorymanager.utils.ToastUtils;
import com.example.inventorymanager.databinding.FragmentItemCameraBinding;
import com.example.inventorymanager.item.ItemAddFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.mlkit.common.model.CustomRemoteModel;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.linkfirebase.FirebaseModelSource;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for Adding an Image for New Item with Camera
 * Uses CameraX and MLKit for Object Detection and Image Capture
 * Passes the object name and image path to ItemAddFragment
 */
public class ItemCameraFragment extends Fragment {
    private FragmentItemCameraBinding binding;
    PreviewView previewView;
    LifecycleCameraController cameraController;
    Button captureButton;
    Button viewImageButton;
    Button skipImageButton;
    MaterialSwitch objectSwitch;
    String selectedImagePath;
    ObjectOverlay objectOverlay;
    private String currentImagePath;
    private ObjectDetector objectDetector;
    private String Object_name = null;
    private File photoFile;
    private static final int REQUEST_PERMISSIONS = 1;
    private static final String CAMERA_PERMISSION = android.Manifest.permission.CAMERA;
    private boolean canClickButton = true;
    private static final int PICK_IMAGE_REQUEST = 1;
    NavController navController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentItemCameraBinding.inflate(inflater, container, false);

        navController = NavHostFragment.findNavController(this);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Enable camera if permissions are granted
        if (checkPermissions()) {
            setupCustomModel();
        }
        setupUIFunctions();
    }

    // Setups A Custom Model from Firebase for Object Detection
    private void setupCustomModel() {
        CustomRemoteModel remoteModel =
                new CustomRemoteModel
                        .Builder(new FirebaseModelSource.Builder("Object-Detector").build())
                        .build();

        DownloadConditions downloadConditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
        RemoteModelManager.getInstance().download(remoteModel, downloadConditions)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            ToastUtils.showToast(requireContext(), "Using Custom Model");

                            // Use Custom Model
                            CustomObjectDetectorOptions customObjectDetectorOptions =
                                    new CustomObjectDetectorOptions.Builder(remoteModel)
                                            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                                            .enableClassification()
                                            .setClassificationConfidenceThreshold(0.5f)
                                            .setMaxPerObjectLabelCount(10)
                                            .build();
                            // Create the object detector
                            objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);

                            // Start the Camera
                            startCamera();
                        } else {
                            // Download failed
                            ToastUtils.showToast(requireContext(), "Download Failed, Using Base Model");

                            // Use Base Model
                            ObjectDetectorOptions options =
                                    new ObjectDetectorOptions.Builder()
                                            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                                            .enableClassification()  // Optional
                                            .build();
                            objectDetector = ObjectDetection.getClient(options);

                            // Start the Camera
                            startCamera();
                        }
                    }
                });
    }

    /* Setups a camera controller for the camera preview
    Pass a string from the object classified to the database */
    private void startCamera() {
        // Set Buttons To Visible
        objectSwitch.setVisibility(View.VISIBLE);
        captureButton.setVisibility(View.VISIBLE);
        viewImageButton.setVisibility(View.VISIBLE);

        objectOverlay = new ObjectOverlay(requireContext());

        cameraController = new LifecycleCameraController(requireContext());
        // Camera Selector Use Case
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        cameraController.setCameraSelector(cameraSelector);

        // Initialize Preview use case
        previewView = binding.previewView;

        // Add MlKitAnalyzer for Object Detection
        cameraController.setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(requireContext()),
                new MlKitAnalyzer(List.of(objectDetector),
                        COORDINATE_SYSTEM_VIEW_REFERENCED,
                        ContextCompat.getMainExecutor(requireContext()), result -> {
                    // Handle object detection results
                    List<DetectedObject> objects = result.getValue(objectDetector);

                    // Remove the objectOverlay if no objects are detected
                    if (objects == null || objects.isEmpty()) {
                        previewView.removeView(objectOverlay);
                    } else {
                        for (DetectedObject object : objects) {
                            // Remove the objectOverlay if it already exists
                            if (objectOverlay.getParent() != null) {
                                previewView.removeView(objectOverlay);
                            }

                            // Set Object for drawing
                            objectOverlay.setObject(object);

                            // Add Drawing to Preview
                            previewView.addView(objectOverlay);

                            // Set a string as the object name
                            for (DetectedObject.Label label : object.getLabels()) {
                                Object_name = label.getText();
                            }
                        }
                        if (!cameraController.isImageAnalysisEnabled()) {
                            previewView.removeView(objectOverlay);
                            Object_name = null;
                        }
                    }
                })
        );

        cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE);

        cameraController.bindToLifecycle(this);
        previewView.setController(cameraController);
    }

    // Setup Functionality for UI Elements
    private void setupUIFunctions(){
        captureButton = binding.captureButton;
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(canClickButton) {
                    canClickButton = false;
                    captureImage();
                }
            }
        });

        viewImageButton = binding.viewImageButton;
        viewImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageDirectory();
            }
        });

        skipImageButton = binding.skipImageButton;
        skipImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenAddItem(Object_name, null);
            }
        });

        objectSwitch = binding.objectDetectionSwitch;
        objectSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ToastUtils.showToast(requireContext(), "Object Detection Enabled");
                cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE | CameraController.IMAGE_ANALYSIS);
            } else {
                // Remove the view from its current parent, if it has one
                cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE);
                if(!cameraController.isImageAnalysisEnabled()){
                    ToastUtils.showToast(requireContext(), "Object Detection Disabled");
                    previewView.removeView(objectOverlay);
                    Object_name = null;
                }
            }
        });

        MaterialToolbar toolbar = binding.topAppBar;
        toolbar.setNavigationOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    // Captures the image and saves it to a temporary file
    private void captureImage() {
        photoFile = createImageFile();

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        cameraController.takePicture(outputFileOptions, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Image saved successfully
                        currentImagePath = photoFile.getAbsolutePath();

                        // Add toast message for when the image is captured
                        ToastUtils.showToast(requireContext(), "Image Captured");

                        OpenAddItem(Object_name, currentImagePath);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        // Handle error
                        ToastUtils.showToast(requireContext(), "Error Capturing Image");
                    }
                });
    }

    // Create a temporary file to store the image taken
    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = requireContext().getExternalCacheDir();

        assert storageDir != null;
        Log.d("DirectoryPath", storageDir.getAbsolutePath());
        File imageFile = null;
        try {
            imageFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            // Handle errors while creating the file
            Log.e("CreateImageFile", "Error creating image file", e);
        }
        return imageFile;
    }

    // Open the image directory to select an image
    private void openImageDirectory() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        }
    }

    // Get Result of Image Selection
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        requireActivity();
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            // Handle the selected image URI here
            Uri selectedImageUri = data.getData();
            selectedImagePath = getPathFromUri(selectedImageUri);
            OpenAddItem(Object_name, selectedImagePath);
        }
    }

    // Get the image URI from the intent and create a temporary file to be passed
    private String getPathFromUri(Uri uri) {
        String filePath = "";

        // Use ContentResolver to get an InputStream for the selected image URI
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                // Copy the InputStream to a temporary file
                File tempFile = createTempFileFromInputStream(inputStream);
                filePath = tempFile.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filePath;
    }

    // Create a temporary file from an InputStream
    private File createTempFileFromInputStream(InputStream inputStream) throws IOException {
        File tempFile = File.createTempFile("temp_image", null, requireContext().getCacheDir());
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer, 0, 8192)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    // Navigate to ItemAddFragment with the object name and image path
    public void OpenAddItem(String Object_name, String imagePath) {
        Bundle bundle = new Bundle();
        bundle.putString(ItemAddFragment.EXTRA_ITEM_OBJECT_NAME, Object_name);
        bundle.putString(ItemAddFragment.EXTRA_ITEM_IMAGE_PATH, imagePath);
        navController.navigate(R.id.action_itemCameraFragment_to_itemAddFragment, bundle);
    }

    // Check if the permissions (camera) are granted
    private boolean checkPermissions() {
        int cameraPermission = ContextCompat.checkSelfPermission(requireContext(), CAMERA_PERMISSION);

        List<String> permissionsList = new ArrayList<>();

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(CAMERA_PERMISSION);
        }

        if (!permissionsList.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsList.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
        return cameraPermission == PackageManager.PERMISSION_GRANTED;
    }

    // Start camera if permission are granted
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            // Check if all permissions are granted
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                ToastUtils.showToast(requireContext(),"Camera permissions were denied. " +
                        "Cannot take image");
            }
            else {
                setupCustomModel();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Reset Object name everytime the activity is Started
        Object_name = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        // Unbind LifecycleCameraController to release camera resources
        if (cameraController != null) {
            cameraController.unbind();
        }
    }
}