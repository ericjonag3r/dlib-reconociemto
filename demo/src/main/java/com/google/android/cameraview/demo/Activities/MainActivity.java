

package com.google.android.cameraview.demo.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.google.android.cameraview.demo.R;
import com.google.android.cameraview.demo.Utils.FaceRecognizer;
import com.tzutalin.dlib.VisionDetRet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MainActivity";
    private static final int INPUT_SIZE = 500;


    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_OFF,
            CameraView.FLASH_AUTO,
            CameraView.FLASH_ON,
    };




    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_on,
    };


    private static final int[] FLASH_TITLES = {
            R.string.flash_off,
            R.string.flash_auto,
            R.string.flash_on,
    };

    String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    long startTimeLocally;

    private int mCurrentFlash;
    private CameraView mCameraView;
    private Handler mBackgroundHandler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mCameraView = (CameraView) findViewById(R.id.camera);
        Button fab = (Button) findViewById(R.id.take_picture);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        checkPermissions();


        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }


        if (fab != null) {
            fab.setOnClickListener(mOnClickListener);
        }


        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }




    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);

            Bitmap bp = drawResizedBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
            new detectAsync().execute(bp);

        }

    };



    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.take_picture:
                    if (mCameraView != null) {
                        mCameraView.takePicture();
                    }
                    break;

            }
        }
    };






    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

        }

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause called");
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();


        if (mBackgroundHandler != null) {

            mBackgroundHandler.getLooper().quitSafely();
            mBackgroundHandler = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_flash:
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;

                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                return true;
            case R.id.switch_camera:
                if (mCameraView != null) {
                    int facing = mCameraView.getFacing();
                    mCameraView.setFacing(facing == CameraView.FACING_FRONT ?
                            CameraView.FACING_BACK : CameraView.FACING_FRONT);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getResultMessage(ArrayList<String> names) {
        String msg = "";
        if (names.isEmpty()) {
            msg = "No face detected or Unknown person";

        } else {
            for (int i = 0; i < names.size(); i++) {
                msg += names.get(i).split(Pattern.quote("."))[0];
                if (i != names.size() - 1) msg += ", ";
            }
            msg += " found!";
        }
        return msg;
    }




    Bitmap drawResizedBitmap(final Bitmap src) {

        final Bitmap dst = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

        Display getOrient = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        Point point = new Point();
        getOrient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
        Log.d(TAG, String.format("screen size (%d,%d)", screen_width, screen_height));
        if (screen_width < screen_height) {
            orientation = Configuration.ORIENTATION_PORTRAIT;
            // mScreenRotation = 0;
        } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
            //   mScreenRotation = 0;
        }


        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();


        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);


        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
        return dst;

    }

    private class detectAsync extends AsyncTask<Bitmap, Void, String> {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        Bitmap sourceBitmap;

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Detecting face...");
            dialog.setCancelable(false);
            dialog.show();
            super.onPreExecute();
        }

        protected String doInBackground(Bitmap... bp) {

            List<VisionDetRet> results;
            sourceBitmap = bp[0];

            startTimeLocally = System.currentTimeMillis();
            results = FaceRecognizer.getInstance().detect(sourceBitmap);
            String msg = null;
            if (results.size() == 0) {
                msg = "No face was detected or face was too small. Please select a different image";
            } else if (results.size() > 1) {
                msg = "More than one face was detected. Please select a different image";
            } else {
                return null;
            }
            return msg;
        }

        protected void onPostExecute(String result) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                if (result != null) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage(result);
                    builder1.setCancelable(true);
                    AlertDialog alert11 = builder1.create();
                    alert11.show();

                } else {
                    if (sourceBitmap != null) {
                        new recognizeAsync().execute(sourceBitmap);
                    }

                }

            }

        }

    }

    private class recognizeAsync extends AsyncTask<Bitmap, Void, ArrayList<String>> {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        Handler handler = new Handler();
        private int mScreenRotation = 0;

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Recognizing...");
            dialog.setCancelable(false);
            dialog.show();
            super.onPreExecute();
        }

        protected ArrayList<String> doInBackground(Bitmap... bp) {

            if (bp[0] != null) {


                List<VisionDetRet> results;

                results = FaceRecognizer.getInstance().recognize(bp[0]);

                final long endTime = System.currentTimeMillis();
                Log.d(TAG, "Time cost: " + (endTime - startTimeLocally) / 1000f + " sec");


                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Time cost: " + (endTime - startTimeLocally) / 1000f + " sec", Toast.LENGTH_LONG).show();
                    }
                });


                ArrayList<String> names = new ArrayList<>();
                for (VisionDetRet n : results) {
                    String getLabelStr = n.getLabel();
                  //  getLabelStr = getLabelStr.replaceAll("[0-9]", "");
                    names.add(getLabelStr);
                }

                HashSet<String> hashSet = new HashSet<String>();
                hashSet.addAll(names);
                names.clear();
                names.addAll(hashSet);


                return names;
            } else {
                Toast.makeText(getApplicationContext(), "Bitmap is null", Toast.LENGTH_LONG).show();
                return null;
            }


        }

        protected void onPostExecute(ArrayList<String> names) {

            if (names != null) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage(getResultMessage(names));
                    builder1.setCancelable(true);
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_LONG).show();
            }


        }

    }



}


