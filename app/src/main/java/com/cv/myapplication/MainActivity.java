package com.cv.myapplication;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    /**
     * CV相机
     */
    private CameraBridgeViewBase mCVCamera;
    /**
     * 加载OpenCV的回调
     */
    private BaseLoaderCallback mLoaderCallback;
    private ImageView image_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        image_view = findViewById(R.id.image_view);
        // 初始化CV相机
        mCVCamera = findViewById(R.id.camera_view);
        mCVCamera.setVisibility(CameraBridgeViewBase.VISIBLE);
        // 设置相机监听
        mCVCamera.setCvCameraViewListener(this);

        // 连接到OpenCV的回调
        mLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        mCVCamera.enableView();
                        break;
                    default:
                        break;
                }
            }
        };

    }

    private void initDabug() {
        if (OpenCVLoader.initDebug()) {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Arrays.asList(mCVCamera);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // 获取相机中的图像
        Mat rgba = inputFrame.rgba();
        Core.rotate(rgba, rgba, Core.ROTATE_90_CLOCKWISE);
        Bitmap bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgba, bitmap);
        Bitmap location = location(bitmap);
        if (location==null){
            return null;
        }
        runOnUiThread(() -> image_view.setImageBitmap(location));
        return null;
    }

    private Bitmap location(Bitmap bmp) {
        Mat originMat=new Mat();
        Utils.bitmapToMat(bmp,originMat);
        Mat resultG = new Mat();
        Mat result = new Mat();
        Imgproc.GaussianBlur(originMat, resultG, new Size(3.0, 3.0), 0);
        Imgproc.Canny(resultG, result, 100.0, 220.0, 3);
        // 膨胀，连接边缘
        Imgproc.dilate(result, result, new Mat(), new Point(-1,-1), 4, 1, new Scalar(1));
//        Bitmap Bmp = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(result,Bmp);

        List<MatOfPoint> contours  = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(result, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        //有了轮廓之后，为了方便我们先将轮廓给画出来，这里的resultMat其实就是srcMat，为了区分用了Mat resultMat = srcMat.clone();，
        // 下面代码的意思是，把contours轮廓列表中的所有轮廓(-1表示所有轮廓)在，resultMat上画用黑色(new Scalar(0, 0, 0))粗细为10的线条画出来。
        if (contours.isEmpty()){
            return null;
        }
        Mat resultMat = resultG.clone();

        double arcLength=0;
        int index=0;
        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint2f source = new MatOfPoint2f();
            source.fromList(contours.get(i).toList());
            if (Imgproc.arcLength(source, true)>arcLength){
                arcLength=Imgproc.arcLength(source, true);
                index=i;
            }

        }
        MatOfPoint matOfPoint = contours.get(index);
        MatOfPoint2f tempMat=new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(matOfPoint.toArray()), tempMat, Imgproc.arcLength(new MatOfPoint2f(matOfPoint.toArray()), true)*0.04, true);
        Point[] points = tempMat.toArray();
        if (points.length!=4){
            return null;
        }
        List<MatOfPoint> matOfPoints  = new ArrayList<>();
        matOfPoints.add(new MatOfPoint(tempMat.toArray()));

        Imgproc.drawContours(resultMat, matOfPoints, -1, new Scalar(0, 0, 255), 4);

        Bitmap resultBmp = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultMat,resultBmp);

        return resultBmp;

    }
    @Override
    protected void onResume() {
        // 界面加载完成的时候向OpenCV的连接回调发送连接成功的信号
        initDabug();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 销毁OpenCV相机
        if (mCVCamera != null)
            mCVCamera.disableView();
    }
}