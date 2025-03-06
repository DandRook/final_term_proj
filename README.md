## Swipe-Enabled Quiz App

### Overview
This project is a swipe-enabled quiz application built for Android using Java. The app allows users to navigate through quiz questions by swiping left or right. It also integrates with Firebase Realtime Database to track user interactions, capturing a variety of swipe-related data.

### Workflow
The app presents quiz questions to the user. Users swipe right to move to the next question and swipe left to return to the previous question. Each swipe action, along with additional tracking data, is logged into Firebase under the user's account.

### Swipe Gesture Implementation
- Implemented using Android’s GestureDetector.
- Detects horizontal swipes (onFling method) to trigger next/previous question updates.
- Logs each swipe with a timestamp and various other metrics in Firebase for tracking.

### Data Captured for Each Swipe
Each swipe logs the following information:
- **Action:** Swiped Right or Swiped Left
- **Question:** The question being viewed at the time of swipe.
- **Timestamp:** When the swipe occurred.
- **Swipe Velocity (X & Y):** The speed of the swipe in both horizontal and vertical directions.
- **Swipe Duration:** The time taken from touch start to swipe completion.
- **Start Position (X & Y):** The exact touch-down position of the swipe.
- **End Position (X & Y):** The exact lift-off position of the swipe.
- **Touch Pressure:** The force applied on the screen during the swipe.

### Firebase Realtime Database Structure
```
user_data/
├── admin@gmail_com/
    ├── swipes/
        ├── -Nabc123xyz/
            ├── action: "Swiped Right"
            ├── question: "What is the capital of France?"
            ├── timestamp: 1700000000000
            ├── velocityX: 500.0
            ├── velocityY: 200.0
            ├── duration: 350
            ├── startX: 100.0
            ├── startY: 200.0
            ├── endX: 400.0
            ├── endY: 200.0
            ├── pressure: 0.5
```

### Challenges Encountered
#### Swipe Gesture Handling
- Ensuring smooth gesture detection without misinterpreting simple taps.
- Debugging swipe detection required Logcat output monitoring.

#### Firebase Integration
- Ensuring data is stored in real-time without overwriting.
- Configuring Firebase rules correctly to allow data writes.
- Handling null values for user authentication when retrieving stored swipes.

### Testing Instructions
#### Testing Swipe Functionality
1. Open the app and login with:
   - **Email:** admin@gmail.com
   - **Password:** 123456
2. Swipe right to move to the next question.
3. Swipe left to return to the previous question.
4. Check that questions update accordingly.

#### Testing Firebase Integration
1. Open Firebase Console → Go to Realtime Database.
   - [Firebase Database Link](https://console.firebase.google.com/project/term-project-1b45b/database/term-project-1b45b-default-rtdb/data)
2. Navigate to `user_data/{your_username}/swipes`.
3. Swipe in the app and refresh the database.
4. Verify that a new swipe entry appears with the correct:
   - Action
   - Question
   - Timestamp
   - VelocityX & VelocityY
   - Duration
   - Start & End Position
   - Touch Pressure

### Contributors
- **Daniel Rook**
- **Lauren Sawvel**

