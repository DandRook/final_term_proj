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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private GestureDetector gestureDetector;
    private TextView swipeTextView; // Reference to the TextView that shows questions
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize TextView reference
        swipeTextView = findViewById(R.id.swipe_area);

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
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("test_data");

        // Write data to Firebase
        databaseReference.setValue("Hello, Firebase!")
                .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Write successful!"))
                .addOnFailureListener(e -> Log.e("FIREBASE", "Write failed", e));

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    String value = snapshot.getValue(String.class);
                    Log.d("FIREBASE", "Read value: " + value);
                } else {
                    Log.e("FIREBASE", "No data found in database.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FIREBASE", "Read failed", error.toException());
            }
        });
    }

    // Custom Gesture Listener Class
    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 50;
        private static final int SWIPE_VELOCITY_THRESHOLD = 50;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            Log.d("Gesture", "onFling detected - X: " + diffX + ", Y: " + diffY);

            try {
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // Horizontal swipe
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Right swipe moves to next question
                            Log.d("GESTURE", "Swiped Right");
                            moveToNextQuestion();
                        } else {
                            // Left Swipe moves to previous question
                            Log.d("GESTURE", "Swiped Left");
                            moveToPreviousQuestion();
                        }
                        return true;
                    }
                }

            } catch (Exception e) {
                Log.e("GESTURE", "Error processing swipe gesture", e);
            }
            return false;
        }
    }
}