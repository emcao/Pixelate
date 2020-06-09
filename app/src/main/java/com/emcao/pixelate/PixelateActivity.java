package com.emcao.pixelate;

import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PixelateActivity extends AppCompatActivity {
    static final int DEFAULT_PIXEL_SIZE = 5;
    static final String FILENAME_PREFIX = "Pixelate_";
    static final String FILENAME_SUFFIX = ".jpg";
    static final String INTENT_EXTRA_IMAGE = "PixelateImagePath";
    static final String INTENT_EXTRA_ORIGIN = "PixelateFromCamera";
    static final String LOG_TAG = "Pixelate Activity";
    static final String FOLDER = "/Pixelate/";
    static final String SAVE_SUCCESS = "Image saved in Pixelate folder";
    static final String SCAN_COMPLETED = "Scan completed - file available in Gallery";
    static final String FOLDER_NOT_CREATED = "Unable to create folder for saved images";
    static final String FILE_NOT_DELETED = "Unable to delete camera file";
    static final String FILE_TIME_PATTERN = "yyyyMMdd_HHmmss";

    private PixelateImage pixelateImage;
    private ImageView imageView;
    private TextView pixelSizeText;
    private SeekBar pixelSizeBar;
    private String photoPath;
    private ImageSelectionHelper imageSelectionHelper;

    private SeekBar.OnSeekBarChangeListener pixelBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            setImageView(i);
            pixelSizeText.setText(String.valueOf(i));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) { }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            setImageView(progress);
            pixelSizeText.setText(String.valueOf(progress));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pixelate);
        imageView = findViewById(R.id.pixelate_image);
        pixelSizeText = findViewById(R.id.pixel_size);
        pixelSizeBar = findViewById(R.id.pixel_size_bar);
        pixelSizeBar.setOnSeekBarChangeListener(pixelBarListener);

        imageSelectionHelper = new ImageSelectionHelper();

        Intent intent = getIntent();
        boolean fromCamera = intent.getBooleanExtra(INTENT_EXTRA_ORIGIN, false);
        photoPath = intent.getStringExtra(INTENT_EXTRA_IMAGE);

        initializeImage(photoPath, fromCamera);
    }

    public void newImageOnClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.new_button)
                .setItems(R.array.image_selection_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                takePicture();
                                break;
                            case 1:
                                selectImageFromGallery();
                                break;
                        }
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void saveOnClick(View view) {
        boolean permission = PermissionManager.checkStoragePermission(this);

        if (permission) {
            imageView.invalidate();
            BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
            Bitmap pixelatedBitmap = bitmapDrawable.getBitmap();
            File fileToSave = createFileToSave();

            try {
                FileOutputStream fos = new FileOutputStream(fileToSave);
                pixelatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                Toast.makeText(getApplicationContext(), SAVE_SUCCESS, Toast.LENGTH_SHORT).show();

                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            MediaScannerConnection.scanFile(this,
                    new String[] { fileToSave.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.d(LOG_TAG, SCAN_COMPLETED);
                        }
                    });
        }
        else {
            Toast.makeText(this, PermissionManager.PERMISSION_DENIED, Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeImage(String photoPath, boolean fromCamera) {
        Bitmap originalBitmap = getOriginalBitmap(photoPath);

        if (fromCamera) {
            File cameraFile = new File(photoPath);

            if (!cameraFile.delete()) {
                Log.d(LOG_TAG, FILE_NOT_DELETED);
            }
        }

        pixelateImage = new PixelateImage(originalBitmap);

        pixelSizeBar.setMax(pixelateImage.getMaxPixelSize());
        pixelSizeBar.setProgress(DEFAULT_PIXEL_SIZE);
    }

    private void setImageView(int pixelSize) {
        Bitmap bitmap = pixelateImage.getImageView(pixelSize);
        imageView.setImageBitmap(bitmap);
    }

    private Bitmap getOriginalBitmap(String photoPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);

        try {
            bitmap = getRotatedBitmap(bitmap, photoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private Bitmap getRotatedBitmap(Bitmap bitmap, String photoPath) throws IOException {
        ExifInterface ei = new ExifInterface(photoPath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateBitmap(bitmap, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateBitmap(bitmap, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateBitmap(bitmap, 270);
            default:
                return bitmap;
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private File createFileToSave() {
        String timeStamp = new SimpleDateFormat(FILE_TIME_PATTERN, Locale.getDefault()).format(new Date());
        String fileName = FILENAME_PREFIX + timeStamp + FILENAME_SUFFIX;

        File filePath = Environment.getExternalStorageDirectory();
        File fileDir = new File(filePath.getAbsolutePath() + FOLDER);

        if (!fileDir.exists() && !fileDir.mkdir()) {
            Log.d(LOG_TAG, FOLDER_NOT_CREATED);
        }

        return new File(fileDir, fileName);
    }

    private void takePicture() {
        photoPath = imageSelectionHelper.dispatchTakePictureIntent(this);
    }

    private void selectImageFromGallery() {
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
                    initializeImage(photoPath, true);
                }
                break;
            case ImageSelectionHelper.REQUEST_GALLERY_IMAGE:
                if (resultCode == RESULT_OK) {
                    photoPath = imageSelectionHelper.getPathFromUri(this, data.getData());
                    initializeImage(photoPath, false);
                }
                break;
        }
    }
}