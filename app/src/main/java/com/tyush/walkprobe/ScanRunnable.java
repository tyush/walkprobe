package com.tyush.walkprobe;

import android.media.Image;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.SessionPausedException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class ScanRunnable implements Runnable {
    private ScannerActivity activity;
    private Handler recurser;
    private int cameraTextureName;
    private Session session;
    public short lastDepth = 0;

    public ScanRunnable(ScannerActivity activity, Session session, Handler recurser, int cameraTextureName) {
        this.activity = activity;
        this.recurser = recurser;
        this.session = session;
        this.cameraTextureName = cameraTextureName;
    }

    @Override
    public void run() {
        Frame frame;
        Image depthImage = null;
        Vibrator vibrator = activity.getSystemService(Vibrator.class);
        try {
            Log.println(Log.INFO, "scan", "grabbing frame");
            frame = session.update();
            depthImage = frame.acquireRawDepthImage16Bits();
            Image.Plane depthPlane = depthImage.getPlanes()[0];
            Log.println(Log.INFO, "scan", "grabbed depth buffer!");

            int center_index = depthPlane.getRowStride() * ((depthImage.getHeight() / 2) - 1) + depthImage.getWidth() / 2;
            ShortBuffer buffer = depthPlane.getBuffer().order(ByteOrder.nativeOrder()).asShortBuffer();
            Log.println(Log.INFO, "scan_data", "middle is " + buffer.get(center_index) + "mm away");
            if(buffer.get(center_index) != 0){
                vibrator.cancel();
                this.lastDepth = buffer.get(center_index);
                vibrator.vibrate(VibrationEffect.createOneShot(400L, mmToIntensity(buffer.get(center_index))));
            }
        } catch (NotYetAvailableException e) {
            // ok
        } catch (CameraNotAvailableException e) {
            Toast.makeText(activity, "Camera not available.", Toast.LENGTH_LONG).show();
        } catch(SessionPausedException ignored) {

        } finally {
            if(depthImage != null) depthImage.close();
        }

    }

    public static int mmToIntensity(short mm) {
        if(mm > 30000) {
            return 10;
        } else if(mm > 20000) {
            return 20;
        } else if(mm > 15000) {
            return 40;
        } else if(mm > 12500) {
            return 60;
        } else if(mm > 7500) {
            return 75;
        } else if(mm > 5000) {
            return 125;
        } else if(mm > 2000) {
            return 150;
        } else if(mm > 1000) {
            return 200;
        } else if(mm > 0) {
            return 255;
        }
        return 1;
    }
}
