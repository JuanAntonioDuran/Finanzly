package com.example.finanzly.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finanzly.R;
import com.example.finanzly.adapters.InvitationAdapter;
import com.example.finanzly.models.Invitation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class InvitationFragment extends Fragment {

    private RecyclerView recyclerReceived, recyclerSent;
    private TextView tvEmptyMessage;
    private Button btnReceived, btnSent;

    private List<Invitation> receivedInvitations = new ArrayList<>();
    private List<Invitation> sentInvitations = new ArrayList<>();

    private InvitationAdapter receivedAdapter, sentAdapter;

    private DatabaseReference invitationsRef;
    private String currentUserId;
    private boolean showingReceived = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_invitation, container, false);

        recyclerReceived = view.findViewById(R.id.recyclerReceivedInvitations);
        recyclerSent = view.findViewById(R.id.recyclerSentInvitations);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        btnReceived = view.findViewById(R.id.btnReceived);
        btnSent = view.findViewById(R.id.btnSent);

        recyclerReceived.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerSent.setLayoutManager(new LinearLayoutManager(getContext()));

        receivedAdapter = new InvitationAdapter(receivedInvitations, getContext(), true);
        sentAdapter = new InvitationAdapter(sentInvitations, getContext(), false);

        recyclerReceived.setAdapter(receivedAdapter);
        recyclerSent.setAdapter(sentAdapter);

        btnReceived.setOnClickListener(v -> showReceived());
        btnSent.setOnClickListener(v -> showSent());

        currentUserId = FirebaseAuth.getInstance().getUid();
        invitationsRef = FirebaseDatabase.getInstance().getReference("invitations");

        loadInvitations();
        setupInvitationActions();
        showReceived(); // Inicializamos mostrando recibidas

        return view;
    }

    //Muestra las invitaciones recibidas
    private void showReceived() {
        showingReceived = true;
        recyclerReceived.setVisibility(View.VISIBLE);
        recyclerSent.setVisibility(View.GONE);
        updateEmptyMessage();
        updateButtonColors();
    }

    //Muestra las invitaciones enviadas
    private void showSent() {
        showingReceived = false;
        recyclerReceived.setVisibility(View.GONE);
        recyclerSent.setVisibility(View.VISIBLE);
        updateEmptyMessage();
        updateButtonColors();
    }

    //Mostrar el mensaje vacio si no hay nada
    private void updateEmptyMessage() {
        if (showingReceived) {
            tvEmptyMessage.setVisibility(receivedInvitations.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            tvEmptyMessage.setVisibility(sentInvitations.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    //funcion para cambiar los botones
    private void updateButtonColors() {
        if (showingReceived) {
            btnReceived.setBackgroundColor(getResources().getColor(R.color.green_primary, null));
            btnReceived.setTextColor(Color.WHITE);
            btnSent.setBackgroundColor(getResources().getColor(R.color.gray_light, null));
            btnSent.setTextColor(Color.BLACK);
        } else {
            btnSent.setBackgroundColor(getResources().getColor(R.color.green_primary, null));
            btnSent.setTextColor(Color.WHITE);
            btnReceived.setBackgroundColor(getResources().getColor(R.color.gray_light, null));
            btnReceived.setTextColor(Color.BLACK);
        }
    }

    // Cargar las invitaciones
    private void loadInvitations() {
        if (currentUserId == null) return;

        invitationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                receivedInvitations.clear();
                sentInvitations.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Invitation inv = child.getValue(Invitation.class);
                    if (inv != null) {
                        inv.setId(child.getKey());

                        // Solo agregar a recibidas si es para el usuario actual
                        if (currentUserId.equals(inv.getToUserId())) receivedInvitations.add(inv);

                        // Solo agregar a enviadas si fue enviada por el usuario actual
                        if (currentUserId.equals(inv.getFromUserId())) sentInvitations.add(inv);
                    }
                }

                // Actualizamos los adapters
                receivedAdapter.notifyDataSetChanged();
                sentAdapter.notifyDataSetChanged();
                updateEmptyMessage();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error cargando invitaciones", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Cargar las invitaciones y sus acciones
    private void setupInvitationActions() {
        // Acciones para invitaciones recibidas
        receivedAdapter.setOnInvitationActionListener(new InvitationAdapter.OnInvitationActionListener() {
            @Override
            public void onAccept(Invitation invitation) {
                acceptInvitation(invitation);
            }

            @Override
            public void onDecline(Invitation invitation) {
                declineInvitation(invitation);
            }

            @Override
            public void onCancel(Invitation invitation) { /* No aplica */ }
        });

        // Acciones para invitaciones enviadas
        sentAdapter.setOnInvitationActionListener(new InvitationAdapter.OnInvitationActionListener() {
            @Override
            public void onAccept(Invitation invitation) { /* No aplica */ }

            @Override
            public void onDecline(Invitation invitation) { /* No aplica */ }

            @Override
            public void onCancel(Invitation invitation) {
                cancelInvitation(invitation);
            }
        });
    }


    //Botones para acceptar rechazar y borrar las invitaciones
    private void acceptInvitation(Invitation invitation) {
        DatabaseReference resourceRef = null;
        String userIdToAdd = invitation.getToUserId();

        if ("budget".equals(invitation.getResourceType())) {
            resourceRef = FirebaseDatabase.getInstance()
                    .getReference("budgets")
                    .child(invitation.getResourceIdBudget())
                    .child("sharedUserIds");
        } else if ("goal".equals(invitation.getResourceType())) {
            resourceRef = FirebaseDatabase.getInstance()
                    .getReference("goals")
                    .child(invitation.getResourceIdGoal())
                    .child("sharedUserIds");
        }

        if (resourceRef == null || userIdToAdd == null) return;

        resourceRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                List<String> sharedUsers = (List<String>) currentData.getValue();
                if (sharedUsers == null) sharedUsers = new ArrayList<>();
                if (!sharedUsers.contains(userIdToAdd)) sharedUsers.add(userIdToAdd);
                currentData.setValue(sharedUsers);
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (committed) {
                    deleteInvitation(invitation);
                    Toast.makeText(getContext(), "Usuario agregado correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Error al agregar usuario", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void declineInvitation(Invitation invitation) {
        deleteInvitation(invitation);
        Toast.makeText(getContext(), "Invitación rechazada", Toast.LENGTH_SHORT).show();
    }

    private void cancelInvitation(Invitation invitation) {
        deleteInvitation(invitation);
        Toast.makeText(getContext(), "Invitación cancelada", Toast.LENGTH_SHORT).show();
    }

    private void deleteInvitation(Invitation invitation) {
        if (invitation.getId() != null) {
            invitationsRef.child(invitation.getId()).removeValue();
        }
    }
}
