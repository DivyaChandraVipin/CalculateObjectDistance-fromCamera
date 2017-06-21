package dv.camera.com.cameraobjectdistance;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.Arrays;

import java.util.Date;


import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.PixelFormat;
import android.hardware.camera2.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.util.Size;
public class MainActivity extends AppCompatActivity implements  SensorEventListener
{
        private TextView txt1;
        private TextureView textureView;
        SensorManager mSensorManager;
        Sensor mAccelerometer;
        Sensor mMagnetometer;
        private static final String TAG = "MeasureApp";
        private String[] units = { "meters", "cms", "feet", "inches"};
        private double AngleA = 0.0;
        private double AngleB = 0.0;
        private double X1 = 0.0;
        private double X2 = 0.0;
        private float[] mGravity;
        private float[] mMagnetic;
        private double h;
        private double D;
        private double H;
        private double L;
        private Size previewsize;
        private Size jpegSizes[]=null;

        private CaptureRequest.Builder previewBuilder;
        private CameraCaptureSession previewSession;
        float[] value = new float[3];
        private int unit;//default unit in mts
        //mts-0,cms-1,ft-2,in-3
        private Spinner spinner;
        DecimalFormat ThreeDForm = new DecimalFormat("#.###");
        float pressure;
        float accel[] =  new float[3];
        float result[] = new float[3];
        CharSequence test = "Results:\nObj Distance = "+D+"";
        private PowerManager.WakeLock wl;
        private PowerManager pm;
        Toast toast;
        private CameraDevice cameraDevice;
        final String filename = "Measures.txt";
        String m = "";
        CameraManager manager;
        String filepath = "MeasureStorage";
        File myInternalFile;
        File directory;
        private static final int CAMERA_PERMISSION = 1;


        private static final SparseIntArray ORIENTATIONS=new SparseIntArray();

        static
        {
                ORIENTATIONS.append(Surface.ROTATION_0,90);
                ORIENTATIONS.append(Surface.ROTATION_90,0);
                ORIENTATIONS.append(Surface.ROTATION_180,270);
                ORIENTATIONS.append(Surface.ROTATION_270,180);
        }
        @Override
        public void onCreate(Bundle icicle)
        {
                Log.d(TAG, "onCreate");
                super.onCreate(icicle);
                Log.e(getClass().getSimpleName(), "onCreate");
                getWindow().setFormat(PixelFormat.TRANSLUCENT);
                setContentView(R.layout.frames);

                textureView=(TextureView)findViewById(R.id.textureview);
                textureView.setSurfaceTextureListener(surfaceTextureListener);//main or frames
                final Button length = (Button) findViewById(R.id.button4);
                mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                final Button distance = (Button) findViewById(R.id.button1);
                final Button height = (Button) findViewById(R.id.button7);
                final Button rst_valider = (Button) findViewById(R.id.button2);
                txt1 = (TextView) findViewById(R.id.textView1);
                final Button adjh = (Button) findViewById(R.id.button3);
                final EditText edit = (EditText) findViewById(R.id.editText1);


                distance.setEnabled(false);
                height.setEnabled(false);
                length.setEnabled(false);
                edit.setText("0.0");
                unit = 0;
                txt1.setText(test);


                ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
                final File directory = contextWrapper.getDir(filepath, Context.MODE_APPEND);
                myInternalFile = new File(directory , filename);

                try
                {
                        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,TAG);
                        wl.acquire();
                }
                catch (Exception ex)
                {
                        Log.e("exception", "here 1");
                }


                addListenerOnButton();
                addListenerOnSpinnerItemSelection();

                distance.setOnClickListener(new View.OnClickListener()
                {
                        public void onClick(View view)
                        {
                                //old method
                                AngleA = getDirection();//taking x value
                                AngleA = Math.toRadians(90)-AngleA;
                                D = Double.valueOf(ThreeDForm.format(Math.abs(h*(Math.tan((AngleA))))));
                                test = "Results:Obj Distance = "+D+"Obj Height = "+H+"Obj Length = "+L+"Cam height =" +
                                        " "+h+""+units[unit];
                                toast = Toast.makeText(getApplicationContext(), "Object distance calculated!", Toast.LENGTH_SHORT);
                                toast.show();
                                txt1.setText(test);
                        }
                });




                length.setOnClickListener(new View.OnClickListener()
                {
                        @Override
                        public void onClick(View v)
                        {
                                // TODO Auto-generated method stub
                                if(X1==0.0)
                                {
                                        X1 = value[0];//taking z

                                }
                                else
                                {
                                        X2 = (value[0]);//taking z

                                        float theta = (float) Math.abs(Math.abs(X1)-Math.abs(X2));
                                        //arc of a circle logic;
                                        L = Double.valueOf(ThreeDForm.format(theta * D));
                                        test = "Results:Obj Distance = "+D+"Obj Height = "+H+"Obj Length = "+L+"Cam height =" +
                                                " "+h+""+units[unit];
                                        toast = Toast.makeText(getApplicationContext(), "Object length calculated!", Toast.LENGTH_SHORT);
                                        toast.show();
                                        txt1.setText(test);
                                }
                        }
                });

                adjh.setOnClickListener(new View.OnClickListener()
                {
                        public void onClick(View view)
                        {
                                h = Double.parseDouble(edit.getText().toString());
                                if(h==0)
                                {
                                        toast = Toast.makeText(getApplicationContext(), "Height must be more than 0!", Toast.LENGTH_SHORT);
                                        toast.show();
                                }
                                else
                                {
                                        txt1.setText("Camera height = "+h+"\n"+units[unit]);
                                        distance.setEnabled(true);
                                        height.setEnabled(true);
                                        length.setEnabled(true);
                                        toast = Toast.makeText(getApplicationContext(), "Phone height adjusted!", Toast.LENGTH_SHORT);
                                        toast.show();
                                }
                        }
                });

                height.setOnClickListener(new View.OnClickListener()
                {
                        @Override
                        public void onClick(View v)
                        {
                                AngleB=0;
                                while(AngleB==0)
                                {
                                        AngleB = getDirection();//taking x
                                }
                                H = Double.valueOf(ThreeDForm.format(h+Math.abs(D*Math.tan((AngleB)))));

                                test = "Results:Obj Distance = "+D+"Obj Height = "+H+"Obj Length = "+L+"Cam height = "+h+""+units[unit];
                                toast = Toast.makeText(getApplicationContext(), "Object height calculated!", Toast.LENGTH_SHORT);
                                toast.show();
                                txt1.setText(test);
                        }
                });


                rst_valider.setOnClickListener(new View.OnClickListener()
                {
                        public void onClick(View view)
                        {
                                AngleA = 0.0;
                                AngleB = 0.0;
                                X1 = 0.0;
                                X2 = 0.0;
                                D=0.0;
                                H=0.0;
                                L=0.0;
                                toast = Toast.makeText(getApplicationContext(), "Values reset!", Toast.LENGTH_SHORT);
                                toast.show();
                                txt1.setText("Results:\nObj Distance = "+D+"\nObj Length = "+L+"");
                        }
                });







        }
        private CameraDevice.StateCallback stateCallback=new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                        cameraDevice=camera;

                        startCamera();
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                camera.close();
                }

                @Override
                public void onError(CameraDevice camera, int error) {

                }
        };
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                switch (requestCode){
                        case  CAMERA_PERMISSION:
                                if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                                        Toast.makeText(this, "Cannot run application because camera service permission have not been granted", Toast.LENGTH_SHORT).show();
                                }
                                break;
                        default:
                                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                                break;
                }
        }
        public  void openCamera()
        {
                manager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
                try
                {
                        String camerId=manager.getCameraIdList()[0];
                        CameraCharacteristics characteristics=manager.getCameraCharacteristics(camerId);
                        StreamConfigurationMap map=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        previewsize=map.getOutputSizes(SurfaceTexture.class)[0];
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
                        }
                        else {
                                manager.openCamera(camerId, stateCallback, null);
                        }
                }catch (Exception e)
                {

                }
        }

        private TextureView.SurfaceTextureListener surfaceTextureListener=new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                        openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                        return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                }
        };

        public void addListenerOnSpinnerItemSelection()
        {
                spinner = (Spinner) findViewById(R.id.spinner1);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.units_array, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                spinner.setOnItemSelectedListener(new CustomOnItemSelectedListener()
                {
                        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
                        {
                                // TODO Auto-generated method stub
                                Toast.makeText(arg0.getContext(), "Unit selected : " + arg0.getItemAtPosition(arg2).toString(),Toast.LENGTH_SHORT).show();

                                if(arg2==0)
                                {
                                        //convert to mts
                                        if(unit==1)
                                        {//cm-m
                                               h = Double.valueOf(ThreeDForm.format(h*0.01));
                                                H = Double.valueOf(ThreeDForm.format(H*0.01));
                                                D = Double.valueOf(ThreeDForm.format(D*0.01));
                                                L = Double.valueOf(ThreeDForm.format(L*0.01));
                                        }
                                        else if(unit==2)
                                        {//ft-m
                                                h = Double.valueOf(ThreeDForm.format(h*.3048));
                                                H = Double.valueOf(ThreeDForm.format(H*.3048));
                                                D = Double.valueOf(ThreeDForm.format(D*.3048));
                                                L = Double.valueOf(ThreeDForm.format(L*.3048));
                                        }
                                        else if(unit==3)
                                        {//in-m
                                                h = Double.valueOf(ThreeDForm.format(h*.0254));
                                                H = Double.valueOf(ThreeDForm.format(H*.0254));
                                                D = Double.valueOf(ThreeDForm.format(D*.0254));
                                                L = Double.valueOf(ThreeDForm.format(L*.0254));
                                        }
                                        unit=0;
                                        test = "Results:Obj Distance = "+D+"Obj Height = "+H+"Obj Length = "+L+"Cam height = "+h+"\n"+units[unit];
                                        txt1.setText(test);
                                }
                                else if(arg2==1)
                                {//convert to cms
                                        if(unit==0)
                                        {//m-cm
                                                h = Double.valueOf(ThreeDForm.format(h*100));
                                                H = Double.valueOf(ThreeDForm.format(H*100));
                                                D = Double.valueOf(ThreeDForm.format(D*100));
                                                L = Double.valueOf(ThreeDForm.format(L*100));
                                        }
                                        else if(unit==2)
                                        {//ft-cm
                                                h = Double.valueOf(ThreeDForm.format(h*30.48));
                                                H = Double.valueOf(ThreeDForm.format(H*30.48));
                                                D = Double.valueOf(ThreeDForm.format(D*30.48));
                                                L = Double.valueOf(ThreeDForm.format(L*30.48));
                                        }
                                        else if(unit==3)
                                        {//in-cm
                                                h = Double.valueOf(ThreeDForm.format(h*2.54));
                                                H = Double.valueOf(ThreeDForm.format(H*2.54));
                                                D = Double.valueOf(ThreeDForm.format(D*2.54));
                                                L = Double.valueOf(ThreeDForm.format(L*2.54));
                                        }
                                        unit=1;
                                        test = "Results:Obj Distance = "+D+"Obj Height = "+H+"Obj Length = "+L+"Cam height =" +
                                                " "+h+""+units[unit];
                                        txt1.setText(test);
                                }
                                else if(arg2==2)
                                {//convert to feet
                                        if(unit==0)
                                        {//m-ft
                                                h = Double.valueOf(ThreeDForm.format(h*3.28084));
                                                H = Double.valueOf(ThreeDForm.format(H*3.28084));
                                                D = Double.valueOf(ThreeDForm.format(D*3.28084));
                                                L = Double.valueOf(ThreeDForm.format(L*3.28084));
                                        }
                                        else if(unit==1)
                                        {//cm-ft
                                                h = Double.valueOf(ThreeDForm.format(h*0.0328084));
                                                H = Double.valueOf(ThreeDForm.format(H*0.0328084));
                                                D = Double.valueOf(ThreeDForm.format(D*0.0328084));
                                                L = Double.valueOf(ThreeDForm.format(L*0.0328084));
                                        }
                                        else if(unit==3)
                                        {//in-ft
                                                h = Double.valueOf(ThreeDForm.format(h*0.0833333));
                                                H = Double.valueOf(ThreeDForm.format(H*0.0833333));
                                                D = Double.valueOf(ThreeDForm.format(D*0.0833333));
                                                L = Double.valueOf(ThreeDForm.format(L*0.0833333));
                                        }
                                        unit=2;
                                        test = "Results:Obj Distance = "+D+"Obj Height = "+H+"Obj Length = "+L+"Cam height =" +
                                                " "+h+""+units[unit];
                                        txt1.setText(test);
                                }
                                else
                                {//convert to in
                                        if(unit==0)
                                        {//m-in
                                                h = Double.valueOf(ThreeDForm.format(h*39.3701));
                                                H = Double.valueOf(ThreeDForm.format(H*39.3701));
                                                D = Double.valueOf(ThreeDForm.format(D*39.3701));
                                                L = Double.valueOf(ThreeDForm.format(L*39.3701));
                                        }
                                        else if(unit==1)
                                        {//cm-in
                                                h = Double.valueOf(ThreeDForm.format(h*0.393701));
                                                H = Double.valueOf(ThreeDForm.format(H*0.393701));
                                                D = Double.valueOf(ThreeDForm.format(D*0.393701));
                                                L = Double.valueOf(ThreeDForm.format(L*0.393701));
                                        }
                                        else if(unit==2)
                                        {//ft-in
                                                h = Double.valueOf(ThreeDForm.format(h*12));
                                                H = Double.valueOf(ThreeDForm.format(H*12));
                                                D = Double.valueOf(ThreeDForm.format(D*12));
                                                L = Double.valueOf(ThreeDForm.format(L*12));
                                        }
                                        unit=3;
                                        test = "Results:Obj Distance = "+D+"Obj Height = "+H+"Obj Length = "+L+"Cam height =" +
                                                " "+h+""+units[unit];
                                        txt1.setText(test);
                                }
                        }


                        public void onNothingSelected(AdapterView<?> arg0)
                        {
                                // TODO Auto-generated method stub
                        }
                });
        }

        public void addListenerOnButton()
        {
                spinner = (Spinner) findViewById(R.id.spinner1);
        }

        @Override
        public void onBackPressed()
        {
                onCreate(null);
        }



        @Override
        protected void onRestoreInstanceState(Bundle savedInstanceState)
        {
                super.onRestoreInstanceState(savedInstanceState);
        }

        @Override
        protected void onResume()
        {
                try
                {
                        Log.v("On resume called","------ wl aquire next!");
                        wl.acquire();
                }
                catch(Exception ex)
                {
                }
                Log.e(getClass().getSimpleName(), "onResume");
                super.onResume();
                //
                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
                mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
                //
        }

        @Override
        protected void onSaveInstanceState(Bundle outState)
        {
                super.onSaveInstanceState(outState);
        }

        @Override
        protected void onPause()
        {
                try
                {
                        Log.v("on pause called", "on pause called");
                        wl.release();
                        if(cameraDevice!=null)
                        {
                                cameraDevice.close();

                        }
                }
                catch(Exception ex)
                {
                        Log.e("Exception in on menu", "exception on menu");
                }
                super.onPause();
                mSensorManager.unregisterListener(this);
        }



        @Override
        protected void onUserLeaveHint()
        {
                try
                {

                        wl.release();
                }
                catch(Exception ex)
                {
                        Log.e("Exception in on menu", "exception on menu");
                }
                super.onUserLeaveHint();
        }

        public void onSensorChanged(SensorEvent event)
        {

                switch(event.sensor.getType())
                {
                        case Sensor.TYPE_ACCELEROMETER:
                                mGravity = event.values.clone();

                                break;
                        case Sensor.TYPE_MAGNETIC_FIELD:
                                mMagnetic = event.values.clone();
                                break;
                        case Sensor.TYPE_PRESSURE:
                                pressure = event.values[0];
                                pressure = pressure*100;
                        default:
                                return;
                }
                if(mGravity != null && mMagnetic != null)
                {
                        getDirection();
                }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy)
        {
                // TODO Auto-generated method stub

        }

        private float getDirection()//For calculating distance
        {
                float[] temp = new float[9];
                float[] R = new float[9];

                //Load rotation matrix into R
                SensorManager.getRotationMatrix(temp, null, mGravity, mMagnetic);

                //Remap to camera's point-of-view
                SensorManager.remapCoordinateSystem(temp, SensorManager.AXIS_X, SensorManager.AXIS_Z, R);

                //Return the orientation values

                SensorManager.getOrientation(R, value);

                //value[0] - Z, value[1]-X, value[2]-Y in radians

                return value[1];       //return x
        }


        void  startCamera()
        {
                if(cameraDevice==null||!textureView.isAvailable()|| previewsize==null)
                {
                        return;
                }

                SurfaceTexture texture=textureView.getSurfaceTexture();
                if(texture==null)
                {
                        return;
                }

                texture.setDefaultBufferSize(previewsize.getWidth(),previewsize.getHeight());
                Surface surface=new Surface(texture);

                try
                {
                        previewBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                }catch (Exception e)
                {
                }
                previewBuilder.addTarget(surface);
                try
                {
                        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(CameraCaptureSession session) {
                                        previewSession=session;
                                        getChangedPreview();
                                }

                                @Override
                                public void onConfigureFailed(CameraCaptureSession session) {

                                }
                        },null);
                }catch (Exception e)
                {

                }
        }
        void getChangedPreview()
        {
                if(cameraDevice==null)
                {
                        return;
                }
                previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                HandlerThread thread=new HandlerThread("changed Preview");
                thread.start();
                Handler handler=new Handler(thread.getLooper());
                try
                {
                        previewSession.setRepeatingRequest(previewBuilder.build(), null, handler);
                }catch (Exception e){}
        }


}