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
    public int intensity = 0;

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
            if(buffer.get(center_index) != 0) {
                vibrator.cancel();
                this.lastDepth = buffer.get(center_index);
                this.intensity = mmToIntensity(this.lastDepth);
            }
            vibrator.vibrate(VibrationEffect.createOneShot(400L, mmToIntensity(lastDepth)));
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
        return (int) Math.min(Math.max((Math.pow((30000.0/ (double) mm), 1.0/8.0) * 255.0 - 255.0), 1), 250);
    }
}
