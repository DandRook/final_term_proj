# Task 1 & 2: Swipe-Enabled Quiz App

##Overview
This project is a swipe-enabled quiz application built for Android using Java. The app allows users to navigate through quiz questions by swiping left or right. 
It also integrates with Firebase Realtime Database to track user interactions.

###Workflow
The app presents quiz questions to the user.
Users swipe right to move to the next question and swipe left to return to the previous question.
Each swipe action is logged into Firebase under the user's account.

###Swipe Gesture Implementation
Implemented using Android’s GestureDetector.
Detects horizontal swipes (onFling method) to trigger next/previous question updates.
Logs each swipe with a timestamp in Firebase for tracking.
Data Captured for Each Swipe

Each swipe logs the following information:
Action: Swiped Right or Swiped Left
Question: The question being viewed at the time of swipe.
Timestamp: When the swipe occurred.

Firebase Realtime Database Structure:
user_data/
  ├── admin@gmail_com/
      ├── swipes/
          ├── -Nabc123xyz/
              ├── action: "Swiped Right"
              ├── question: "What is the capital of France?"
              ├── timestamp: 1700000000000

###Challenges Encountered
Swipe Gesture Handling
Ensuring smooth gesture detection without misinterpretation of simple taps.
Debugging swipe detection required Logcat output monitoring.

###Firebase Integration
Ensuring data is stored in real-time without overwriting.
Configuring Firebase rules correctly to allow data writes.
Handling null values for user authentication when retrieving stored swipes.

###Testing Instructions
Testing Swipe Functionality
Open the app and login with:
Email: admin@gmail.com
Password: 123456
Swipe right to move to the next question.
Swipe left to return to the previous question.
Check that questions update accordingly.

###Testing Firebase Integration
Open Firebase Console → Go to Realtime Database.
(link to real time DB [https://console.firebase.google.com/project/term-project-1b45b/database/term-project-1b45b-default-rtdb/data](url))
Navigate to user_data/{your_username}/swipes.
Swipe in the app and refresh the database.
Verify that a new swipe entry appears with the correct:
action
question
timestamp

###Contributors
[Daniel Rook], [Lauren Sawvel]
