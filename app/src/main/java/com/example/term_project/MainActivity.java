package com.example.term_project;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;
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

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            Log.d("Gesture", "onFling detected - X: " + diffX);

            try {
                if (Math.abs(diffX) > SWIPE_THRESHOLD) {
                    if (diffX > 0) {
                        // Right swipe moves to next question
                        Log.d("GESTURE", "Swiped Right");
                        sendUserResponse("Swiped Right");
                        moveToNextQuestion();
                    } else {
                        // Left Swipe moves to previous question
                        Log.d("GESTURE", "Swiped Left");
                        sendUserResponse("Swiped Left");
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

    private void sendUserResponse(String action) {
        Log.d("DEBUG", "sendUserResponse called for action: " + action);

        if (uId == null || uId.isEmpty()) {
            Log.e("FIREBASE", "uId is NULL or EMPTY! Cannot save swipe.");
            return; // Stop execution if uId is invalid
        }
            DatabaseReference userRef = databaseReference.child(uId).child("swipes");

            // Gen a unique key for each instance
            String swipeId = userRef.push().getKey();
            long timestamp = System.currentTimeMillis();

            Log.d("DEBUG", "Generated swipeId: " + swipeId);
            Log.d("DEBUG", "Timestamp: " + timestamp);

            // Debug logs to check values before saving
            Log.d("DEBUG", "SwipeData - Action: " + action);
            Log.d("DEBUG", "SwipeData - Question: " + quizQuestions[currentQuestionIndex]);
            Log.d("DEBUG", "SwipeData - Timestamp: " + timestamp);

            if(swipeId != null){
                // Create a new swipe record
                SwipeData swipeData = new SwipeData(action, quizQuestions[currentQuestionIndex], timestamp);

                Log.d("DEBUG", "SwipeData - Action: " + swipeData.action);
                Log.d("DEBUG", "SwipeData - Question: " + swipeData.question);
                Log.d("DEBUG", "SwipeData - Timestamp: " + swipeData.timestamp);

                // Save data as a structured object
                userRef.child(swipeId).setValue(swipeData)
                    .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Swipe action logged!"))
                    .addOnFailureListener(e -> Log.e("FIREBASE", "Failed to log swipe action", e));
        } else {
                Log.e("FIREBASE", "uId is NULL or EMPTY! Cannot save swipe.");
            }

    }

    // Helper class to structure swipe data
    public static class SwipeData {
        public String action;
        public String question;
        public long timestamp;

        // Constructor
        public SwipeData(String action, String question, long timestamp) {
            this.action = action;
            this.question = question;
            this.timestamp = timestamp;
        }
    }
}