package com.salah.app.models;

/** A single adhkar item (text + count + optional online audio URL). */
public class DhikrItem {
    public int id;
    public String title;
    public String text;
    public int count;
    public String audioUrl;

    public DhikrItem() {}
    public DhikrItem(int id, String title, String text, int count, String audioUrl) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.count = count;
        this.audioUrl = audioUrl;
    }
}
