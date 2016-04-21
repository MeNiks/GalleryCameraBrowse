package com.niks.gallerycamerabrowse;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.FileDescriptor;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements View.OnClickListener {

    private final static int TAKE_CAMERA_REQUEST = 1, TAKE_GALLERY_REQUEST = 2;

    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 4;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 5;

    private Uri mMakePhotoUri;
    private int prev_request_code;
    private int prev_result_code;
    private Intent prev_intent;

    private boolean check_storage_permission;

    private Button btn_gallery, btn_camera;



    private ImageView imageview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageview = (ImageView) findViewById(R.id.imageView1);
        btn_gallery = (Button) findViewById(R.id.btn_gallery);
        btn_gallery.setOnClickListener(this);
        btn_camera = (Button) findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if (v == btn_gallery) {
            checkForReadExternalStoragePermission();
        } else if (v == btn_camera) {
            checkForCameraPermission();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        this.prev_request_code = requestCode;
        this.prev_result_code = resultCode;
        this.prev_intent = data;
        Bitmap postImage = null;
        if (requestCode == TAKE_CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                check_storage_permission = true;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                String name = Build.MODEL;

                try {
                    Uri selectedImageUri = data.getData();
                    String imagePath = getRealPathFromURI(selectedImageUri);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    options.inDither = false; // Disable Dithering mode
//                    options.inPurgeable = true;
                    postImage = BitmapFactory.decodeFile(imagePath, options);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (postImage == null && !name.contains("Nexus")) {
                    postImage = (Bitmap) data.getExtras().get("data");
                }
                imageview.setImageBitmap(postImage);

            }

        } else if (requestCode == TAKE_GALLERY_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            try {
                postImage = (Bitmap) data.getExtras().get("data");
            } catch (Exception e) {
                e.printStackTrace();
            }


            if (postImage == null) {
                Uri selectedImageUri = data.getData();
                String[] filePathColumn = {Media.DATA};
                Cursor cursor = this.getContentResolver().query(selectedImageUri, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String imagePath = cursor.getString(columnIndex);
                if (!TextUtils.isEmpty(imagePath)) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    options.inDither = false;
                    postImage = BitmapFactory.decodeFile(imagePath, options);
                }
                cursor.close();
                if (postImage == null) {
                    ParcelFileDescriptor parcelFileDescriptor;
                    try {
                        parcelFileDescriptor = this.getContentResolver().openFileDescriptor(selectedImageUri, "r");
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        postImage = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                        parcelFileDescriptor.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            imageview.setImageBitmap(postImage);

        }

    }

    public String getRealPathFromURI(Uri contentUri) {
        String photoPath = "";
        Cursor cursor = this.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, new String[]
                {Media.DATA, Media.DATE_ADDED, MediaStore.Images.ImageColumns.ORIENTATION}, Media.DATE_ADDED, null, "date_added ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndex(Media.DATA)));
                photoPath = uri.toString();
            } while (cursor.moveToNext());
            cursor.close();
        }
        return photoPath;
    }

    private void checkForCameraPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    TAKE_CAMERA_REQUEST);
        } else {
            checkForWriteExternalStoragePermission();
        }
    }

    private void checkForWriteExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "IMG_" + timeStamp+ ".jpg");
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            mMakePhotoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String name = Build.MODEL;
        if (name.contains("Nexus"))
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,mMakePhotoUri);
        startActivityForResult(cameraIntent, TAKE_CAMERA_REQUEST);
    }

    private void checkForReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, TAKE_GALLERY_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case TAKE_CAMERA_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkForCameraPermission();
                }
                return;
            }
            case REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (check_storage_permission) {
                        check_storage_permission = false;
                        onActivityResult(prev_request_code, prev_result_code, prev_intent);
                    } else {
                        openCamera();
                    }
                }
                return;
            }
            case REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                }
                return;
            }
        }
    }
}
