package com.image.multipleimageupload;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.UploadTask;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.image.multipleimageupload.Common.Common;
import com.image.multipleimageupload.Model.CategoryItem;
import com.image.multipleimageupload.Model.WallpaperItem;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ImageView image_preview;
    Button btn_browse, btn_upload;
    MaterialSpinner spinner;
    private Uri filePath;
    EditText title;
    EditText author;

    String categoryIdSelect = "";

    //Material Data
    Map<String, String> spinnerData = new HashMap<>();

    //FireStorage
    FirebaseStorage storage;
    StorageReference storageReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();


        //View
        image_preview = (ImageView)findViewById(R.id.image_preview);
        btn_upload = (Button)findViewById(R.id.btn_upload);
        btn_browse = (Button)findViewById(R.id.btn_browser);
        spinner = (MaterialSpinner)findViewById(R.id.spinner);
        title = (EditText)findViewById(R.id.description1);
        author = (EditText)findViewById(R.id.author);

        //Load Spinner data
        loadCategoryToSpinner();

        btn_browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFromGallery();
            }
        });

        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(spinner.getSelectedIndex() == 0) //Hint, not choose anymore
                    Toast.makeText(MainActivity.this, "Choose your category first", Toast.LENGTH_SHORT).show();
                else if (title.getText().toString().equals("") || title.getText().toString().equals(null) )
                    Toast.makeText(MainActivity.this, "Name your wallpaper first", Toast.LENGTH_SHORT).show();
                else if (author.getText().toString().equals("") || title.getText().toString().equals(null) )
                    Toast.makeText(MainActivity.this, "Insert your author name first", Toast.LENGTH_SHORT).show();

                else
                    upload();
            }
        });

    }

    private void pickFromGallery(){


        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(intent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, Common.PICK_IMAGE_REQUEST);


        /*Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), Common.PICK_IMAGE_REQUEST);*/
    }


       /* @Override
        public void onClick(View v) {


            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent,PICK_IMAGE);
        }*/


    private void upload() {
        if(filePath != null)
        {
            Log.d("uploading", "test");
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            String nameOfFile = UUID.randomUUID().toString();
            final StorageReference ref = storageReference.child(new StringBuilder("images/").append(nameOfFile).toString());

            ref.putFile(filePath)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //When the image has successfully uploaded, get its download URL
                            ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Uri dlUri = uri;
                                    Log.d("Download URI", dlUri.toString());
                                    saveUrlToCategory(categoryIdSelect, dlUri.toString(), title.getText().toString(), author.getText().toString());
                                    progressDialog.dismiss();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage("Uploaded: "+(int)progress+"%");
                        }
                    });
        }
    }

    private void saveUrlToCategory(String categoryIdSelect, String imageLink, String title, String author) {
        FirebaseDatabase.getInstance().getReference(Common.STR_WALLPAPER)
                .push() // generate key
                .setValue(new WallpaperItem(imageLink, categoryIdSelect, title, author ))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                        finish();

                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                });
    }


    @SuppressLint("MissingSuperCall")
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode){
                case Common.PICK_IMAGE_REQUEST:
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };

                    Cursor cursor = getContentResolver().query(selectedImage,
                            filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    filePath = selectedImage;
                    cursor.close();

                    Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
                    image_preview.setImageBitmap(bitmap);
                    btn_upload.setEnabled(true);
                    break;
            }
    }

    private void loadCategoryToSpinner() {
        FirebaseDatabase.getInstance()
                .getReference(Common.STR_CATEGORY_BACKGROUND)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot postSnapshot : dataSnapshot.getChildren())
                        {

                            CategoryItem item = postSnapshot.getValue(CategoryItem.class);
                            String key = postSnapshot.getKey();
                            spinnerData.put(key, item.getName());



                        }
                        //Because Material Spinner will not receive hint so, we need custom hint.
                        Object[] valueArray = spinnerData.values().toArray();
                        List<Object> valueList = new ArrayList<>(Arrays.asList(valueArray));
                        valueList.add(0,"Category"); // Adding first item is a hint
//                        valueList.addAll(Arrays.asList(valueArray)); // And add all the remain category names;

                        spinner.setItems(valueList); // Set source data for spinner
                        spinner.setOnItemSelectedListener( new MaterialSpinner.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                                //When user choose category, we will get categoryId key
                                Object[] keyArray = spinnerData.keySet().toArray();
                                List<Object> keyList = new ArrayList<>(Arrays.asList(keyArray));
                                keyList.add(0, "Category_Key");
                                keyList.addAll(Arrays.asList(keyArray));
                                categoryIdSelect = keyList.get(position).toString(); //Assign key when user choose category

                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

}