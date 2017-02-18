package com.aaronlife.remoteshot;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    private SurfaceHolder mHolder = null;
    private Camera mCamera;

    private static OutputStream out;

    public CameraView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setOutputStream (OutputStream out)
    {
        CameraView.out = out;
        Log.d("aarontest", "開始傳資料");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        mCamera = Camera.open();
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewCallback(this);

        try
        {
            mCamera.setPreviewDisplay(holder);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Camera.Parameters parameters = mCamera.getParameters();

        List<Camera.Size> availablePreviewSizes =
                        mCamera.getParameters().getSupportedPreviewSizes();
        for(Camera.Size s : availablePreviewSizes)
        {
            Log.d("aarontest", "Support Size: " + s.width + ", " + s.height);
        }

        // 設定Preview解析度
        parameters.setPreviewSize(640, 480);

        // 支援的照片解析度
        List<Camera.Size> availablePictureSizes =
                mCamera.getParameters().getSupportedPictureSizes();

        Camera.Size bestSize = availablePictureSizes.get(0);
        for(Camera.Size s : availablePictureSizes)
        {
            Log.d("aarontest", "Support Size: " + s.width + ", " + s.height);

            if(bestSize.width < s.width || bestSize.height < s.height)
                bestSize = s;
        }

        // 設定用最大解析度來拍照
        parameters.setPictureSize(bestSize.width, bestSize.height);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        if(getContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);

        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.autoFocus(null); // must call after startPreview
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        mHolder.removeCallback(this);
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        Camera.Parameters parameters = camera.getParameters();
        int imageFormat = parameters.getPreviewFormat();
        int w = parameters.getPreviewSize().width;
        int h = parameters.getPreviewSize().height;
        int format = parameters.getPreviewFormat();

        if (imageFormat == ImageFormat.NV21)
        {
            YuvImage img = new YuvImage(data, format, w, h, null);

            Rect rect = new Rect(0, 0, w, h);
            ByteArrayOutputStream outPic = new ByteArrayOutputStream();
            img.compressToJpeg(rect, 50, outPic);

            if(out != null)
            {
                try
                {
                    // 先傳送大小
                    byte[] sizeArray = GlobalSettings.intToByteArray(outPic.size());
                    out.write(sizeArray, 0, 4);
                    out.flush();

                    // 傳送資料
                    out.write(outPic.toByteArray(), 0, outPic.size());
                    out.flush();
                }
                catch(IOException e)
                {
                    Log.d("aarontest", "Send preview error: " + e.getMessage());
                    ((CameraActivity)getContext()).updateMessage("已斷線");
                    out = null;
                }
            }
        }
    }

    public void takeShot()
    {
        try // 0111  按太快會失敗，需做防範
        {
            mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
        }
        catch(RuntimeException e)
        {
            Log.d("aarontest", "拍照失敗(" + e.getMessage() + ")");
        }
    }

    public boolean turnOnFlash()
    {
        try
        {
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            mCamera.setParameters(p);
        }
        catch(RuntimeException e)
        {
            return false;
        }

        return true;
    }

    public boolean trunOffFlash()
    {
        try
        {
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(p);
        }
        catch(RuntimeException e)
        {
            return false;
        }

        return true;
    }

    public boolean trunAutoFlash()
    {
        try
        {
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            mCamera.setParameters(p);
        }
        catch(RuntimeException e)
        {
            return false;
        }

        return true;
    }

    public boolean trunFillFlash()
    {
        try
        {
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_RED_EYE);
            mCamera.setParameters(p);
        }
        catch(RuntimeException e)
        {
            return false;
        }

        return true;
    }

    public boolean trunTorchFlash()
    {
        try
        {
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(p);
        }
        catch(RuntimeException e)
        {
            return false;
        }

        return true;
    }

    public boolean autoFocus()
    {
        try
        {
            mCamera.autoFocus(null);
        }
        catch(RuntimeException e)
        {
            return false;
        }

        return true;
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback()
    {
        public void onShutter()
        {
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback()
    {
        public void onPictureTaken(byte[] data, Camera camera)
        {
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback()
    {
        public void onPictureTaken(byte[] data, Camera camera)
        {
            new SaveImageTask().execute(data);

            mCamera.startPreview();
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, File>
    {
        @Override
        protected File doInBackground(byte[]... data)
        {
            try
            {
                Bitmap bm =
                    BitmapFactory.decodeByteArray(data[0], 0, data[0].length);
                Matrix m = new Matrix();
                m.postRotate(90);
                Bitmap pic =
                    Bitmap.createBitmap(bm , 0, 0, bm .getWidth(), bm .getHeight(), m, true);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                pic.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                File path =
                    new File(Environment.getExternalStoragePublicDirectory(
                             Environment.DIRECTORY_DCIM) + "/RemoteShot");
                path.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());

                File file =
                    new File(Environment.getExternalStoragePublicDirectory(
                             Environment.DIRECTORY_DCIM) + "/RemoteShot/" + fileName);

                FileOutputStream outFile = new FileOutputStream(file);
                outFile.write(stream.toByteArray());
                outFile.flush();
                outFile.close();

                return file;
            }

            catch (IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(File file)
        {
            super.onPostExecute(file);

            if(file != null)
                refreshGallery(file);
        }
    }

    private void refreshGallery(File file)
    {
        Intent mediaScanIntent =
                         new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        getContext().sendBroadcast(mediaScanIntent);
    }
}
