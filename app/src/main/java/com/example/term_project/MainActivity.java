package com.example.term_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    private GestureDetector gestureDetector;
    private TextView swipeTextView; // Reference to the TextView that shows questions
    private String uId = "";
    private int currentQuestionIndex = 0; // To track the current question

    private Button returnButton; // Declare the return button


    // Array of quiz questions for user to swipe through, can swipe right or left
    private String[] quizQuestions = {
            "What is the capital of France?",
            "What is 2 + 2?",
            "Who wrote '1984'?",
            "What is the smallest planet in our solar system?",
            "What is the largest ocean on Earth?"
    };

    private DatabaseReference databaseReference; // Firebase database reference

    //Todo if need to debug swipe gestures then use view>tools>logcat and check output
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the button
        returnButton = findViewById(R.id.return_button);

        // Set up onClickListener for the button
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create an Intent to go back to the Login activity
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent); // Start the Login activity
                finish(); // Optionally, finish MainActivity to prevent back navigation
            }
        });

        uId = getIntent().getStringExtra("Username");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize TextView reference
        swipeTextView = findViewById(R.id.swipe_area);
        // Create database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("user_data");
        // Initialize GestureDetector inside onCreate()
        gestureDetector = new GestureDetector(this, new SwipeGestureListener());

        // Attach GestureDetector to a View
        View swipeView = findViewById(R.id.main);
        if (swipeView != null) {
            swipeView.setOnTouchListener((v, event) -> {
                boolean result = gestureDetector.onTouchEvent(event);
                Log.d("GESTURE", "Touch event detected: " + event.getAction());
                // Keep true else won't record swipes just touches
                return true;
            });
        } else {
            Log.e("GESTURE", "Error: swipe_area view not found!");
        }
        // Display the first question from array on startup screen
        updateQuestion();

        // Keep Firebase database logic separate
        setupFirebase();
    }

    // Function to update the question displayed in the TextView
    private void updateQuestion() {
        swipeTextView.setText(quizQuestions[currentQuestionIndex]);
    }

    // Function to move to the next question by right swipe
    private void moveToNextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex >= quizQuestions.length) {
            currentQuestionIndex = 0; // Loop back to the first question
        }
        updateQuestion();
    }

    // Function to move to the previous question by left swipe
    private void moveToPreviousQuestion() {
        currentQuestionIndex--;
        if (currentQuestionIndex < 0) {
            currentQuestionIndex = quizQuestions.length - 1; // Loop back to the last question
        }
        updateQuestion();
    }

    // Function to handle Firebase reads separately
    private void setupFirebase() {

        // Check for uId and start data tranmisssion
        if (uId != null && !uId.isEmpty()) {
            DatabaseReference userRef = databaseReference.child(uId);
            userRef.child("activity").push().setValue("User " + uId + " Started the quiz")
                    .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "User data saved!"))
                    .addOnFailureListener(e -> Log.e("FIREBASE", "Write failed", e));
        }
    }

    // Custom Gesture Listener Class
    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 50;
        private long touchStartTime;
        private float touchStartX, touchStartY;
        private float touchEndX, touchEndY;
        private float touchPressure;

        @Override
        public boolean onDown(MotionEvent e) {
            // Capture touch start details
            touchStartX = e.getX();
            touchStartY = e.getY();
            touchPressure = e.getPressure();
            touchStartTime = System.currentTimeMillis();  // Timestamp of touch start
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Capture touch end details
            touchEndX = e2.getX();
            touchEndY = e2.getY();
            long touchEndTime = System.currentTimeMillis();  // Timestamp of touch end
            long swipeDuration = touchEndTime - touchStartTime;  // Duration of swipe

            float diffX = touchEndX - touchStartX;
            Log.d("Gesture", "onFling detected - X: " + diffX);

            try {
                if (Math.abs(diffX) > SWIPE_THRESHOLD) {
                    String swipeDirection = (diffX > 0) ? "Right" : "Left";

                    // Log swipe details for debugging
                    Log.d("GESTURE", "Swiped " + swipeDirection);
                    sendUserResponse(swipeDirection, velocityX, velocityY, swipeDuration, touchStartX, touchStartY, touchEndX, touchEndY, touchPressure);
                    if (diffX > 0) {
                        moveToNextQuestion();
                    } else {
                        moveToPreviousQuestion();
                    }
                    return true;
                }

            } catch (Exception e) {
                Log.e("GESTURE", "Error processing swipe gesture", e);
            }
            return false;
        }
    }

    private void sendUserResponse(String action, float velocityX, float velocityY, long duration,
                                  float startX, float startY, float endX, float endY, float pressure) {
        Log.d("DEBUG", "sendUserResponse called for action: " + action);

        if (uId == null || uId.isEmpty()) {
            Log.e("FIREBASE", "uId is NULL or EMPTY! Cannot save swipe.");
            return; // Stop execution if uId is invalid
        }

        DatabaseReference userRef = databaseReference.child(uId).child("swipes");

        // Gen a unique key for each instance
        String swipeId = userRef.push().getKey();
        long timestamp = System.currentTimeMillis();  // Current timestamp

        Log.d("DEBUG", "Generated swipeId: " + swipeId);
        Log.d("DEBUG", "Timestamp: " + timestamp);

        // Create a new swipe record
        SwipeData swipeData = new SwipeData(action, quizQuestions[currentQuestionIndex], timestamp, velocityX, velocityY,
                duration, startX, startY, endX, endY, pressure);

        Log.d("DEBUG", "SwipeData - Action: " + swipeData.action);
        Log.d("DEBUG", "SwipeData - Question: " + swipeData.question);
        Log.d("DEBUG", "SwipeData - Timestamp: " + swipeData.timestamp);

        if (swipeId != null) {
            // Save data as a structured object
            userRef.child(swipeId).setValue(swipeData)
                    .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Swipe action logged!"))
                    .addOnFailureListener(e -> Log.e("FIREBASE", "Failed to log swipe action", e));
        } else {
            Log.e("FIREBASE", "swipeId is NULL! Cannot save swipe.");
        }
    }

    // Helper class to structure swipe data
    public static class SwipeData {
        public String action;
        public String question;
        public long timestamp;
        public float velocityX;
        public float velocityY;
        public long duration;  // Duration of the swipe in milliseconds
        public float startX;
        public float startY;
        public float endX;
        public float endY;
        public float pressure;

        // Constructor
        public SwipeData(String action, String question, long timestamp, float velocityX, float velocityY,
                         long duration, float startX, float startY, float endX, float endY, float pressure) {
            this.action = action;
            this.question = question;
            this.timestamp = timestamp;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.duration = duration;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.pressure = pressure;
        }
    }
}