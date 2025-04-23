package com.example.term_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GameActivity extends AppCompatActivity {

    private TextView questionText, timerText;
    private Button[] optionButtons;
    private String userId;

    private String currentQuestionId;
    private String currentQuestionText;
    private long questionStartTime;

    private int correctCount = 0;
    private int totalAnswered = 0;

    private CountDownTimer matchTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gameact);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        questionText = findViewById(R.id.question_text);
        timerText = findViewById(R.id.timer_text);
        optionButtons = new Button[]{
                findViewById(R.id.option_button_1),
                findViewById(R.id.option_button_2),
                findViewById(R.id.option_button_3),
                findViewById(R.id.option_button_4)
        };

        for (Button btn : optionButtons) {
            btn.setOnClickListener(view -> handleAnswer(btn.getText().toString()));
        }

        startMatchTimer();
        fetchNextQuestion();
    }

    private void startMatchTimer() {
        matchTimer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time: " + millisUntilFinished / 1000 + "s");
            }

            public void onFinish() {
                Toast.makeText(GameActivity.this, "Match Over!", Toast.LENGTH_SHORT).show();
                showResultsDialog();
            }
        }.start();
    }

    private void fetchNextQuestion() {
        runOnUiThread(() -> {
            questionText.setText("Loading...");
            for (Button btn : optionButtons) {
                btn.setEnabled(false);
                btn.setVisibility(Button.GONE);
            }
        });

        new Thread(() -> {
            try {
                URL url = new URL("https://final-term-proj.onrender.com/next-question?userId=" + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                JSONObject json = new JSONObject(response.toString());
                currentQuestionId = json.getString("questionId");
                currentQuestionText = json.getString("question");
                JSONArray optionsArray = json.getJSONArray("options");

                questionStartTime = System.currentTimeMillis();

                runOnUiThread(() -> {
                    questionText.setText(currentQuestionText);
                    for (int i = 0; i < optionButtons.length; i++) {
                        if (i < optionsArray.length()) {
                            try {
                                optionButtons[i].setText(optionsArray.getString(i));
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            optionButtons[i].setVisibility(Button.VISIBLE);
                            optionButtons[i].setEnabled(true);
                        } else {
                            optionButtons[i].setVisibility(Button.GONE);
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(GameActivity.this, "Failed to load question", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void handleAnswer(String userAnswer) {
        long timeSpent = System.currentTimeMillis() - questionStartTime;
        totalAnswered++;

        submitAnswer(userAnswer, isCorrect -> {
            if (isCorrect) correctCount++;
            logAnswerToFirebase(userAnswer, timeSpent, isCorrect);
            fetchNextQuestion();
        });
    }

    private void submitAnswer(String userAnswer, AnswerCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://final-term-proj.onrender.com/submit-answer");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String payload = new JSONObject()
                        .put("userId", userId)
                        .put("questionId", currentQuestionId)
                        .put("userAnswer", userAnswer)
                        .toString();

                conn.getOutputStream().write(payload.getBytes());

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) result.append(line);
                in.close();

                JSONObject response = new JSONObject(result.toString());
                boolean correct = response.getBoolean("correct");

                runOnUiThread(() -> callback.onResult(correct));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(GameActivity.this, "Error submitting answer", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void logAnswerToFirebase(String userAnswer, long timeSpent, boolean isCorrect) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        FirebaseDatabase.getInstance().getReference("user_data")
                .child(userId)
                .child("answers")
                .push()
                .setValue(new AnswerData(
                        currentQuestionId,
                        currentQuestionText,
                        userAnswer,
                        isCorrect,
                        timeSpent,
                        timeStamp
                ))
                .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Answer logged!"))
                .addOnFailureListener(e -> Log.e("FIREBASE", "Failed to log answer", e));
    }

    private void showResultsDialog() {
        double percentCorrect = totalAnswered == 0 ? 0 : (correctCount * 100.0) / totalAnswered;
        String message = "Match Complete!\n\n" +
                "Answered: " + totalAnswered + "\n" +
                "Correct: " + correctCount + "\n" +
                "Accuracy: " + String.format(Locale.getDefault(), "%.1f", percentCorrect) + "%";

        new AlertDialog.Builder(this)
                .setTitle("Results")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> goToMainWithResults())
                .show();
    }

    private void goToMainWithResults() {
        Intent intent = new Intent(GameActivity.this, MainActivity.class);
        intent.putExtra("correct", correctCount);
        intent.putExtra("total", totalAnswered);
        startActivity(intent);
        finish();
    }

    public interface AnswerCallback {
        void onResult(boolean isCorrect);
    }

    public static class AnswerData {
        public String questionId;
        public String question;
        public String userAnswer;
        public boolean correct;
        public long timeSpentMs;
        public String answeredAt;

        public AnswerData() {}

        public AnswerData(String questionId, String question, String userAnswer, boolean correct,
                          long timeSpentMs, String answeredAt) {
            this.questionId = questionId;
            this.question = question;
            this.userAnswer = userAnswer;
            this.correct = correct;
            this.timeSpentMs = timeSpentMs;
            this.answeredAt = answeredAt;
        }
    }
}
