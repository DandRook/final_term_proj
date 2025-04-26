package com.example.term_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class GameActivity extends AppCompatActivity {
    private static final String TAG = "GAME_ACTIVITY";
    private TextView questionText, timerText, swipeHint;
    private Button[] optionButtons;
    private Button nextButton;
    private String questionId, selectedAnswer = "";
    private long startTime;
    private int questionCount = 0;
    private int correctCount = 0;
    private final int MAX_QUESTIONS = 10;
    private final int TOTAL_TIME = 30; // 2 minutes for entire game
    private CountDownTimer totalGameTimer;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gameact);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("results");

        questionText = findViewById(R.id.question_text);
        swipeHint = findViewById(R.id.swipe_hint);
        timerText = findViewById(R.id.timer_text);
        nextButton = findViewById(R.id.next_question_button);

        optionButtons = new Button[]{
                findViewById(R.id.option1),
                findViewById(R.id.option2),
                findViewById(R.id.option3),
                findViewById(R.id.option4)
        };

        nextButton.setEnabled(false); // Disable at start

        for (Button button : optionButtons) {
            button.setOnClickListener(v -> {
                for (Button b : optionButtons)
                    b.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                selectedAnswer = ((Button) v).getText().toString();
                nextButton.setEnabled(true); // Enable after selection
            });
        }

        nextButton.setOnClickListener(v -> {

            finalizeAnswer();
        });

        startTotalTimer();
        fetchNextQuestion();
    }

    private void fetchNextQuestion() {
        if (questionCount >= MAX_QUESTIONS) {
            showResultsDialog();
            return;
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "Calling /next-question API to fetch new question.");
                URL url = new URL("https://final-term-proj.onrender.com/next-question?userId=" + mAuth.getUid());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = in.readLine();
                in.close();

                JSONObject json = new JSONObject(response);
                questionId = json.getString("questionId");
                String question = json.getString("question");

                JSONArray optsArray = json.getJSONArray("options");
                String[] options = new String[4];
                for (int i = 0; i < 4; i++) {
                    options[i] = i < optsArray.length() ? optsArray.getString(i) : "";
                }

                runOnUiThread(() -> {
                    questionText.setText(question);
                    swipeHint.setText("Tap an option, then tap Next");
                    for (int i = 0; i < optionButtons.length; i++) {
                        optionButtons[i].setVisibility(options[i].isEmpty() ? View.GONE : View.VISIBLE);
                        optionButtons[i].setText(options[i]);
                        optionButtons[i].setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    }
                    selectedAnswer = "";
                    nextButton.setEnabled(false); // Lock again until new selection
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startTotalTimer() {
        totalGameTimer = new CountDownTimer(TOTAL_TIME * 1000L, 1000) {
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.format(Locale.getDefault(), "%ds", millisUntilFinished / 1000));
            }

            public void onFinish() {
                showResultsDialog();
            }
        }.start();
    }

    private void finalizeAnswer() {
        if (selectedAnswer.isEmpty()) return;

        questionCount++;
        long timeSpent = (System.currentTimeMillis() - startTime) / 1000;
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown";
        // NEW: Submit the answer to the server
        new Thread(() -> {
            try {
                URL url = new URL("https://final-term-proj.onrender.com/submit-answer");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setDoOutput(true);

                JSONObject postData = new JSONObject();
                postData.put("userId", userId);
                postData.put("questionId", questionId);
                postData.put("userAnswer", selectedAnswer);

                connection.getOutputStream().write(postData.toString().getBytes("utf-8"));

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = in.readLine();
                in.close();

                JSONObject jsonResponse = new JSONObject(response);
                boolean isCorrect = jsonResponse.getBoolean("correct");

                if (isCorrect) correctCount++;

                // Always log the answer in Firebase
                DatabaseReference userRef = dbRef.child(userId).push();
                userRef.setValue(new QuestionLog(questionId, selectedAnswer, timeSpent, isCorrect))
                        .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Logged"))
                        .addOnFailureListener(e -> Log.e("FIREBASE", "Logging failed", e));

                runOnUiThread(() -> {
                    selectedAnswer = "";
                    fetchNextQuestion();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showResultsDialog() {
        if (isFinishing()) return;
        totalGameTimer.cancel();

        int percentCorrect = (int) ((correctCount / (float) MAX_QUESTIONS) * 100);
        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("Correct: " + correctCount + "/" + MAX_QUESTIONS + "\nAccuracy: " + percentCorrect + "%")
                .setPositiveButton("OK", (dialog, which) -> {
                    startActivity(new Intent(GameActivity.this, MainActivity.class));
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    static class QuestionLog {
        public String questionId, selected;

        public QuestionLog() {}

        public QuestionLog(String questionId, String selected, long timeSpent, boolean isCorrect) {
            this.questionId = questionId;
            this.selected = selected;
        }
    }
}
