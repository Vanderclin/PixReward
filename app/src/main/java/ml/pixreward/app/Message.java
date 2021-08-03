package ml.pixreward.app;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Message {
    private String id;
    private String name;
    private String message;
    private String time;
    private String uid;

    public Message() {
        //this constructor is required
    }

    public Message(String id, String name, String message, String time, String uid) {
        this.id = id;
        this.name = name;
        this.message = message;
        this.time = time;
        this.uid = uid;
    }

    public String getID() {
        return id;
    }

    public String getNAME() {
        return name;
    }

    public String getMESSAGE() {
        return message;
    }

    public String getTIME() {
        return time;
    }

    public String getUID() {
        return uid;
    }
}
