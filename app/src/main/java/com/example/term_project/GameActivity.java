package com.example.term_project;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class GameActivity extends AppCompatActivity {
    private static final String TAG = "GAME_ACTIVITY";

    private TextView questionText, timerText;
    private Button[] optionButtons;
    private Button nextButton;

    private String questionId, selectedAnswer = "";
    private int questionCount = 0;
    private long startTime;
    private final int TOTAL_GAME_TIME = 60000;

    private CountDownTimer gameTimer;
    private boolean gameTimerStarted = false;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private String gameMode = "single";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gameact);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("results");

        gameMode = getIntent().getStringExtra("mode");

        questionText = findViewById(R.id.question_text);
        timerText = findViewById(R.id.timer_text);
        nextButton = findViewById(R.id.next_question_button);

        optionButtons = new Button[]{
                findViewById(R.id.option1),
                findViewById(R.id.option2),
                findViewById(R.id.option3),
                findViewById(R.id.option4)
        };

        for (Button button : optionButtons) {
            button.setOnClickListener(v -> {
                for (Button b : optionButtons)
                    b.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                selectedAnswer = ((Button) v).getText().toString();
                nextButton.setEnabled(true);
            });
        }

        nextButton.setOnClickListener(v -> finalizeAnswer());

        fetchNextQuestion();
    }

    private void fetchNextQuestion() {
        if (questionCount >= 100) {
            runOnUiThread(this::showResultsDialog);
            return;
        }

        new Thread(() -> {
            try {
                String userId = mAuth.getUid();
                URL url = new URL("https://final-term-proj.onrender.com/next-question?userId=" + userId + "&mode=" + gameMode);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = in.readLine();
                in.close();

                JSONObject json = new JSONObject(response);
                String status = json.getString("status");

                if (status.equals("waiting")) {
                    runOnUiThread(() -> {
                        questionText.setText("Waiting for opponent...");
                        timerText.setText("...");
                        nextButton.setEnabled(false);
                    });
                    new Handler(Looper.getMainLooper()).postDelayed(this::fetchNextQuestion, 2000);
                    return;
                }

                questionId = json.getString("questionId");
                String question = json.getString("question");
                JSONArray optsArray = json.getJSONArray("options");
                String[] options = new String[4];
                for (int i = 0; i < 4; i++) {
                    options[i] = i < optsArray.length() ? optsArray.getString(i) : "";
                }

                startTime = System.currentTimeMillis();

                runOnUiThread(() -> {
                    questionText.setText(question);
                    for (int i = 0; i < optionButtons.length; i++) {
                        optionButtons[i].setVisibility(options[i].isEmpty() ? View.GONE : View.VISIBLE);
                        optionButtons[i].setText(options[i]);
                        optionButtons[i].setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    }
                    selectedAnswer = "";
                    nextButton.setEnabled(false);

                    if (!gameTimerStarted) {
                        startGameTimer();
                        gameTimerStarted = true;
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching question", e);
            }
        }).start();
    }

    private void finalizeAnswer() {
        if (selectedAnswer.isEmpty()) return;

        questionCount++;
        long timeSpent = (System.currentTimeMillis() - startTime) / 1000;
        String userId = mAuth.getUid();

        DatabaseReference userRef = dbRef.child(userId).push();
        userRef.setValue(new QuestionLog(questionId, selectedAnswer, timeSpent));

        sendAnswerToServer(userId, questionId, selectedAnswer);

        fetchNextQuestion();
    }

    private void sendAnswerToServer(String userId, String qid, String answer) {
        new Thread(() -> {
            try {
                URL url = new URL("https://final-term-proj.onrender.com/submit-answer");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("userId", userId);
                body.put("questionId", qid);
                body.put("userAnswer", answer);
                body.put("mode", gameMode);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                Log.d(TAG, "Submit HTTP code: " + code);
                if (code >= 200 && code < 300) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String result = in.readLine();
                    in.close();
                    Log.d(TAG, "submit-answer response: " + result);
                } else {
                    Log.e(TAG, "Submit failed with HTTP code: " + code);
                }

            } catch (Exception e) {
                Log.e(TAG, "Submit error", e);
            }
        }).start();
    }

    private void showResultsDialog() {
        if (isFinishing()) return;

        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("You answered " + questionCount + " questions.")
                .setPositiveButton("OK", (dialog, which) -> {
                    startActivity(new Intent(GameActivity.this, MainActivity.class));
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void startGameTimer() {
        gameTimer = new CountDownTimer(TOTAL_GAME_TIME, 1000) {
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.format(Locale.getDefault(), "%ds", millisUntilFinished / 1000));
            }

            public void onFinish() {
                showResultsDialog();
            }
        };
        gameTimer.start();
    }

    static class QuestionLog {
        public String questionId, selected;
        public long timeSpent;

        public QuestionLog() {}

        public QuestionLog(String questionId, String selected, long timeSpent) {
            this.questionId = questionId;
            this.selected = selected;
            this.timeSpent = timeSpent;
        }
    }
}
