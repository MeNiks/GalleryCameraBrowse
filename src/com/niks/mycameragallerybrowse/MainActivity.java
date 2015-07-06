package com.niks.mycameragallerybrowse;

import java.io.FileDescriptor;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.example.mycameragallerybrowse.R;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity implements View.OnClickListener
{

	int TAKE_CAMERA = 1, TAKE_GALLERY = 2;

	private Button btn_gallery, btn_camera;
	ImageView imageview;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		imageview = (ImageView) findViewById(R.id.imageView1);
		btn_gallery = (Button) findViewById(R.id.btn_gallery);
		btn_gallery.setOnClickListener(this);
		btn_camera = (Button) findViewById(R.id.btn_camera);
		btn_camera.setOnClickListener(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		int id = item.getItemId();

		if (id == R.id.action_settings)
		{
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v)
	{
		if (v == btn_gallery)
		{
			Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, TAKE_GALLERY);
		} else if (v == btn_camera)
		{
			Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//			String name = Build.MODEL;
//			if (name.contains("Nexus"))
//			{
//				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//				ContentValues values = new ContentValues();
//				values.put(MediaStore.Images.Media.TITLE, "IMG_" + timeStamp + ".jpg");
//				Uri mMakePhotoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//				cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMakePhotoUri);
//			} 
			startActivityForResult(cameraIntent, TAKE_CAMERA);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		String name = Build.MODEL;
		super.onActivityResult(requestCode, resultCode, data);
		Bitmap postImage =null;
		if (requestCode == TAKE_CAMERA && resultCode == Activity.RESULT_OK)
		{
			String imagePath;
			Uri selectedImageUri = data.getData();
			if (selectedImageUri != null)
			{
				imagePath = getRealPathFromURI(selectedImageUri);
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 2;
				options.inDither = false;
				postImage = BitmapFactory.decodeFile(imagePath, options);
				if (postImage == null)
					postImage = (Bitmap) data.getExtras().get("data");
			} else
			{
				postImage = (Bitmap) data.getExtras().get("data");
			}
			imageview.setImageBitmap(postImage);

		} else if (requestCode == TAKE_GALLERY && resultCode == Activity.RESULT_OK && data != null)
		{
			try
			{
				postImage = (Bitmap) data.getExtras().get("data");
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			
			
			if(postImage==null)
			{
				Uri selectedImageUri = data.getData();
				String[] filePathColumn ={ MediaStore.Images.Media.DATA };
				Cursor cursor = this.getContentResolver().query(selectedImageUri, filePathColumn, null, null, null);
				cursor.moveToFirst();
				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				String imagePath = cursor.getString(columnIndex);
				if(!TextUtils.isEmpty(imagePath))
				{
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = 2;
					options.inDither = false; 
					postImage = BitmapFactory.decodeFile(imagePath, options);
				}
				cursor.close();
				if(postImage==null)
				{
					ParcelFileDescriptor parcelFileDescriptor;
					try
					{
						parcelFileDescriptor = this.getContentResolver().openFileDescriptor(selectedImageUri, "r");
						FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
						postImage = BitmapFactory.decodeFileDescriptor(fileDescriptor);
						parcelFileDescriptor.close();
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
	 
			imageview.setImageBitmap(postImage);

		}

	}

	public String getRealPathFromURI(Uri contentUri)
	{
		String photoPath = "";
		Cursor cursor = this.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, new String[]
		{ Media.DATA, Media.DATE_ADDED, MediaStore.Images.ImageColumns.ORIENTATION }, Media.DATE_ADDED, null, "date_added ASC");
		if (cursor != null && cursor.moveToFirst())
		{
			do
			{
				Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndex(Media.DATA)));
				photoPath = uri.toString();
			} while (cursor.moveToNext());
			cursor.close();
		}
		return photoPath;
	}

}
