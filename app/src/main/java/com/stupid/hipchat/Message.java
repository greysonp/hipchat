package com.stupid.hipchat;

public class Message {

    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_ACTION = 2;

    private int mType;
    private String mMessage;
    private String mUsername;
    private boolean mFromSelf;

    private Message() {}

    public int getType() {
        return mType;
    };

    public String getMessage() {
        return mMessage;
    };

    public String getUsername() {
        return mUsername;
    };

    public boolean isFromSelf() { return mFromSelf; };


    public static class Builder {
        private final int mType;
        private String mUsername;
        private String mMessage;
        private boolean mFromSelf;

        public Builder(int type) {
            mType = type;
        }

        public Builder username(String username, boolean fromSelf) {
            mUsername = username;
            mFromSelf = fromSelf;
            return this;
        }

        public Builder message(String message) {
            mMessage = message;
            return this;
        }

        public Message build() {
            Message message = new Message();
            message.mType = mType;
            message.mUsername = mUsername;
            message.mFromSelf = mFromSelf;
            message.mMessage = mMessage;
            return message;
        }
    }
}
