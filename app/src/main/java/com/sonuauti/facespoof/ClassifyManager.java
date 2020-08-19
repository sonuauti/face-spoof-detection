package com.sonuauti.facespoof;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.SVM;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ClassifyManager  {

    interface ResultCallback{
        void onResult(boolean isFake);
    }

    private static ClassifyManager classifyManager;
    private SVM svm;

    private ClassifyManager(){ }

    public static synchronized ClassifyManager getInstance(){
        if (classifyManager==null)
            classifyManager=new ClassifyManager();

        return classifyManager;
    }

    public void setSvm(SVM svm) {
        this.svm = svm;
    }

    public SVM getSvm() {
        return svm;
    }

    CascadeClassifier cascadeClassifier;

    public void setCascadeClassifier(CascadeClassifier cascadeClassifier) {
        this.cascadeClassifier = cascadeClassifier;
    }

    public CascadeClassifier getCascadeClassifier() {
        return cascadeClassifier;
    }

    public boolean detectFaces(Mat graymat){

        if (getCascadeClassifier()==null) {
            CascadeClassifier cascadeClassifier = new CascadeClassifier();
            cascadeClassifier.load(Environment.getExternalStorageDirectory()+File.separator+"face_cascade.xml");
            setCascadeClassifier(cascadeClassifier);
        }

        MatOfRect faces=new MatOfRect();
        getCascadeClassifier().detectMultiScale(graymat,faces,1.1,2,2,new Size(200,200),new Size());

        if (faces!=null && faces.toArray().length>0) {
            //mask = faces.toArray()[0];
            //mask=rect;
            return true;
        }
        return false;
    }

    /**
     * Extract feature from the input gray scale image
     * @param graymat - gray mat
     * @return - output vector for classification
     */
    public double[] getFeatureVector(Mat graymat){

        /**
         * Count edges and calculate the standard deviation
         */
        Mat edges=new Mat();
        Imgproc.Canny(graymat,edges,100,200);

        MatOfDouble stdEdge=new MatOfDouble();
        MatOfDouble stdMean=new MatOfDouble();
        Core.meanStdDev(edges,stdMean,stdEdge);
        double edgeDev=stdEdge.get(0,0)[0];

        //find high freq
        Mat lapmat=new Mat();
        Imgproc.Laplacian(graymat,lapmat,3);
        MatOfDouble stdblur=new MatOfDouble();
        Core.meanStdDev(lapmat,new MatOfDouble(),stdblur);

        double blur = (stdblur.get(0,0)[0]);

        double[] featureVector = {edgeDev,blur};

        return featureVector;
    }

    /**
     * load the training model from file into memory
     * @return
     */
    public boolean loadModel(Context context,String fileName){

        loadLivenessLib(context,fileName);

        //load the model
        String filePath = Environment.getExternalStorageDirectory()+ File.separator+fileName;
        Log.d("liveness",filePath);
        if (new File(filePath).exists()) {

            //use this for fresh training
            //svm.train(samples, Ml.ROW_SAMPLE,labels);
            ClassifyManager.getInstance().setSvm(SVM.load(filePath));

            //save the training model
            //svm.save(myDir.getAbsolutePath()+"/"+"spoof.xml");

            //TEST setup
            /*Mat test = new Mat(1, 2, CvType.CV_32F);
            for (int i = 0; i < input.length; i++) {
                test.put(0, i, input[i]);
            }

            float r = ClassifyManager.getInstance().getSvm().predict(test); */
            //Log.d("SVM","Result of predict -> "+r);

           return true;
        }else {
            return true;
        }

    }

    private void loadLivenessLib(Context context, String fileName){
        try {
            String output = Environment.getExternalStorageDirectory()+File.separator+fileName;
            File output_file=new File(output);

            if (!output_file.exists()) {
                //load train file
                InputStream inputStream = context.getApplicationContext().getAssets().open(fileName);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output_file));
                byte[] bytesIn = new byte[4096];
                int read = 0;
                while ((read = inputStream.read(bytesIn)) != -1) {
                    bos.write(bytesIn, 0, read);
                }
                bos.close();
                inputStream.close();

                //load face_cascase.xml
                output = Environment.getExternalStorageDirectory()+File.separator+"face_cascade.xml";
                inputStream = context.getApplicationContext().getAssets().open("face_cascade.xml");
                bos = new BufferedOutputStream(new FileOutputStream(output));
                bytesIn = new byte[4096];
                read = 0;
                while ((read = inputStream.read(bytesIn)) != -1) {
                    bos.write(bytesIn, 0, read);
                }
                bos.close();
                inputStream.close();

            }

        }catch (Exception er){
           er.printStackTrace();
        }
    }

    /**
     * Classify the input features
     * @param input - feature vector to classify in double array
     * @return
     */
    public void classifiyInput(double[] input,ResultCallback resultCallback){

        if (getSvm()!=null) {
            Mat test=new Mat(1,2, CvType.CV_32F);

            for (int i=0;i<input.length;i++){
                test.put(0,i,input[i]);
            }

            float r=getSvm().predict(test);
            Log.d("SVM","Result of predict -> "+r);
            //binary classification, output either 1 or 0
            resultCallback.onResult(r>0);
            return;
        }
        resultCallback.onResult(false);
    }

}
