package com.ece420.lab7;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.TrackerKCF;
import java.io.*;
import java.util.Scanner;
import com.opencsv.CSVReader;
import org.opencv.ximgproc.Ximgproc;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    // UI Variables
    private Button controlButton;
    //private SeekBar colorSeekbar;
    //private SeekBar widthSeekbar;
    //private SeekBar heightSeekbar;
    private TextView widthTextview;
    private TextView heightTextview;

    // Declare OpenCV based camera view base
    private CameraBridgeViewBase mOpenCvCameraView;
    // Camera size
    private int myWidth;
    private int myHeight;

    // Mat to store RGBA and Grayscale camera preview frame
    private Mat mRgba;
    private Mat mGray;

    // KCF Tracker variables
    private TrackerKCF myTracker;
    private Rect2d myROI = new Rect2d(0,0,0,0);
    private int myROIWidth = 70;
    private int myROIHeight = 70;
    private Scalar myROIColor = new Scalar(0,0,0);
    private int tracking_flag = -1;

    private int size = 100;
    private int K = 9;
    private int num_images = 14;
    private Mat evects_K;
    private Mat avg_face;
    private Mat omega_i;
    double[][] evects = new double[K][size*size];
    double[][] w_i = new double[K][num_images];
    double[][] avg = new double[size][size];

    @Override
    protected void onCreate(Bundle savedInstanceState) {




        try{
            CSVReader reader = new CSVReader(new InputStreamReader(getResources().openRawResource(R.raw.evects_k)));//Specify asset file name
            String [] nextLine;
            double sum = 0;
            int count = 0;


            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                for(int i = 0; i < nextLine.length; i++){
                    sum += Double.parseDouble(nextLine[i]);
                    evects[count][i] = Double.parseDouble(nextLine[i]);

                }
                count++;

            }
            Log.d("SUM", String.valueOf(sum));

        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            CSVReader reader = new CSVReader(new InputStreamReader(getResources().openRawResource(R.raw.w)));//Specify asset file name
            String [] nextLine;
            double sum = 0;
            int count = 0;


            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                Log.d("COUNT_", String.valueOf(count));

                for(int i = 0; i < nextLine.length; i++){
                    sum += Double.parseDouble(nextLine[i]);
                    w_i[count][i] = Double.parseDouble(nextLine[i]);

                }
                count++;

            }
            Log.d("SUM_", String.valueOf(sum));

        }catch(Exception e){
            e.printStackTrace();
        }

        try{
            CSVReader reader = new CSVReader(new InputStreamReader(getResources().openRawResource(R.raw.avg_face)));//Specify asset file name
            String [] nextLine;
            double sum = 0;
            int count = 0;


            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                for(int i = 0; i < nextLine.length; i++){
                    sum += Double.parseDouble(nextLine[i]);
                    avg[count][i] = Double.parseDouble(nextLine[i]);
                    if(count == 0){
                        Log.d("avg_check", String.valueOf(nextLine[i]));

                    }

                }
                count++;

            }

            Log.d("SUM", String.valueOf(sum));


        }catch(Exception e){
            e.printStackTrace();
        }








        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
//        super.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Request User Permission on Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1);}

        // OpenCV Loader and Avoid using OpenCV Manager
        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }
        /*
        // Setup color seek bar
        colorSeekbar = (SeekBar) findViewById(R.id.colorSeekBar);
        colorSeekbar.setProgress(50);
        setColor(50);
        colorSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                setColor(progress);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Setup width seek bar
        widthTextview = (TextView) findViewById(R.id.widthTextView);
        widthSeekbar = (SeekBar) findViewById(R.id.widthSeekBar);
        widthSeekbar.setProgress(myROIWidth - 20);
        widthSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                // Only allow modification when not tracking
                if(tracking_flag == -1) {
                    myROIWidth = progress + 20;
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Setup width seek bar
        heightTextview = (TextView) findViewById(R.id.heightTextView);
        heightSeekbar = (SeekBar) findViewById(R.id.heightSeekBar);
        heightSeekbar.setProgress(myROIHeight - 20);
        heightSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                // Only allow modification when not tracking
                if(tracking_flag == -1) {
                    myROIHeight = progress + 20;
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        */
        // Setup control button
        controlButton = (Button)findViewById((R.id.controlButton));
        controlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tracking_flag == -1) {
                    // Modify UI
                    controlButton.setText("STOP");
//                    widthTextview.setVisibility(View.INVISIBLE);
                    //widthSeekbar.setVisibility(View.INVISIBLE);
//                    heightTextview.setVisibility(View.INVISIBLE);
                    //heightSeekbar.setVisibility(View.INVISIBLE);
                    // Modify tracking flag
                    tracking_flag = 1;
                }
                else if(tracking_flag == 1){
                    // Modify UI
                    controlButton.setText("START");
//                    widthTextview.setVisibility(View.VISIBLE);
                    //widthSeekbar.setVisibility(View.VISIBLE);
//                    heightTextview.setVisibility(View.VISIBLE);
                    //heightSeekbar.setVisibility(View.VISIBLE);
                    // Tear down myTracker
//                    myTracker.clear();
                    // Modify tracking flag
                    tracking_flag = -1;
                }
            }
        });

        // Setup OpenCV Camera View
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_camera_preview);
        // Use main camera with 0 or front camera with 1
        mOpenCvCameraView.setCameraIndex(1);
        // Force camera resolution, ignored since OpenCV automatically select best ones

//         mOpenCvCameraView.setMaxFrameSize(1280, 1280);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    // Helper Function to map single integer to color scalar
    // https://www.particleincell.com/2014/colormap/
    public void setColor(int value) {
        double a=(1-(double)value/100)/0.2;
        int X=(int)Math.floor(a);
        int Y=(int)Math.floor(255*(a-X));
        double newColor[] = {0,0,0};
        switch(X)
        {
            case 0:
                // r=255;g=Y;b=0;
                newColor[0] = 255;
                newColor[1] = Y;
                break;
            case 1:
                // r=255-Y;g=255;b=0
                newColor[0] = 255-Y;
                newColor[1] = 255;
                break;
            case 2:
                // r=0;g=255;b=Y
                newColor[1] = 255;
                newColor[2] = Y;
                break;
            case 3:
                // r=0;g=255-Y;b=255
                newColor[1] = 255-Y;
                newColor[2] = 255;
                break;
            case 4:
                // r=Y;g=0;b=255
                newColor[0] = Y;
                newColor[2] = 255;
                break;
            case 5:
                // r=255;g=0;b=255
                newColor[0] = 255;
                newColor[2] = 255;
                break;
        }
        myROIColor.set(newColor);
        return;
    }

    // OpenCV Camera Functionality Code
    @Override
    public void onCameraViewStarted(int width, int height) {
//        height = 1200;
        evects_K = new Mat(K,size*size, CvType.CV_8UC1);
        omega_i = new Mat(K,num_images, CvType.CV_8UC1);
        avg_face = new Mat(size,size, CvType.CV_8UC1);
        for(int row=0;row<K;row++){
            for(int col=0;col<size*size;col++)
                evects_K.put(row, col, evects[row][col]);
        }
        for(int row=0;row<K;row++){
            for(int col=0;col<num_images;col++)
                omega_i.put(row, col, w_i[row][col]);
        }
        for(int row=0;row<size;row++){
            for(int col=0;col<size;col++)
                avg_face.put(row, col, avg[row][col]);
        }


        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        myWidth = width;
        myHeight = height;
        myROI = new Rect2d(myWidth / 2 - myROIWidth / 2,
                            myHeight / 2 - myROIHeight / 2,
                            myROIWidth,
                            myROIHeight);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // Timer
        long start = Core.getTickCount();
        // Grab camera frame in rgba and grayscale format
        mRgba = inputFrame.rgba();


        Core.flip(mRgba, mRgba, 1); // this makes it so the camera is not inverted.
//        Mat mRgbaT = mRgba.t(); //need to transpose b/c the picture is actually landscape, but it is seen as portrait. (when we switched to portrait the picture looked squished)
//        Imgproc.resize(mRgbaT, mRgbaT,new Size(100,100));
//        Core.flip(mRgbaT, mRgbaT, 1);


        mGray = inputFrame.gray();
        Core.flip(mGray, mGray, 1); // this makes it so the camera is not inverted.
        Mat mGrayT = mGray.t(); //need to transpose b/c the picture is actually landscape, but it is seen as portrait. (when we switched to portrait the picture looked squished)
        Imgproc.resize(mGrayT, mGrayT,mGray.size());
        Core.flip(mGrayT, mGrayT, 1);


        //This is to check where (0,0) is on the screen, can remove whenever
        Imgproc.rectangle(mGrayT,
                new Point(0, 0),
                new Point(100, 400),
                myROIColor,
                4);


        double phi[] = new double[size*size];

        double[][] faces = new double[size][size];
        for(int i = 0;i < size;i++){
            for(int j = 0; j<size;j++){
                double val = mGrayT.get(i,j)[0];
                //faces[i][j] = val;
                phi[i*size + j] = val - avg[i][j];
            }
        }

        double omega[][] = new double[K][1];
        for(int i = 0;i < K;i++) {
            double val = 0;
            for (int j = 0; j < size * size; j++) {
                val += evects[i][j]*phi[j];
            }
            omega[i][0] = val;
        }

        double omega_norms[][] = new double[num_images][1];
        for(int i = 0;i < num_images;i++){
            double val = 0;
            for(int j = 0; j<K;j++){
                val += Math.pow(w_i[j][i] - omega[j][0],2);
            }
            omega_norms[i][0] = Math.sqrt(val);
        }
////        Log.d("length", String.valueOf(omega.length));
//        Log.d("length", "rows: " +String.valueOf(w_i.length));
//        Log.d("length", "cols: " +String.valueOf(w_i[0].length));

        double min_norm = omega_norms[0][0];
        for(int i = 0;i < num_images;i++){
            min_norm = Math.min(min_norm,omega_norms[i][0]);
        }
        Log.d("norm", "value: " +String.valueOf(min_norm));
        int detection_threshold = 3000;

        if (min_norm < detection_threshold) {
            Imgproc.putText(mRgba, "We found a face.", new Point(10, 30), Core.FONT_HERSHEY_SIMPLEX, 0.75, myROIColor);
        } else {
            Imgproc.putText(mRgba, "No face is found.",new Point(10,30),Core.FONT_HERSHEY_SIMPLEX, 0.75, myROIColor);
        }

        return mRgba;



//        Mat phi = new Mat(size,size,CvType.CV_8UC1);
//        Core.subtract(mGrayT,avg_face,phi);
        //We need to work with mRgbaT for the analysis
        //do calculation here: (just print "is a face" or "is not a face" for now, that's easy to get on the screen.
//        double omega[][];
//        double phi[] = new double[size*size];

//        for (int i = 0; i < size; i++ ) {
//            for (int j = 0; j <size; j++) {
//            phi[i*size + j] = (faces - avg);


//        for (int i = 0; i < evects_K.length; i++ ) {
//            for (int j = 0; j < phi[0].length; i++) {
//                omega[i][j] = evects_K[i][j]*phi[i][j];
//            }
//        }
//
//        double norms = 0.0;
//        for(int i=0;i<omega.length;i++) {
//            norms = norms + Math.pow((omega_i[i]-omega[i]),2.0);
//        }
//        norms = Math.sqrt(norms);
//
//        //Find the min index
//        int v = Integer.MAX_VALUE;
//        int ind = -1;
//        for (int i = 0; i < omega.length; i++) {
//            for (int j = 0; j < omega[0].length; j++) {
//                if (omega[i][j] < v) {
//                    v = omega[i][j];
//                    ind = i;
//            }
//        }
//
//        int detection_threshold = 8800;
//



//        // Grab camera frame in gray format
//        mGray = inputFrame.gray();
//
//        // Action based on tracking flag
//        if(tracking_flag == -1){
//            // Update myROI to keep the window to the center
//            myROI.x = myWidth / 2 - myROIWidth / 2;
//            myROI.y = myHeight / 2 - myROIHeight / 2;
//            myROI.width = myROIWidth;
//            myROI.height = myROIHeight;
//        }
//        else if(tracking_flag == 0){
//            // Initialize KCF Tracker and Start Tracking
//            // 1. Create a KCF Tracker
//            // 2. Initialize KCF Tracker with grayscale image and ROI
//            // 3. Modify tracking flag to start tracking
//            // ******************** START YOUR CODE HERE ******************** //
//            myTracker = TrackerKCF.create();
//            myTracker.init(mGray, myROI);
//            tracking_flag = 1;
//            // ******************** END YOUR CODE HERE ******************** //
//        }
//        else{
//            // Update tracking result is succeed
//            // If failed, print text "Tracking failure occurred!" at top left corner of the frame
//            // Calculate and display "FPS@fps_value" at top right corner of the frame
//            // ******************** START YOUR CODE HERE ******************** //
//            boolean check = myTracker.update(mGray, myROI);
//            if (!check) {
//                Imgproc.putText(mRgba, "Tracking Failure Occurred!",new Point(10,30),Core.FONT_HERSHEY_SIMPLEX, 0.75, myROIColor);
//            }
//            double fps = Core.getTickFrequency()/(Core.getTickCount()-start);
//            Imgproc.putText(mRgba, "FPS@"+fps, new Point(950,30), Core.FONT_HERSHEY_SIMPLEX, 0.75, myROIColor);
//            // ******************** END YOUR CODE HERE ******************** //
//        }

        // Draw a rectangle on to the current frame


        // Returned frame will be displayed on the screen





//        return mRgba;
    }
}