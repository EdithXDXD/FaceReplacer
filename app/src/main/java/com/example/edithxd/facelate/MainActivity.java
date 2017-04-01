package com.example.edithxd.facelate;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.*;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

public class MainActivity extends AppCompatActivity {

    private final int PICK_IMAGE = 1;
    //diallog xiaochuangkou
    private ProgressDialog detectionProgressDialog;
    private FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("35af2f4e36c843488016a34cd4b9c6fc");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set UI -> activity.xml
        //R is referring to id -> layout folder -> activity_main
        setContentView(R.layout.activity_main);
        //findViewById extract view object -> downcast
        Button button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //choose picture from gallary
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });
    //context as argument
        detectionProgressDialog = new ProgressDialog(this);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ImageView imageView = (ImageView) findViewById(R.id.imageView1);
                imageView.setImageBitmap(bitmap);
//added
                detectAndFrame(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Detect faces by uploading face images
// Frame faces after detection

    private void detectAndFrame(final Bitmap imageBitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            java.lang.String random = "age";
                            publishProgress("Detecting...");
                         FaceServiceClient.FaceAttributeType[] attributess = {FaceServiceClient.FaceAttributeType.Age, FaceServiceClient.FaceAttributeType.Gender};
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    attributess      // returnFaceAttributes: a string like "age, gender"
                            );
                            if (result == null)
                            {
                                publishProgress("Detection Finished. Nothing detected");
                                return null;
                            }
                            publishProgress(
                                    String.format("Detection Finished. %d face(s) detected",
                                            result.length));
                            return result;
                        } catch (Exception e) {
                            publishProgress("Detection failed");
                            return null;
                        }
                    }
                    @Override
                    protected void onPreExecute() {

                        detectionProgressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {

                        detectionProgressDialog.setMessage(progress[0]);
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {

                        detectionProgressDialog.dismiss();
                        if (result == null) return;
                        ImageView imageView = (ImageView)findViewById(R.id.imageView1);
                        imageView.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap, result));
                        imageBitmap.recycle();
                    }
                };
        detectTask.execute(inputStream);
    }

    private static Bitmap drawFaceRectanglesOnBitmap(Bitmap originalBitmap, Face[] faces) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);


        int stokeWidth = 4;
        paint.setStrokeWidth(stokeWidth);
        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
            }
        }
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setTextSize(60);

        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                FaceAttribute faceAttributes = face.faceAttributes;

                canvas.drawText("Age: "+faceAttributes.age, faceRectangle.left,
                        faceRectangle.top, paint);
            }
        }

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setTextSize(60);

        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                FaceAttribute faceAttributes = face.faceAttributes;

                canvas.drawText("Gender: "+faceAttributes.gender, faceRectangle.left,
                        faceRectangle.top+faceRectangle.height, paint);
            }
        }

        canvas.translate(0, 200);                 

        return bitmap;
    }
}