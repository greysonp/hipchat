package com.stupid.hipchat;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * A chat fragment containing messages view and input form.
 */
public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";

    private static final int NOTIFICATION_ID = 1;

    private static final int REQUEST_LOGIN = 0;

    private static final int TYPING_TIMER_LENGTH = 600;

    private SensorManager mSensorManager;
    private SensorEventListener mShakira;
    private SensorEventListener mShakiraJump;
    private NotificationManager mNotificationManager;
    private Vibrator mVibrator;
    private Timer mSensorLockTimer;
    private boolean mSensorLocked = false;
    private TextToSpeech mTextToSpeech;

    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private ViewGroup mInputMorseView;
    private List<Message> mMessages = new ArrayList<Message>();
    private RecyclerView.Adapter mAdapter;
    private boolean mTyping = false;
    private Handler mTypingHandler = new Handler();
    private String mUsername;
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://hipsdontlie.herokuapp.com");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public MainFragment() {
        super();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAdapter = new MessageAdapter(activity, mMessages);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        mSensorLockTimer = new Timer();

        mShakira = new Shakira();
        mShakiraJump = new ShakiraJump();

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        setHasOptionsMenu(true);

        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("new message", onNewMessage);
        mSocket.on("user joined", onUserJoined);
        mSocket.on("user left", onUserLeft);
        mSocket.on("typing", onTyping);
        mSocket.on("stop typing", onStopTyping);
        mSocket.connect();

        startSignIn();

        mTextToSpeech = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Assume success for the hackathon
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("new message", onNewMessage);
        mSocket.off("user joined", onUserJoined);
        mSocket.off("user left", onUserLeft);
        mSocket.off("typing", onTyping);
        mSocket.off("stop typing", onStopTyping);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessagesView = (RecyclerView) view.findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mMessagesView.setAdapter(mAdapter);

        mInputMessageView = (EditText) view.findViewById(R.id.message_input);
        mInputMessageView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.id.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    return true;
                }
                return false;
            }
        });
        mInputMessageView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == mUsername) return;
                if (!mSocket.connected()) return;

                if (!mTyping) {
                    mTyping = true;
                    mSocket.emit("typing");
                }

                mTypingHandler.removeCallbacks(onTypingTimeout);
                mTypingHandler.postDelayed(onTypingTimeout, TYPING_TIMER_LENGTH);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mInputMorseView = (ViewGroup) view.findViewById(R.id.chat_morse);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Activity.RESULT_OK != resultCode) {
            getActivity().finish();
            return;
        }

        mUsername = data.getStringExtra("username");
        int numUsers = data.getIntExtra("numUsers", 1);

        addLog(getResources().getString(R.string.message_welcome));
        addParticipantsLog(numUsers);

        // Init sensors
        mSensorLockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mSensorManager.registerListener(mShakira, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
                mSensorManager.registerListener(mShakiraJump, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
                mTextToSpeech.speak("Shake it!", TextToSpeech.QUEUE_ADD, null);
            }
        }, 5000);

    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mShakira);
        mSensorManager.unregisterListener(mShakiraJump);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_leave) {
            leave();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addLog(String message) {
        mMessages.add(new Message.Builder(Message.TYPE_LOG)
                .message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addParticipantsLog(int numUsers) {
        addLog(getResources().getQuantityString(R.plurals.message_participants, numUsers, numUsers));
    }

    private void addMessage(String username, String message) {
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .username(username).message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void addTyping(String username) {
        mMessages.add(new Message.Builder(Message.TYPE_ACTION)
                .username(username).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void removeTyping(String username) {
        for (int i = mMessages.size() - 1; i >= 0; i--) {
            Message message = mMessages.get(i);
            if (message.getType() == Message.TYPE_ACTION && message.getUsername().equals(username)) {
                mMessages.remove(i);
                mAdapter.notifyItemRemoved(i);
            }
        }
    }

    private boolean attemptSend() {
        if (null == mUsername) return false;
        if (!mSocket.connected()) return false;

        mTyping = false;

        String message = mInputMessageView.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            mInputMessageView.requestFocus();
            return false;
        }

        mInputMessageView.setText("");
        addMessage(mUsername, message);

        // perform the sending message attempt.
        mSocket.emit("new message", message);

        return true;
    }

    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void leave() {
        mUsername = null;
        mSocket.disconnect();
        mSocket.connect();
        startSignIn();
    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getApplicationContext(),
                            R.string.error_connect, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            final String username;
            final String message;
            try {
                username = data.getString("username");
                message = data.getString("message");
            } catch (JSONException e) {
                return;
            }

            if (!mUsername.equalsIgnoreCase(username)) {
                Activity activity = getActivity();
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(activity)
                                .setSmallIcon(R.drawable.logo)
                                .setContentTitle("New shake from " + username)
                                .setContentText(message);

                mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    removeTyping(username);
                    addMessage(username, message);
                }
            });
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_joined, username));
                    addParticipantsLog(numUsers);
                }
            });
        }
    };

    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    int numUsers;
                    try {
                        username = data.getString("username");
                        numUsers = data.getInt("numUsers");
                    } catch (JSONException e) {
                        return;
                    }

                    addLog(getResources().getString(R.string.message_user_left, username));
                    addParticipantsLog(numUsers);
                    removeTyping(username);
                }
            });
        }
    };

    private Emitter.Listener onTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }
                    addTyping(username);
                }
            });
        }
    };

    private Emitter.Listener onStopTyping = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String username;
                    try {
                        username = data.getString("username");
                    } catch (JSONException e) {
                        return;
                    }
                    removeTyping(username);
                }
            });
        }
    };

    private Runnable onTypingTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mTyping) return;

            mTyping = false;
            mSocket.emit("stop typing");
        }
    };

    private void startLock(int duration) {
        mSensorLocked = true;
        mSensorLockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mSensorLocked = false;
            }
        }, duration);
    }

    private ImageView createMorseImage(boolean isDash) {
        ImageView morseImage = new ImageView(getActivity());
        Resources r = getResources();
        int paddingPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, r.getDisplayMetrics());
        morseImage.setPadding(paddingPixels, 0, paddingPixels, 0);
        if (isDash) {
            morseImage.setImageResource(R.drawable.dash);
        } else {
            morseImage.setImageResource(R.drawable.dot);
        }
        return morseImage;
    }

    private void addMorse(char morse) {
        boolean isDash = morse == '-';
        mInputMorseView.addView(createMorseImage(isDash));
    }

    private void clearMorse() {
        mInputMorseView.removeAllViews();
    }

    private void playDing() {
        MediaPlayer mp = MediaPlayer.create(getActivity(), R.raw.ding2);
        mp.start();
    }

    private class Shakira implements SensorEventListener{

        private float x = 0;
        private float y = 0;
        private MorseCodeTranslator translator;

        private static final float SIDE_THRESHOLD = 1.25f;
        private static final float BACK_THRESHOLD = 1f;

        private static final int SIDE_DURATION = 750;
        private static final int BACK_DURATION = 1000;


        public Shakira() {
            translator = new MorseCodeTranslator();
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            x = event.values[0];
            y = event.values[1];

            // Do stuff
            if (y > SIDE_THRESHOLD && !mSensorLocked) {
                shakeRight();
            } else if (y < -SIDE_THRESHOLD && !mSensorLocked) {
                shakeLeft();
            } else if (x > BACK_THRESHOLD && !mSensorLocked) {
                backThrust();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do nothing
        }

        private void shakeRight() {
            Log.d(TAG, "-");
            translator.collect('-');
            mVibrator.vibrate(500);
            notifyCollection();
            startLock(SIDE_DURATION);
        }

        private void shakeLeft() {
            Log.d(TAG, ".");
            translator.collect('.');
            mVibrator.vibrate(250);
            notifyCollection();
            startLock(SIDE_DURATION);
        }

        private void backThrust() {
            String letter = translator.getTranslation(true);
            Log.d(TAG, letter == null ? "null" : letter);
            if (letter != null) {
                mInputMessageView.append(letter);
                mTextToSpeech.speak(letter, TextToSpeech.QUEUE_ADD, null);
                mVibrator.vibrate(new long[] {0, 100, 100, 100}, -1);
                clearMorse();
            }
            startLock(BACK_DURATION);
        }

        private void notifyCollection() {
            String morseCollection = translator.getMorseCollection();
            addMorse(morseCollection.charAt(morseCollection.length() - 1));
        }
    }

    private class ShakiraJump implements SensorEventListener {

        private static final int JUMP_LOCK_DURATION = 1500;
        private float lastY = 0f;

        @Override
        public void onSensorChanged(SensorEvent event) {
            float y = event.values[1];
            float yDelta = Math.abs(y - lastY);

            // The accelerometer takes gravity into account, so if we have a near-zero acceleration in the y direction,
            // we must be near the precipice of a jump
            if (y < 1 && yDelta > 0.15f && !mSensorLocked) {
                Log.d(TAG, "SEND, " + y);
                clearMorse();
                boolean sent = attemptSend();
                if (sent) {
                    playDing();
                } else {
                    mVibrator.vibrate(1000);
                }
                startLock(JUMP_LOCK_DURATION);
            }

            lastY = y;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do nothing
        }
    }
}

