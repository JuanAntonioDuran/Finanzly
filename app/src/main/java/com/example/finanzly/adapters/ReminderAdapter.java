package com.example.finanzly.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.models.Reminder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {

    public interface OnReminderActionListener {
        void onEdit(Reminder reminder);

        void onDelete(Reminder reminder);

        void onToggleComplete(Reminder reminder);

        void onAddUsers(Reminder reminder);

        void onLeave(Reminder reminder);
    }

    private final Context context;
    private final List<Reminder> items;
    private final Map<String, List<String>> sharedUsersMap;
    private final OnReminderActionListener listener;
    private final SimpleDateFormat isoDateTime;

    public ReminderAdapter(Context context, List<Reminder> items,
                           Map<String, List<String>> sharedUsersMap,
                           OnReminderActionListener listener) {
        this.context = context;
        this.items = items;
        this.sharedUsersMap = sharedUsersMap;
        this.listener = listener;
        this.isoDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Reminder r = items.get(position);

        // ---- TÍTULO ----
        holder.txtTitle.setText(
                !TextUtils.isEmpty(r.getTitle()) ? r.getTitle() : "Sin título"
        );

        // ---- DESCRIPCIÓN ----
        holder.txtDescription.setText(
                !TextUtils.isEmpty(r.getDescription()) ? r.getDescription() : "Sin descripción"
        );

        // ---- FECHA ----
        holder.txtDate.setText(
                r.getDate() != null ? r.getDate() : "—"
        );

        // ---- HORA ----
        holder.txtTime.setText(
                r.getTime() != null ? r.getTime() : ""
        );

        // ---- USUARIOS COMPARTIDOS ----
        List<String> names = sharedUsersMap.get(r.getId());
        if (names != null && !names.isEmpty()) {
            holder.txtSharedCount.setVisibility(View.VISIBLE);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < names.size(); i++) {
                sb.append(names.get(i));
                if (i != names.size() - 1) sb.append(", ");
            }

            holder.txtSharedCount.setText(sb.toString());
        } else {
            holder.txtSharedCount.setVisibility(View.GONE);
        }

        // ---- EXPIRADO / COMPLETADO ----
        boolean expired = computeExpired(r);


        int colorBlack = ContextCompat.getColor(context, android.R.color.black);
        int colorGray = ContextCompat.getColor(context, android.R.color.darker_gray);
        int colorRed = ContextCompat.getColor(context, android.R.color.holo_red_dark);
        int colorGreen = ContextCompat.getColor(context, android.R.color.holo_green_dark);


        // ---- LISTENERS ----
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(r);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(r);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onToggleComplete(r);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private boolean computeExpired(Reminder r) {
        if (r.getDate() == null || r.getTime() == null) return false;

        try {
            Date target = isoDateTime.parse(r.getDate() + "T" + r.getTime());

        } catch (ParseException e) {
            return false;
        }
        return false;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDescription, txtDate, txtTime, txtSharedCount;
        Button btnEdit, btnDelete;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtReminderTitle);
            txtDescription = itemView.findViewById(R.id.txtReminderDescription);
            txtDate = itemView.findViewById(R.id.txtReminderDate);
            txtTime = itemView.findViewById(R.id.txtReminderTime);
            txtSharedCount = itemView.findViewById(R.id.txtSharedCount);

            btnEdit = itemView.findViewById(R.id.btnEditReminder);
            btnDelete = itemView.findViewById(R.id.btnDeleteReminder);
        }

    }

    public void updateList(List<Reminder> newList) {
        items.clear();
        items.addAll(newList);
        notifyDataSetChanged();
    }
}
