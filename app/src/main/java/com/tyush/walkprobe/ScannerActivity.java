package com.tyush.walkprobe;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.camera2.CameraDevice;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextWatcher;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.tyush.walkprobe.databinding.ActivityScanFullBinding;

import java.util.EnumSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ScannerActivity extends AppCompatActivity {
    private Session session;
    private Handler scanHandler = new Handler(Looper.myLooper());
    private ScanRunnable scanner;
    private AppBarConfiguration appBarConfiguration;
    private ActivityScanFullBinding binding;
    private GLSurfaceView.Renderer renderer = new GLSurfaceView.Renderer() {
        // dummy renderer to appease arcore
        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int i, int i1) {

        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            Log.v("renderer", "running scan");
            scanRunnable.run();
        }
    };
    private GLSurfaceView view;
    private ScanRunnable scanRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityScanFullBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // requestPermissions({Manifest.permission.CAMERA});

        view = findViewById(R.id.surfaceView);


        // don't recreate the entire damn gl context when paused
        view.setPreserveEGLContextOnPause(true);
        // use gles 2 b/c it's reliable and mandated
        view.setEGLContextClientVersion(2);
        view.setRenderer(renderer);
        // really, render never but there's no option

        if(session == null) {
            try {
                if(ArCoreApk.getInstance().requestInstall(this, false) == ArCoreApk.InstallStatus.INSTALLED) {
                    session = new Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA));
                    Config conf = session.getConfig();
                    conf.setDepthMode(Config.DepthMode.RAW_DEPTH_ONLY);
                    session.configure(conf);
                    if(!session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)) {
                        Toast.makeText(this, "This device does not support Depth Mode.", Toast.LENGTH_LONG).show();
                        Log.e("MAIN", "DEPTH MODE NOT SUPPORTED!");
                        System.exit(100);
                    }
                }
            } catch (UnavailableDeviceNotCompatibleException |
                     UnavailableUserDeclinedInstallationException |
                     UnavailableArcoreNotInstalledException | UnavailableApkTooOldException |
                     UnavailableSdkTooOldException e) {
            }
        }
        enableAr();
        //scanHandler.post();
        session.setCameraTextureName(view.getId());
        //session.setCameraTextureName(240);

        Config config = session.getConfig();
        config.setUpdateMode(Config.UpdateMode.BLOCKING);
        session.configure(config);
        this.scanRunnable = new ScanRunnable(this, session, scanHandler, 240);
        //scanHandler.post(new ScanRunnable(this, session, scanHandler, view.getId()));
    }

    public long getDelay() {
        return 1000;
    }

    public void setText(String text) {
        TextView title = findViewById(R.id.distanceView);
        title.setText(text);
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // TODO: can we even access cam?

        try {
            if(session == null) {
                if(ArCoreApk.getInstance().requestInstall(this, false) == ArCoreApk.InstallStatus.INSTALLED) {
                    session = new Session(this);
                    if(!session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        Toast.makeText(this, "This device does not support Depth Mode.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } catch (UnavailableDeviceNotCompatibleException |
                 UnavailableUserDeclinedInstallationException | UnavailableSdkTooOldException |
                 UnavailableArcoreNotInstalledException | UnavailableApkTooOldException e) {
            throw new RuntimeException(e);
        }


        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            Toast.makeText(this, "Camera not available!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(session != null) session.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] perms, int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        finish();
    }

    void enableAr() {
        ArCoreApk.getInstance().checkAvailabilityAsync(this, availability -> {
            if(!availability.isSupported()) {
                Toast.makeText(this, "This device does not support AR.", Toast.LENGTH_LONG).show();
            }
        });
    }

    public Session getSession() {
        return this.session;
    }
}
