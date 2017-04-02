package com.example.edithxd.facelate;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.*;
import java.util.Vector;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;
import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;

public class MainActivity extends AppCompatActivity {

    private final int PICK_IMAGE = 1;
    private final int CAMERA_PIC_REQUEST = 1000;

    //diallog xiaochuangkou
    private ProgressDialog detectionProgressDialog;
    private FaceServiceClient faceServiceClient =
            new FaceServiceRestClient("35af2f4e36c843488016a34cd4b9c6fc");
    private static Vector<FaceCoordinates> faceCoordinatesVector = new Vector<FaceCoordinates>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set UI -> activity.xml
        //R is referring to id -> layout folder -> activity_main
        setContentView(R.layout.activity_main);
        Button button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //choose pictur.e from gallary
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });
//        button2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
//            }
//        });
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

//        if (requestCode == CAMERA_PIC_REQUEST) {
//            Bitmap image = (Bitmap) data.getExtras().get("data");
//            ImageView imageView = (ImageView) findViewById(R.id.imageView1);
//            imageView.setImageBitmap(image);
//            detectAndFrame(image);
//        }
    }

    // Detect faces by uploading face images
// Frame faces after detection

    private void detectAndFrame(final Bitmap imageBitmap)
    {
        faceCoordinatesVector.clear();
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
                                    true,        // returnFaceLandmarks
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


        Vector <Path> facePaths = new Vector<>();

        if (faces != null) {
            for (Face face : faces) {

                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setAntiAlias(true);
                paint.setTextSize(60);

                FaceRectangle faceRectangle = face.faceRectangle;
                FaceAttribute faceAttributes = face.faceAttributes;
                canvas.drawText("Gender: "+faceAttributes.gender, faceRectangle.left,
                        faceRectangle.top+faceRectangle.height, paint);

                FaceCoordinates thisFaceCoordinate = new FaceCoordinates();

                thisFaceCoordinate.eyebrowLeftInnerX = face.faceLandmarks.eyebrowLeftInner.x;
                thisFaceCoordinate.eyebrowLeftInnerY = face.faceLandmarks.eyebrowLeftInner.y;
                thisFaceCoordinate.eyebrowRightInnerX = face.faceLandmarks.eyebrowRightInner.x;
                thisFaceCoordinate.eyebrowRightInnerY = face.faceLandmarks.eyebrowLeftInner.y;

                thisFaceCoordinate.eyebrowLeftOuterX = face.faceLandmarks.eyebrowLeftOuter.x;
                thisFaceCoordinate.eyebrowLeftOuterY = face.faceLandmarks.eyebrowLeftOuter.y;
                thisFaceCoordinate.eyebrowRightOuterX = face.faceLandmarks.eyebrowRightOuter.x;
                thisFaceCoordinate.eyebrowRightOuterY = face.faceLandmarks.eyebrowLeftOuter.y;

                thisFaceCoordinate.mouthLeftX = face.faceLandmarks.mouthLeft.x;
                thisFaceCoordinate.mouthLeftY = face.faceLandmarks.mouthLeft.y;
                thisFaceCoordinate.mouthRightX = face.faceLandmarks.mouthRight.x;
                thisFaceCoordinate.mouthRightY = face.faceLandmarks.mouthRight.y;

                thisFaceCoordinate.underLipBottomX = face.faceLandmarks.underLipBottom.x;
                thisFaceCoordinate.underLipBottomY = face.faceLandmarks.underLipBottom.y;

                // add facecoordinates, the index is face
                faceCoordinatesVector.add(thisFaceCoordinate);
                Path path = new Path();
                /*Chop out the face*/
                path.lineTo(Float.parseFloat(""+thisFaceCoordinate.eyebrowLeftOuterX ),
                        Float.parseFloat(""+thisFaceCoordinate.eyebrowLeftOuterY));
                float middleX = (Float.parseFloat(""+thisFaceCoordinate.eyebrowLeftOuterX)+
                        Float.parseFloat(""+thisFaceCoordinate.eyebrowLeftOuterY))/2;
                float topY =   Float.parseFloat(""+thisFaceCoordinate.eyebrowLeftOuterY)+20;
                path.lineTo(middleX,topY);
                path.lineTo(Float.parseFloat(""+thisFaceCoordinate.eyebrowRightOuterX ),
                        Float.parseFloat(""+thisFaceCoordinate.eyebrowRightOuterY));
                path.lineTo(Float.parseFloat(""+thisFaceCoordinate.mouthRightX ),
                        Float.parseFloat(""+thisFaceCoordinate.mouthRightY));
                path.lineTo(Float.parseFloat(""+thisFaceCoordinate.underLipBottomX ),
                        Float.parseFloat(""+thisFaceCoordinate.underLipBottomY));
                path.lineTo(Float.parseFloat(""+thisFaceCoordinate.mouthLeftX ),
                        Float.parseFloat(""+thisFaceCoordinate.mouthLeftY));
                path.lineTo(Float.parseFloat(""+thisFaceCoordinate.eyebrowLeftOuterX ),
                        Float.parseFloat(""+thisFaceCoordinate.eyebrowLeftOuterY));

                facePaths.add(path);
            }
        }

        if (facePaths.size()>1) {
            Path FacePath1 = new Path();
            FacePath1 = facePaths.get(0); //TO_DO

            int one_to_two_x = (int) (faceCoordinatesVector.get(1).eyebrowLeftOuterX - faceCoordinatesVector.get(0).eyebrowLeftOuterX);
            int one_to_two_y = (int) (faceCoordinatesVector.get(1).eyebrowLeftOuterY - faceCoordinatesVector.get(0).eyebrowLeftOuterY);

            FacePath1.setFillType(Path.FillType.INVERSE_EVEN_ODD);
            Bitmap cropped_bitmap_1 = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas cropped_canvas_1 = new Canvas(cropped_bitmap_1);
            paint.setColor(Color.BLACK);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
            paint.setAntiAlias(true);
            cropped_canvas_1.drawPath(FacePath1, paint);
            int[] all_old_pixels = new int[cropped_bitmap_1.getHeight() * cropped_bitmap_1.getWidth()];
            int[] all_background_pixels = new int[bitmap.getHeight() * bitmap.getWidth()];
            cropped_bitmap_1.getPixels(all_old_pixels, 0, cropped_bitmap_1.getWidth(), 0, 0, cropped_bitmap_1.getWidth(), cropped_bitmap_1.getHeight());
            bitmap.getPixels(all_background_pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            for (int i = 0; i < all_old_pixels.length; i++) {
                int oldLocation = (int) (i + one_to_two_y * bitmap.getWidth() + one_to_two_x);
                if (all_old_pixels[i] == Color.BLACK) {
                    if (oldLocation < all_background_pixels.length && oldLocation >= 0) {
                        all_old_pixels[i] = all_background_pixels[oldLocation];
                    }
                }
            }
            cropped_bitmap_1.setPixels(all_old_pixels, 0, cropped_bitmap_1.getWidth(), 0, 0, cropped_bitmap_1.getWidth(), cropped_bitmap_1.getHeight());

            canvas.drawBitmap(cropped_bitmap_1, one_to_two_x, one_to_two_y, null);

            Path FacePath2 = new Path();
            FacePath2 = facePaths.get(1); //TO_DO

            FacePath2.setFillType(Path.FillType.INVERSE_EVEN_ODD);
            Bitmap cropped_bitmap_2 = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas cropped_canvas_2 = new Canvas(cropped_bitmap_2);
            paint.setColor(Color.BLACK);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
            paint.setAntiAlias(true);
            cropped_canvas_2.drawPath(FacePath2, paint);
            int[] all_old_pixels_2 = new int[cropped_bitmap_2.getHeight() * cropped_bitmap_2.getWidth()];
            int[] all_background_pixels_2 = new int[bitmap.getHeight() * bitmap.getWidth()];
            cropped_bitmap_2.getPixels(all_old_pixels_2, 0, cropped_bitmap_2.getWidth(), 0, 0, cropped_bitmap_2.getWidth(), cropped_bitmap_2.getHeight());
            bitmap.getPixels(all_background_pixels_2, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            for (int i = 0; i < all_old_pixels_2.length; i++) {
                int oldLocation = (int) (i - one_to_two_y * bitmap.getWidth() - one_to_two_x);
                if (all_old_pixels_2[i] == Color.BLACK) {
                    if (oldLocation < all_background_pixels_2.length && oldLocation >= 0) {
                        all_old_pixels_2[i] = all_background_pixels_2[oldLocation];
                    }
                }
            }
            cropped_bitmap_2.setPixels(all_old_pixels_2, 0, cropped_bitmap_2.getWidth(), 0, 0, cropped_bitmap_2.getWidth(), cropped_bitmap_2.getHeight());

            //twoFaceSwap(cropped_bitmap_1,cropped_bitmap_2,bitmap,0,1);


            canvas.drawBitmap(cropped_bitmap_2, -one_to_two_x, -one_to_two_y, null);
            // Now cropped_bitmap is a transparent version

            canvas.translate(0, 200);
        }
        return bitmap;
    }

    private static void twoFaceSwap (Bitmap face1, Bitmap face2, Bitmap background,int p1, int p2){
        //face1 coordinate
        // p1 -> person1 p2 -> person2
         FaceCoordinates face1C =  faceCoordinatesVector.get(p1);
        FaceCoordinates face2C =  faceCoordinatesVector.get(p2);
        // 1 move to 2
        double x1 = face2C.eyebrowLeftOuterX - face1C.eyebrowLeftOuterX;
        double y1 = face2C.eyebrowLeftOuterY - face1C.eyebrowLeftOuterY;
        //2 move to 1
        double x2 = face1C.eyebrowLeftOuterX - face2C.eyebrowLeftOuterX;
        double y2 = face1C.eyebrowLeftOuterY - face2C.eyebrowLeftOuterY;

        movePic(face1,background,x1,y1);
        movePic(face2,background,x2,y2);

    }

    /*Move the picture to (current location +x , curr loc +y)*/
    private static void movePic(Bitmap face, Bitmap background,double x, double y){
        // Converting from black to alpha
        int [] all_old_pixels = new int[face.getHeight()*face.getWidth()];
        int [] all_background_pixels = new int[background.getHeight()*background.getWidth()];
        face.getPixels(all_old_pixels, 0, face.getWidth(), 0, 0, face.getWidth(), face.getHeight());
        background.getPixels(all_background_pixels, 0,  background.getWidth(), 0, 0, background.getWidth(), background.getHeight());
        for (int i = 0; i < all_old_pixels.length; i++)
        {
            int  oldLocation = (int)(i+y*background.getWidth()+x);
            if(all_old_pixels[i] == Color.BLACK) {
                if (oldLocation < all_background_pixels.length) {
                    all_old_pixels[i] = all_background_pixels[oldLocation];
                }
            }
        }
        face.setPixels(all_old_pixels, 0, face.getWidth(), 0, 0, face.getWidth(), face.getHeight());


    }
}
