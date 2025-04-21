package com.example.term_project;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GameActivity extends Activity {

    private TextView questionView;
    private Button correctBtn, wrongBtn;

    private int correctAnswers = 0;
    private int totalQuestions = 0;
    private long startTime;

    private String question;
    private String questionId;
    private String uId;

    private DatabaseReference databaseReference;
    // TODO make it properly wait for other player & cycle through questions after clicking on an answer
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gameact);

        questionView = findViewById(R.id.question_view);
        correctBtn = findViewById(R.id.correct_button);
        wrongBtn = findViewById(R.id.wrong_button);

        question = getIntent().getStringExtra("question");
        questionId = getIntent().getStringExtra("questionId");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uId = user.getUid();

        databaseReference = FirebaseDatabase.getInstance().getReference("user_data");

        questionView.setText(question);
        startTime = System.currentTimeMillis();

        correctBtn.setOnClickListener(v -> recordAnswer(true));
        wrongBtn.setOnClickListener(v -> recordAnswer(false));
    }

    private void recordAnswer(boolean isCorrect) {
        totalQuestions++;
        if (isCorrect) correctAnswers++;

        long timeSpent = System.currentTimeMillis() - startTime;
        logAnswerToFirebase(isCorrect, timeSpent);

        new Handler().postDelayed(() -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("correctAnswers", correctAnswers);
            resultIntent.putExtra("totalQuestions", totalQuestions);
            setResult(RESULT_OK, resultIntent);
            finish();
        }, 1000);
    }

    private void logAnswerToFirebase(boolean isCorrect, long timeSpent) {
        if (uId == null || uId.isEmpty()) return;

        DatabaseReference userRef = databaseReference.child(uId).child("answers").push();

        userRef.setValue(new AnswerRecord(
                        questionId,
                        question,
                        isCorrect,
                        timeSpent,
                        getFormattedTimestamp(System.currentTimeMillis())
                )).addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Answer logged!"))
                .addOnFailureListener(e -> Log.e("FIREBASE", "Failed to log answer", e));
    }

    private String getFormattedTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static class AnswerRecord {
        public String questionId;
        public String question;
        public boolean correct;
        public long timeSpentMs;
        public String answeredAt;

        public AnswerRecord() {}

        public AnswerRecord(String questionId, String question, boolean correct, long timeSpentMs, String answeredAt) {
            this.questionId = questionId;
            this.question = question;
            this.correct = correct;
            this.timeSpentMs = timeSpentMs;
            this.answeredAt = answeredAt;
        }
    }
}
