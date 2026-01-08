package com.gamebooster.launcher;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayService extends Service {
    
    private WindowManager windowManager;
    private View overlayView;
    private View expandedView;
    private boolean isExpanded = false;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Create floating button
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_button, null);
        expandedView = inflater.inflate(R.layout.overlay_expanded, null);
        
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;
        
        windowManager.addView(overlayView, params);
        
        setupOverlayButton();
    }
    
    private void setupOverlayButton() {
        ImageButton btnFloat = overlayView.findViewById(R.id.btnFloat);
        
        btnFloat.setOnClickListener(v -> toggleExpanded());
        
        // Make draggable
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) overlayView.getLayoutParams();
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });
    }
    
    private void toggleExpanded() {
        if (isExpanded) {
            // Collapse
            windowManager.removeView(expandedView);
            isExpanded = false;
        } else {
            // Expand
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.CENTER;
            
            windowManager.addView(expandedView, params);
            setupExpandedControls();
            isExpanded = true;
        }
    }
    
    private void setupExpandedControls() {
        TextView btnRobot = expandedView.findViewById(R.id.btnOverlayRobot);
        TextView btnWoman = expandedView.findViewById(R.id.btnOverlayWoman);
        TextView btnMan = expandedView.findViewById(R.id.btnOverlayMan);
        TextView btnChild = expandedView.findViewById(R.id.btnOverlayChild);
        TextView btnMonster = expandedView.findViewById(R.id.btnOverlayMonster);
        TextView btnOff = expandedView.findViewById(R.id.btnOverlayOff);
        TextView btnClose = expandedView.findViewById(R.id.btnOverlayClose);
        
        btnRobot.setOnClickListener(v -> {
            AudioEffectService.setEffect(AudioEffectService.VoiceEffect.ROBOT);
            SystemVoiceService.setEffect(SystemVoiceService.VoiceEffect.ROBOT);
            Toast.makeText(this, "🤖 Robot", Toast.LENGTH_SHORT).show();
            toggleExpanded();
        });
        
        btnWoman.setOnClickListener(v -> {
            AudioEffectService.setEffect(AudioEffectService.VoiceEffect.WOMAN);
            SystemVoiceService.setEffect(SystemVoiceService.VoiceEffect.WOMAN);
            Toast.makeText(this, "👩 Kadın", Toast.LENGTH_SHORT).show();
            toggleExpanded();
        });
        
        btnMan.setOnClickListener(v -> {
            AudioEffectService.setEffect(AudioEffectService.VoiceEffect.MAN);
            SystemVoiceService.setEffect(SystemVoiceService.VoiceEffect.MAN);
            Toast.makeText(this, "👨 Erkek", Toast.LENGTH_SHORT).show();
            toggleExpanded();
        });
        
        btnChild.setOnClickListener(v -> {
            AudioEffectService.setEffect(AudioEffectService.VoiceEffect.CHILD);
            SystemVoiceService.setEffect(SystemVoiceService.VoiceEffect.CHILD);
            Toast.makeText(this, "👶 Çocuk", Toast.LENGTH_SHORT).show();
            toggleExpanded();
        });
        
        btnMonster.setOnClickListener(v -> {
            AudioEffectService.setEffect(AudioEffectService.VoiceEffect.MONSTER);
            SystemVoiceService.setEffect(SystemVoiceService.VoiceEffect.MONSTER);
            Toast.makeText(this, "👹 Canavar", Toast.LENGTH_SHORT).show();
            toggleExpanded();
        });
        
        btnOff.setOnClickListener(v -> {
            AudioEffectService.setEffect(AudioEffectService.VoiceEffect.NONE);
            SystemVoiceService.setEffect(SystemVoiceService.VoiceEffect.NONE);
            stopService(new Intent(this, AudioEffectService.class));
            stopService(new Intent(this, SystemVoiceService.class));
            stopSelf();
            Toast.makeText(this, "❌ Kapatıldı", Toast.LENGTH_SHORT).show();
        });
        
        btnClose.setOnClickListener(v -> toggleExpanded());
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
        if (isExpanded && expandedView != null) {
            windowManager.removeView(expandedView);
        }
    }
}
