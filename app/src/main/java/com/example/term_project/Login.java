package com.example.term_project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Objects;

public class Login extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        editTextEmail = findViewById(R.id.editTextUsername); // Change ID to Email if using Email-based login
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister); // Button to navigate to registration

        // Login button click event
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUser();
            }
        });

        // Register button click event
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login.this, RegisterActivity.class));
            }
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Error checking
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            return;
        }


        // Firebase Authentication login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Login.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                            // Get username from email before the @
                            String username = email.substring(0, email.indexOf('@'));
                            Intent intent = new Intent(Login.this, MainActivity.class);
                            intent.putExtra("Username", username);
                            startActivity(intent);

                            finish();
                        } else {
                            Toast.makeText(Login.this, "Login Failed! " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
