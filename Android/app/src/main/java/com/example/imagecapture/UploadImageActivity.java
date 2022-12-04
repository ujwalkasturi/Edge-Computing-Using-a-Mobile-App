package com.example.imagecapture;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadImageActivity extends AppCompatActivity{

    private Spinner spinner;
    private ImageView image1, image2, image3, image4;
    private TextView description;
    private ProgressBar progressBar;
    private Button uploadButton;
    String imageCategory;
    Uri capturedImageUri, topLeftImageUri, bottomLeftImageUri, topRightImageUri, bottomRightImageUri;
    Bitmap imageBitMap;
    byte[] byteArray;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_upload_image);
        image1 = findViewById(R.id.imageViewUpload1);
        image2 = findViewById(R.id.imageViewUpload2);
        image3 = findViewById(R.id.imageViewUpload3);
        image4 = findViewById(R.id.imageViewUpload4);
        description = findViewById(R.id.descriptionText);
        progressBar = findViewById(R.id.progressBar);
        uploadButton = findViewById(R.id.uploadButton);


        Bitmap topLeftImageBitMap = null, bottomLeftImageBitMap = null, topRightImageBitMap = null, bottomRightImageBitMap = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("capturedImageUri") && extras.containsKey("capturedImageBitMap")
                && extras.containsKey("capturedImagePath")) {
            capturedImageUri = extras.getParcelable("capturedImageUri");
            try {
                imageBitMap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), capturedImageUri);
//                imageBitMap = rotateImage(imageBitMap, 90);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            imageBitMap = (Bitmap) extras.getParcelable("capturedImageBitMap");
            topLeftImageBitMap = Bitmap.createBitmap(imageBitMap, 0, 0, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2);
            topLeftImageUri = getImageUri(this.getContentResolver(), topLeftImageBitMap, "topLeftImageBitMap");
            bottomLeftImageBitMap = Bitmap.createBitmap(imageBitMap, 0, imageBitMap.getHeight()/2, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2);
            bottomLeftImageUri = getImageUri(this.getContentResolver(), bottomLeftImageBitMap, "bottomLeftImageBitMap");
            topRightImageBitMap = Bitmap.createBitmap(imageBitMap, imageBitMap.getWidth()/2, 0, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2);
            topRightImageUri = getImageUri(this.getContentResolver(), topRightImageBitMap, "topRightImageBitMap");
            bottomRightImageBitMap = Bitmap.createBitmap(imageBitMap, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2);
            bottomRightImageUri = getImageUri(this.getContentResolver(), bottomRightImageBitMap, "bottomRightImageBitMap");
        }
        image1.setImageURI(topLeftImageUri);
        image2.setImageURI(topRightImageUri);
        image3.setImageURI(bottomLeftImageUri);
        image4.setImageURI(bottomRightImageUri);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                preparePostRequest();
            }
        });
//        capturedImage.setImageBitmap(topRightImageBitMap);
//        capturedImage.setImageURI(capturedImageUri);
        //Conversion of bitmap to byte array to send it to server

    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }


    private Uri getImageUri(ContentResolver contentResolver, Bitmap inImage, String title) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(contentResolver, inImage, title, null);
        return Uri.parse(path);
    }

    private void preparePostRequest(){
        progressBar.setVisibility(View.VISIBLE);
        String[] res = new String[4];
        String[] links = {"http://192.168.0.106:5000/","http://192.168.0.252:5000/","http://192.168.0.106:5000/","http://192.168.0.252:5000/"};
        res[0] = callServer(topLeftImageUri,links[0]);
        res[1] = callServer(bottomLeftImageUri,links[1]);
        res[2] = callServer(topRightImageUri,links[2]);
        res[3] = callServer(bottomRightImageUri,links[3]);
        float confidence=-1;
        int num = -1;
        for(String line:res)
        {
            float val = Float.parseFloat(line.split(",")[0]);
            if(val>confidence)
            {
                confidence=val;
                num = Integer.parseInt(line.split(",")[1]);
            }
        }
        imageCategory=String.valueOf(num);
        if(imageCategory != null){
            String text = "Predicted Value:- "+ imageCategory;
            description.setText(text);
            description.setTypeface(null, Typeface.BOLD);
        }
        progressBar.setVisibility(View.GONE);
        uploadButton.setVisibility(View.GONE);
        description.setVisibility(View.VISIBLE);
        storeImage(num);
    }

    private void storeImage(int num){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/MC/"+num+"/");
        if (!storageDir.exists())
            storageDir.mkdirs();
        try {
            File image = File.createTempFile(
                    timeStamp,                   /* prefix */
                    ".jpeg",                     /* suffix */
                    storageDir                   /* directory */
            );
            FileOutputStream out = new FileOutputStream(image);
            imageBitMap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String callServer(Uri ImageUri, String url){

        InputStream iStream;
        try {
            iStream = getContentResolver().openInputStream(ImageUri);
            byteArray = getBytes(iStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        OkHttpClient client = new OkHttpClient();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "Image_"+timeStamp+".jpeg";
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", filename,RequestBody.create(MediaType.parse("image/jpg"), byteArray))
                .build();

        Request request = new Request.Builder().url(url).post(formBody).build();

        Call call = client.newCall(request);
        Response response;
        try {
            response = call.execute();
            imageCategory = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Category Selected: "+ imageCategory);
//        testMethod(imageCategory);
        return imageCategory;
    }

    private void testMethod(String imageCategory){
        Toast.makeText(this, "Number Category:-  "+ imageCategory, Toast.LENGTH_SHORT).show();
    }


    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}