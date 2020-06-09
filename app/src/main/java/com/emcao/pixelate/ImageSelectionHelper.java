package com.emcao.pixelate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;
import androidx.loader.content.CursorLoader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageSelectionHelper {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_GALLERY_IMAGE = 2;
    static final String LOG_TAG = "Pixelate Image Select";
    static final String FILENAME_PREFIX = "Pixelate_";
    static final String FILENAME_SUFFIX = ".jpg";
    static final String FILEPROVIDER_AUTHORITY = "com.emcao.pixelate.fileprovider";
    static final String FILE_TIME_PATTERN = "yyyyMMdd_HHmmss";

    public ImageSelectionHelper() { }

    public String dispatchTakePictureIntent(Activity activity) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile(activity);
            }
            catch (IOException ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
            }

            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(activity, FILEPROVIDER_AUTHORITY, photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                return photoFile.getAbsolutePath();
            }
        }

        return null;
    }

    public void dispatchSelectImageFromGallery(Activity activity) {
        Intent galleryPictureIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(galleryPictureIntent, REQUEST_GALLERY_IMAGE);
    }

    public String getPathFromUri(Context context, Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(context, uri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }

        return null;
    }

    private File createImageFile(Activity activity) throws IOException {
        String timeStamp = new SimpleDateFormat(FILE_TIME_PATTERN, Locale.getDefault()).format(new Date());
        String imageFileName = FILENAME_PREFIX + timeStamp;
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, FILENAME_SUFFIX, storageDir);
    }
}