package com.example.floatinglayout;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

public class FloatingLayoutService extends Service {
    View viewRoot;
    WindowManager windowManager;
    WindowManager.LayoutParams rootParams;
    ImageView imageView, close;
    TextView textView;
    int width;

    public FloatingLayoutService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        width = metrics.widthPixels;

        if (rootParams == null) {
            int LAYOUT_FLAG;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
            }
            rootParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    LAYOUT_FLAG,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("com.example.floatinglayout", "Floating Layout Service", NotificationManager.IMPORTANCE_LOW);
                channel.setLightColor(Color.BLUE);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                assert notificationManager != null;
                notificationManager.createNotificationChannel(channel);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "com.example.floatinglayout");
                Notification notification = builder.setOngoing(true)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Floating Layout Service is Running")
                        .setPriority(NotificationManager.IMPORTANCE_HIGH)
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .build();
                startForeground(2, notification);
            }

            if (viewRoot == null) {
                viewRoot = LayoutInflater.from(this).inflate(R.layout.floating_layout, null);
                rootParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.START;
                rootParams.x = 0;
                rootParams.y = 0;

                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                windowManager.addView(viewRoot, rootParams);

                textView = viewRoot.findViewById(R.id.textView);
                imageView = viewRoot.findViewById(R.id.imageView);
                close = viewRoot.findViewById(R.id.close);

                viewRoot.findViewById(R.id.root).setOnTouchListener(new View.OnTouchListener() {
                    private int initialX;
                    private int initialY;
                    private int initialTouchX;
                    private int initialTouchY;
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                initialX = rootParams.x;
                                initialY = rootParams.y;

                                initialTouchX = (int) motionEvent.getRawX();
                                initialTouchY = (int) motionEvent.getRawY();
                                return true;
                            case MotionEvent.ACTION_UP:
                                if (motionEvent.getRawX() < width / 2) {
                                    rootParams.x = 0;
                                } else {
                                    rootParams.x = width;
                                }
                                rootParams.y = initialY + (int) (motionEvent.getRawY() - initialTouchY);
                                windowManager.updateViewLayout(viewRoot, rootParams);

                                int xDiff = (int) (motionEvent.getRawX() - initialTouchX);
                                int yDiff = (int) (motionEvent.getRawY() - initialTouchY);

                                if (xDiff < 20 && yDiff < 20) {
                                    if (textView.getVisibility() == View.GONE) {
                                        textView.setVisibility(View.VISIBLE);
                                        close.setVisibility(View.VISIBLE);
                                    } else {
                                        textView.setVisibility(View.GONE);
                                        close.setVisibility(View.GONE);
                                    }
                                }
                                return true;
                                case MotionEvent.ACTION_MOVE:
                                    rootParams.x = initialX + (int) (motionEvent.getRawX() - initialTouchX);
                                    rootParams.y = initialY + (int) (motionEvent.getRawY() - initialTouchY);

                                    windowManager.updateViewLayout(viewRoot, rootParams);
                                    return true;
                        }
                        return false;
                    }
                });

                close.setOnClickListener(view -> stopService());
            }
        }
    }

    private void stopService() {
        try {
            stopForeground(true);
            stopSelf();
            windowManager.removeViewImmediate(viewRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}