package com.example.finanzly.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        void onLeave(Reminder reminder);
    }

    private final Context context;
    private final List<Reminder> items;
    private final Map<String, List<String>> sharedUsersMap;
    private final OnReminderActionListener listener;
    private final SimpleDateFormat isoDateTime;
    private final String currentUserId;

    public ReminderAdapter(Context context, List<Reminder> items,
                           Map<String, List<String>> sharedUsersMap,
                           OnReminderActionListener listener,
                           String currentUserId) {
        this.context = context;
        this.items = items;
        this.sharedUsersMap = sharedUsersMap;
        this.listener = listener;
        this.currentUserId = currentUserId;
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
        holder.txtTitle.setText(!TextUtils.isEmpty(r.getTitle()) ? r.getTitle() : "Sin título");

        // ---- DESCRIPCIÓN ----
        holder.txtDescription.setText(!TextUtils.isEmpty(r.getDescription()) ? r.getDescription() : "Sin descripción");

        // ---- FECHA ----
        holder.txtDate.setText(r.getDate() != null ? r.getDate() : "—");

        // ---- HORA ----
        holder.txtTime.setText(r.getTime() != null ? r.getTime() : "");

        // ---- USUARIOS COMPARTIDOS ----
        List<String> names = sharedUsersMap.get(r.getId());
        if (names != null && !names.isEmpty()) {
            holder.txtSharedCount.setVisibility(View.VISIBLE);
            holder.txtSharedCount.setText(TextUtils.join(", ", names));
        } else {
            holder.txtSharedCount.setVisibility(View.GONE);
        }

        // ---- ROLES ----
        boolean isCreator = currentUserId != null && currentUserId.equals(r.getUserId());
        boolean isSharedUser = r.getSharedUserIds() != null &&
                currentUserId != null &&
                r.getSharedUserIds().contains(currentUserId);

        // ---- ESTADO DEL USUARIO EN ESTE REMINDER ----
        boolean isCompletedByUser = false;

        Map<String, Boolean> sharedStatus = r.getSharedUsersStatus();
        if (sharedStatus != null && sharedStatus.containsKey(currentUserId)) {
            isCompletedByUser = sharedStatus.get(currentUserId);
        }

        // ---- BOTONES DEL CREADOR ----
        holder.btnEdit.setVisibility(isCreator ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(isCreator ? View.VISIBLE : View.GONE);

        // ---- BOTÓN SALIR (solo shared users, no creator) ----
        holder.btnLeave.setVisibility(!isCreator && isSharedUser ? View.VISIBLE : View.GONE);

        // ---- CONTROL DE BOTONES COMPLETE/PENDING ----
        if (isSharedUser) {
            if (isCompletedByUser) {
                holder.btnComplete.setVisibility(View.GONE);
                holder.btnPending.setVisibility(View.VISIBLE);
            } else {
                holder.btnComplete.setVisibility(View.VISIBLE);
                holder.btnPending.setVisibility(View.GONE);
            }
        } else {
            holder.btnComplete.setVisibility(View.GONE);
            holder.btnPending.setVisibility(View.GONE);
        }

        // ---- ESTADO GENERAL DEL REMINDER ----
        boolean isCompleted = r.getIsCompleted();
        boolean isExpired = computeExpired(r);

        if (isCompleted) {
            holder.txtStatus.setText("Completado");
            holder.txtStatus.setTextColor(context.getResources().getColor(R.color.green_light));
        } else if (isExpired) {
            holder.txtStatus.setText("Expirado");
            holder.txtStatus.setTextColor(context.getResources().getColor(R.color.red_error));
        } else {
            holder.txtStatus.setText("Pendiente");
            holder.txtStatus.setTextColor(context.getResources().getColor(R.color.yellow));
        }

        // ---- LISTENERS ----
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(r));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(r));
        holder.btnLeave.setOnClickListener(v -> listener.onLeave(r));

        // Ambos botones usan el toggle
        holder.btnComplete.setOnClickListener(v -> listener.onToggleComplete(r));
        holder.btnPending.setOnClickListener(v -> listener.onToggleComplete(r));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private boolean computeExpired(Reminder r) {
        if (r.getDate() == null || r.getTime() == null) return false;

        try {
            Date target = isoDateTime.parse(r.getDate() + "T" + r.getTime());
            return new Date().after(target);
        } catch (ParseException e) {
            return false;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDescription, txtDate, txtTime, txtSharedCount, txtStatus; // <-- añadimos txtStatus
        Button btnEdit, btnDelete, btnComplete, btnPending, btnLeave;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtReminderTitle);
            txtDescription = itemView.findViewById(R.id.txtReminderDescription);
            txtDate = itemView.findViewById(R.id.txtReminderDate);
            txtTime = itemView.findViewById(R.id.txtReminderTime);
            txtSharedCount = itemView.findViewById(R.id.txtSharedCount);
            txtStatus = itemView.findViewById(R.id.txtReminderStatus); // <-- enlazamos TextView

            btnEdit = itemView.findViewById(R.id.btnEditReminder);
            btnDelete = itemView.findViewById(R.id.btnDeleteReminder);
            btnComplete = itemView.findViewById(R.id.btnCompleteReminder);
            btnPending = itemView.findViewById(R.id.btnPendingReminder);
            btnLeave = itemView.findViewById(R.id.btnLeaveReminder);
        }
    }


    public void updateList(List<Reminder> newList) {
        items.clear();
        items.addAll(newList);
        notifyDataSetChanged();
    }
}
