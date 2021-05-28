package com.mechat.app.model;

public class User {
    private String userName, email, userId, profilePic, lastMessage;

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public User(String userName, String email, String userId, String profilePic) {
        this.userName = userName;
        this.email = email;
        this.userId = userId;
        this.profilePic = profilePic;
    }

    public User(String userName, String email, String userId, String profilePic, String lastMessage) {
        this.userName = userName;
        this.email = email;
        this.userId = userId;
        this.profilePic = profilePic;
        this.lastMessage = lastMessage;
    }

    public User() {

    }

    public User(String userId, String lastMessage) {
        this.userId = userId;
        this.lastMessage = lastMessage;
    }

    public User(String userName, String email, String userId) {
        this.userName = userName;
        this.email = email;
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }
}
