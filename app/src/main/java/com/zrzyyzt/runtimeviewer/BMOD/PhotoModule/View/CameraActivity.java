package com.zrzyyzt.runtimeviewer.BMOD.PhotoModule.View;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.zrzyyzt.runtimeviewer.R;
import com.zrzyyzt.runtimeviewer.Utils.TimeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private AutoFitTextureView textureView;

    // 摄像头ID（通常0代表后置摄像头，1代表前置摄像头）
    private String mCameraId = "0";
    // 定义代表摄像头的成员变量
    private CameraDevice cameraDevice;
    // 预览尺寸
    private Size previewSize;
    private CaptureRequest.Builder previewRequestBuilder;
    // 定义用于预览照片的捕获请求
    private CaptureRequest previewRequest;
    // 定义CameraCaptureSession成员变量
    private CameraCaptureSession captureSession;
    private ImageReader mImageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        textureView = (AutoFitTextureView) findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        findViewById(R.id.capture).setOnClickListener(this);
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // 当TextureView可用时，打开摄像头
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        // 摄像头被打开时激发该方法
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            CameraActivity.this.cameraDevice = camera;

            // 开始预览
            createCameraPreviewSession();  // ②
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
            CameraActivity.this.cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            CameraActivity.this.cameraDevice = null;
            CameraActivity.this.finish();
        }
    };


    @Override
    public void onClick(View v) {
        captureStillPicture();
    }

    private void captureStillPicture() {
        try {
            if (cameraDevice == null) {
                return;
            }
            // 创建作为拍照的CaptureRequest.Builder
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将imageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // 设置自动对焦模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 设置自动曝光模式
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // 根据设备方向计算设置照片的方向
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION
                    , ORIENTATIONS.get(rotation));
            // 停止连续取景
            captureSession.stopRepeating();
            // 捕获静态图像
            captureSession.capture(captureRequestBuilder.build()
                    , new CameraCaptureSession.CaptureCallback()  // ⑤
                    {
                        // 拍照完成时激发该方法
                        @Override
                        public void onCaptureCompleted(CameraCaptureSession session
                                , CaptureRequest request, TotalCaptureResult result) {
                            try {
                                // 重设自动对焦模式
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                                // 设置自动曝光模式
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                                // 打开连续取景模式
                                captureSession.setRepeatingRequest(previewRequest, null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开摄像头
     * @param width
     * @param height
     */
    private void openCamera(int width, int height) {
        setUpCameraOutputs(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"未获取摄像头权限",Toast.LENGTH_LONG).show();
                requestCameraPermission();
                return;
            }
            manager.openCamera(mCameraId, stateCallback, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }


    private void createCameraPreviewSession() {

        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(),previewSize.getHeight());

            Surface surface = new Surface(texture);

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    // 如果摄像头为null，直接结束方法
                    if (null == cameraDevice)
                    {
                        return;
                    }
                    // 当摄像头已经准备好时，开始显示预览
                    captureSession = session;
                    try{
                        // 设置自动对焦模式
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // 设置自动曝光模式
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        // 开始显示相机预览
                        previewRequest = previewRequestBuilder.build();
                        // 设置预览时连续捕获图像数据
                        captureSession.setRepeatingRequest(previewRequest,
                                null, null);  // ④
                    }catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(CameraActivity.this, "配置失败！"
                            , Toast.LENGTH_SHORT).show();
                }
            },null);
        }catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 获取指定摄像头的特性
            CameraCharacteristics characteristics
                    = manager.getCameraCharacteristics(mCameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // 获取摄像头支持的最大尺寸
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new CompareSizesByArea());
            // 创建一个ImageReader对象，用于获取摄像头的图像数据
            mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                    ImageFormat.JPEG, 2);

            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    // 获取捕获的照片数据
                    Image image = reader.acquireNextImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    // 使用IO流将照片写入指定文件

                    File file = new File(getExternalFilesDir(null), TimeUtils.getCurrentTime() + ".jpg");
                    buffer.get(bytes);
                    try (
                            FileOutputStream output = new FileOutputStream(file))
                    {
                        output.write(bytes);
                        Toast.makeText(CameraActivity.this, "保存: "
                                + file, Toast.LENGTH_SHORT).show();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        image.close();
                    }
                }
            },null);

            // 获取最佳的预览尺寸
            previewSize = chooseOptimalSize(map.getOutputSizes(
                    SurfaceTexture.class), width, height, largest);
            // 根据选中的预览尺寸来调整预览组件（TextureView）的长宽比
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.
                        getHeight());
            }
            else
            {
                textureView.setAspectRatio(previewSize.getHeight(),
                        previewSize.getWidth());
            }
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
        catch (NullPointerException e)
        {
            System.out.println("出现错误。");
        }
    }

    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // 收集摄像头支持的大过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height)
            {
                bigEnough.add(option);
            }
        }

        // 如果找到多个预览尺寸，获取其中面积最小的
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else
        {
            System.out.println("找不到合适的预览尺寸！！！");
            return choices[0];
        }
    }

    private class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    public static class ConfirmationDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }
}
