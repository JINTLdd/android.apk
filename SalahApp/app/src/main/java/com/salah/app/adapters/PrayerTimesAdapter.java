package com.salah.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.salah.app.R;
import com.salah.app.models.PrayerTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrayerTimesAdapter extends RecyclerView.Adapter<PrayerTimesAdapter.VH> {

    private final List<PrayerTime> items = new ArrayList<>();

    public void submit(List<PrayerTime> times) {
        items.clear();
        if (times != null) items.addAll(times);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_prayer_time, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PrayerTime p = items.get(position);
        h.txtName.setText(p.getArabicName());
        h.txtTime.setText(p.formatTime12h());
        // Highlight the next upcoming prayer (the first non-past item that is not Sunrise).
        boolean isNext = false;
        long now = System.currentTimeMillis();
        for (PrayerTime q : items) {
            if (q.prayer == PrayerTime.Prayer.SUNRISE) continue;
            if (q.epochMs() > now) { isNext = (q == p); break; }
        }
        h.itemView.setSelected(isNext);
        h.icon.setImageResource(iconFor(p.prayer));
    }

    private int iconFor(PrayerTime.Prayer p) {
        switch (p) {
            case FAJR:    return R.drawable.ic_fajr;
            case SUNRISE: return R.drawable.ic_sunrise;
            case DHUHR:   return R.drawable.ic_dhuhr;
            case ASR:     return R.drawable.ic_asr;
            case MAGHRIB: return R.drawable.ic_maghrib;
            case ISHA:    return R.drawable.ic_isha;
            default:      return R.drawable.ic_mosque;
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView txtName, txtTime;
        final ImageView icon;
        VH(@NonNull View v) {
            super(v);
            txtName = v.findViewById(R.id.txt_name);
            txtTime = v.findViewById(R.id.txt_time);
            icon = v.findViewById(R.id.icon);
        }
    }
}
