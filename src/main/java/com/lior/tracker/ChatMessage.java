package com.lior.tracker;

public class ChatMessage {
    private String role;
    private String text;
    private String inline_data;
    private String mimeType;
    private String filename;
    public ChatMessage() {}
    public ChatMessage(String role, String text) {
        this.role = role;
        this.text = text;
    }
    public ChatMessage(String role, String text, String fileText, String mimeType, String filename) {
        this.role = role;
        this.text = text;
        this.inline_data = fileText;
        this.mimeType = mimeType;
        this.filename = filename;
    }
    public String getRole() {return role;}
    public String getText() {return text;}
    public String getInline_data() {
        if(this.inline_data != null) return inline_data;
        return null;
    }
    public String getMimeType() {return mimeType;}
    public String getFilename() {return filename;}
}
