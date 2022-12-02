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
    private ImageView capturedImage;
    private TextView description;
    ProgressBar progressBar;
    String imageCategory;
    Uri capturedImageUri;
    byte[] byteArray;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_upload_image);
        capturedImage = findViewById(R.id.imageViewUpload);
        description = findViewById(R.id.descriptionText);
        progressBar = findViewById(R.id.progressBar);


        Bitmap imageBitMap = null;
        Bitmap topLeftImageBitMap = null, bottomLeftImageBitMap = null, topRightImageBitMap = null, bottomRightImageBitMap = null;
        Uri topLeftImageUri = null, bottomLeftImageUri = null, topRightImageUri = null, bottomRightImageUri = null;
        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.containsKey("capturedImageUri") && extras.containsKey("capturedImageBitMap")
                && extras.containsKey("capturedImagePath")) {
            capturedImageUri = extras.getParcelable("capturedImageUri");
            try {
                imageBitMap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), capturedImageUri);
                imageBitMap = rotateImage(imageBitMap, 90);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            imageBitMap = (Bitmap) extras.getParcelable("capturedImageBitMap");
            topLeftImageBitMap = Bitmap.createBitmap(imageBitMap, 0, 0, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2);
            topLeftImageBitMap=rotateImage(topLeftImageBitMap,-90);
            topLeftImageUri = getImageUri(this.getContentResolver(), topLeftImageBitMap, "topLeftImageBitMap");
            bottomLeftImageBitMap = Bitmap.createBitmap(imageBitMap, 0, imageBitMap.getHeight()/2, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2);
            bottomLeftImageBitMap=rotateImage(bottomLeftImageBitMap,-90);
            bottomLeftImageUri = getImageUri(this.getContentResolver(), bottomLeftImageBitMap, "bottomLeftImageBitMap");
            topRightImageBitMap = Bitmap.createBitmap(imageBitMap, imageBitMap.getWidth()/2, 0, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2);
            topRightImageBitMap=rotateImage(topRightImageBitMap,-90);
            topRightImageUri = getImageUri(this.getContentResolver(), topRightImageBitMap, "topRightImageBitMap");
            bottomRightImageBitMap = Bitmap.createBitmap(imageBitMap, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2, imageBitMap.getWidth()/2, imageBitMap.getHeight()/2);
            bottomRightImageBitMap=rotateImage(bottomRightImageBitMap,-90);
            bottomRightImageUri = getImageUri(this.getContentResolver(), bottomRightImageBitMap, "bottomRightImageBitMap");
        }



        String[] res = new String[4];
        String[] links = {"http://192.168.0.106:5000/","http://192.168.0.106:5000/","http://192.168.0.106:5000/","http://192.168.0.106:5000/"};

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

//        float confidence=-1;
//        int num = -1;
//        for(int i=0; i<2;i++)
//        {
//            res=callServer(links[i],i);
//            float val = Float.parseFloat(res.split(",")[0]);
//            if(val>confidence)
//            {
//                confidence=val;
//                num = Integer.parseInt(res.split(",")[1]);
//            }
//        }


        imageCategory=String.valueOf(num);


//        capturedImage.setImageBitmap(topRightImageBitMap);
//        capturedImage.setImageURI(capturedImageUri);
        //Conversion of bitmap to byte array to send it to server

//        InputStream iStream = null;
//        try {
//            iStream = getContentResolver().openInputStream(capturedImageUri);
//            byteArray = getBytes(iStream);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        callServer();
        progressBar.setVisibility(View.GONE);
        if(imageCategory != null){
            String text = "Predicted Value:- "+ imageCategory;
            description.setText(text);
            description.setTypeface(null, Typeface.BOLD);
        }
        description.setVisibility(View.VISIBLE);

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

    private String callServer(Uri ImageUri, String url){

        InputStream iStream = null;
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
        Response response = null;
        try {
            response = call.execute();
            imageCategory = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Category Selected: "+ imageCategory);
        capturedImage.setImageURI(ImageUri);
        testMethod(imageCategory);
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