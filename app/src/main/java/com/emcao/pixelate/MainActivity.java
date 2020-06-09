package com.emcao.pixelate;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    static final String INTENT_EXTRA_IMAGE = "PixelateImagePath";
    static final String INTENT_EXTRA_ORIGIN = "PixelateFromCamera";

    private String photoPath;
    private ImageSelectionHelper imageSelectionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageSelectionHelper = new ImageSelectionHelper();
    }

    public void takePicture(View view) {
        photoPath = imageSelectionHelper.dispatchTakePictureIntent(this);
    }

    public void selectImageFromGallery(View view) {
        boolean permission = PermissionManager.checkStoragePermission(this);

        if (permission) {
            imageSelectionHelper.dispatchSelectImageFromGallery(this);
        }
        else {
            Toast.makeText(this, PermissionManager.PERMISSION_DENIED, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ImageSelectionHelper.REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Intent newActivityIntent = new Intent(this, PixelateActivity.class);
                    newActivityIntent.putExtra(INTENT_EXTRA_ORIGIN, true);
                    newActivityIntent.putExtra(INTENT_EXTRA_IMAGE, photoPath);
                    startActivity(newActivityIntent);
                }
                break;
            case ImageSelectionHelper.REQUEST_GALLERY_IMAGE:
                if (resultCode == RESULT_OK) {
                    Intent newActivityIntent = new Intent(this, PixelateActivity.class);
                    newActivityIntent.putExtra(INTENT_EXTRA_ORIGIN, false);
                    photoPath = imageSelectionHelper.getPathFromUri(this, data.getData());
                    newActivityIntent.putExtra(INTENT_EXTRA_IMAGE, photoPath);
                    startActivity(newActivityIntent);
                }
                break;
        }
    }
}