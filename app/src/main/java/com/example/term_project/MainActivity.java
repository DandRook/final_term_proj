package com.example.term_project;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private GestureDetector gestureDetector;
    //Todo if need to debug swipe gestures then use view>tools>logcat and check output
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize GestureDetector inside onCreate()
        gestureDetector = new GestureDetector(this, new SwipeGestureListener());

        // Attach GestureDetector to a View
        View swipeView = findViewById(R.id.swipe_area);
        if(swipeView != null){
            swipeView.setOnTouchListener((v, event) -> {
                boolean result = gestureDetector.onTouchEvent(event);
                Log.d("GESTURE", "Touch event detected: " + event.getAction());
                // keep true else won't record swipes just touches
                return true;
            });
        } else {
            Log.e("GESTURE", "Error: swipe_area view not found!");
        }
    }

    // Custom Gesture Listener Class
    //Todo make variables to track data and pass along to firebase for storage
    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 50;
        private static final int SWIPE_VELOCITY_THRESHOLD = 50;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            Log.d("Gesture", "onFling dected - X: " + diffX + ", Y: " + diffY);

            try {
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // Horizontal swipe
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Right Swipe
                            Log.d("GESTURE", "Swiped Right");
                        } else {
                            // Left Swipe
                            Log.d("GESTURE", "Swiped Left");
                        }
                        return true;
                    }
                } else {
                    // Vertical swipe
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            // Down Swipe
                            Log.d("GESTURE", "Swiped Down");
                        } else {
                            // Up Swipe
                            Log.d("GESTURE", "Swiped Up");
                        }
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e("GESTURE", "Eror processign swip gesture", e);
            }
            return false;
        }

        public void onSwipeRight() {
            System.out.println("Swiped Right");
        }

        public void onSwipeLeft() {
            System.out.println("Swiped Left");
        }

        public void onSwipeUp() {
            System.out.println("Swiped Up");
        }

        public void onSwipeDown() {
            System.out.println("Swiped Down");
        }
    }
}