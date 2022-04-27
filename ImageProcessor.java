package com.example.extract_frames;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.SIFT;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ImageProcessor {

    public static Mat run(Mat a, Mat b) {


        Mat img1 = new Mat();
        Mat img2 = new Mat();
        Imgproc.cvtColor(a,img1,Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(b,img2,Imgproc.COLOR_BGR2GRAY);
        if (img1.empty() || img2.empty()) {
            System.err.println("Cannot read images!");
            System.exit(0);
        }
        //-- Step 1: Detect the keypoints using SIFT Detector, compute the descriptors
        double hessianThreshold = 400;
        int nOctaves = 4, nOctaveLayers = 3;
        boolean extended = false, upright = false;
        SIFT detector = SIFT.create(50, 3, 0.09);
        MatOfKeyPoint keypoints1 = new MatOfKeyPoint(), keypoints2 = new MatOfKeyPoint();
        Mat descriptors1 = new Mat(), descriptors2 = new Mat();
        detector.detectAndCompute(img1, new Mat(), keypoints1, descriptors1);
        detector.detectAndCompute(img2, new Mat(), keypoints2, descriptors2);

        //-- Step 2: Matching descriptor vectors with a FLANN based matcher
        // Since SURF is a floating-point descriptor NORM_L2 is used
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        List<MatOfDMatch> knnMatches = new ArrayList<>();
        matcher.knnMatch(descriptors1, descriptors2, knnMatches, 2);

        //-- Filter matches using the Lowe's ratio test
        float ratioThresh = 0.7f;
        List<DMatch> listOfGoodMatches = new ArrayList<>();
        for (int i = 0; i < knnMatches.size(); i++) {
            if (knnMatches.get(i).rows() > 1) {
                DMatch[] matches = knnMatches.get(i).toArray();
                if (matches[0].distance < ratioThresh * matches[1].distance) {
                    listOfGoodMatches.add(matches[0]);
                }
            }
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(listOfGoodMatches);
        //-- Draw matches
        Mat imgMatches = new Mat();
        Features2d.drawMatches(img1, keypoints1, img2, keypoints2, goodMatches, imgMatches, Scalar.all(-1),
                Scalar.all(-1), new MatOfByte(), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS);

        //-- Localize the object
        List<Point> obj = new ArrayList<>();
        List<Point> scene = new ArrayList<>();
        List<KeyPoint> listOfKeypointsObject = keypoints1.toList();
        List<KeyPoint> listOfKeypointsScene = keypoints2.toList();
        for (int i = 0; i < listOfGoodMatches.size(); i++) {
            //-- Get the keypoints from the good matches
            obj.add(listOfKeypointsObject.get(listOfGoodMatches.get(i).queryIdx).pt);
            scene.add(listOfKeypointsScene.get(listOfGoodMatches.get(i).trainIdx).pt);
        }

        if (obj.isEmpty() || scene.isEmpty()) {
            System.out.println("No matches found at all.....");

        }

        /*
        MatOfPoint2f objMat = new MatOfPoint2f(), sceneMat = new MatOfPoint2f();
        objMat.fromList(obj);
        sceneMat.fromList(scene);
        double ransacReprojThreshold = 3.0;
        Mat H = Calib3d.findHomography( objMat, sceneMat, Calib3d.RANSAC, ransacReprojThreshold );
        //-- Get the corners from the image_1 ( the object to be "detected" )
        Mat objCorners = new Mat(4, 1, CvType.CV_32FC2), sceneCorners = new Mat();
        float[] objCornersData = new float[(int) (objCorners.total() * objCorners.channels())];
        objCorners.get(0, 0, objCornersData);
        objCornersData[0] = 0;
        objCornersData[1] = 0;
        objCornersData[2] = img1.cols();
        objCornersData[3] = 0;
        objCornersData[4] = img1.cols();
        objCornersData[5] = img1.rows();
        objCornersData[6] = 0;
        objCornersData[7] = img1.rows();
        objCorners.put(0, 0, objCornersData);
        Core.perspectiveTransform(objCorners, sceneCorners, H);
        float[] sceneCornersData = new float[(int) (sceneCorners.total() * sceneCorners.channels())];
        sceneCorners.get(0, 0, sceneCornersData);
        //-- Draw lines between the corners (the mapped object in the scene - image_2 )
        Imgproc.line(imgMatches, new Point(sceneCornersData[0] + img1.cols(), sceneCornersData[1]),
                new Point(sceneCornersData[2] + img1.cols(), sceneCornersData[3]), new Scalar(0, 255, 0), 4);
        Imgproc.line(imgMatches, new Point(sceneCornersData[2] + img1.cols(), sceneCornersData[3]),
                new Point(sceneCornersData[4] + img1.cols(), sceneCornersData[5]), new Scalar(0, 255, 0), 4);
        Imgproc.line(imgMatches, new Point(sceneCornersData[4] + img1.cols(), sceneCornersData[5]),
                new Point(sceneCornersData[6] + img1.cols(), sceneCornersData[7]), new Scalar(0, 255, 0), 4);
        Imgproc.line(imgMatches, new Point(sceneCornersData[6] + img1.cols(), sceneCornersData[7]),
                new Point(sceneCornersData[0] + img1.cols(), sceneCornersData[1]), new Scalar(0, 255, 0), 4);

         */


        return imgMatches;

    }
}
