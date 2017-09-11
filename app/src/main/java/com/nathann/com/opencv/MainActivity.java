package com.nathann.com.opencv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends Activity {

    private final int SELECT_PHOTO = 1;
    private ImageView ivImage, ivImageProcessed;
    Mat src;
    static int ACTION_MODE = 0;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    //DO YOUR WORK/STUFF HERE
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        ivImage = (ImageView)findViewById(R.id.ivImage);
        ivImageProcessed = (ImageView)findViewById(R.id.ivImageProcessed);
        Intent intent = getIntent();

        if(intent.hasExtra("ACTION_MODE")){
            ACTION_MODE = intent.getIntExtra("ACTION_MODE", 0);
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_load_image) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        src = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(selectedImage, src);
                        switch (ACTION_MODE){
                            case HomeActivity.GAUSSIAN_BLUR:
                                // original values:  new Size(3,3) and 0
                                // 35,35 is very resource consuming but effective.
                                Imgproc.GaussianBlur(src, src,new Size(15,15),15,15,0);
                                break;
                            case HomeActivity.MEAN_BLUR:

                                // original 3,3 -  13,13 not bad too
                                Imgproc.blur(src, src, new Size(15,15));
                                break;
                            case HomeActivity.MEDIAN_BLUR:
                                Imgproc.medianBlur(src, src, 3);
                                break;
                            case HomeActivity.SHARPEN:

                                  //if there are too much white pixels in he image then i have to use weaker algorithm

//                                MatOfInt kernel = new MatOfInt(-1,-1,-1,-1,9,-1,-1,-1,-1);
//
//                                // this one sucks
//                                //MatOfInt kernel = new MatOfInt (-1,-1,-1,-1,-1,-1,2,2,2,-1,-1,2,8,2,-1,-1,2,2,2,-1,-1,-1,-1,-1,-1);
//
//                                // this one sucks too not as much as the previous one
//                                //MatOfInt kernel = new MatOfInt(1,1,1,1,-7,1,1,1,1);
//
//
//                                //filter2D(src, dst, depth , kernel, anchor, delta, BORDER_DEFAULT );
//
//                                Point anchor = new Point( -1, -1 );
//                                double delta = 0;
//
//                                //sharpening is strong so  i do GB before sharpening
//                                Imgproc.GaussianBlur(src, src,new Size(3,3),15,15,0);
//                                Imgproc.filter2D(src, src, src.depth(), kernel , anchor , delta, Imgproc.BORDER_DEFAULT  );


                                 // weaker algorithm

                                // relatively ok, mostly ineffective
                                //kernel.put(0, 0, 0, -1, 0, -1, 5, -1, 0, -1, 0);
                                // ultra strong
                                //kernel.put(0, 0, 0, -5, 0, -5, 25, -5, 0, -5, 0);

                                 // does not print the image
                                 //kernel.put( 0 , 0 , 0 , -3 , 0 , -3 , 7.5 , -3 , 0 , -3 , 0 );

                                 Mat kernel = new Mat(3,3,CvType.CV_32F);
                                 kernel.put(0, 0, 0, -1, 0, -1, 5, -1, 0, -1, 0);
                                 Imgproc.filter2D(src, src, src.depth(), kernel);
                                 //

                                break;
                            case HomeActivity.DILATE:
                                Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
                                Imgproc.dilate(src, src, kernelDilate);
                                break;
                            case HomeActivity.ERODE:
                                Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
                                Imgproc.erode(src, src, kernelErode);
                                break;
                            case HomeActivity.THRESHOLD:
                                Imgproc.threshold(src, src, 100, 255, Imgproc.THRESH_BINARY);
                                break;
                            case HomeActivity.ADAPTIVE_THRESHOLD:
                                Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
                                Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, 0);
                                break;
                        }

                        //dealing with OOM
                        System.gc();

                        Bitmap processedImage = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
                        Log.v("com.packtpub", CvType.typeToString(src.type())+"");
                        Utils.matToBitmap(src, processedImage);

                        ivImage.setImageBitmap(selectedImage);
                        ivImageProcessed.setImageBitmap(processedImage);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this,
                mOpenCVCallBack);
    }
}
