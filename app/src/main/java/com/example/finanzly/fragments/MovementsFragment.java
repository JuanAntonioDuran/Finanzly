package com.example.finanzly.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.finanzly.R;
import com.example.finanzly.adapters.MovementFragmentAdapter;
import com.example.finanzly.models.Movement;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MovementsFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovementFragmentAdapter adapter;
    private Button btnApplyFilter, btnClearFilter;
    private Spinner spinnerTypeFilter;
    private TextView tvEmptyState;
    private EditText etCategoryFilter, etStartDateFilter, etEndDateFilter;
    private List<Movement> movementList = new ArrayList<>();
    private List<Movement> filteredList = new ArrayList<>();

    private DatabaseReference movementsRef;
    private String uid;

    public MovementsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_movements, container, false);

        uid = FirebaseAuth.getInstance().getUid();
        movementsRef = FirebaseDatabase.getInstance().getReference("movements");

        // Views
        recyclerView = view.findViewById(R.id.recyclerViewMovements);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);
        btnClearFilter = view.findViewById(R.id.btnClearFilter);
        spinnerTypeFilter = view.findViewById(R.id.spinnerTypeFilter);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        etCategoryFilter = view.findViewById(R.id.etCategoryFilter);
        etStartDateFilter = view.findViewById(R.id.etStartDateFilter);
        etEndDateFilter = view.findViewById(R.id.etEndDateFilter);

        // DatePickers
        etStartDateFilter.setOnClickListener(v -> showDatePicker(etStartDateFilter));
        etEndDateFilter.setOnClickListener(v -> showDatePicker(etEndDateFilter));

        // Spinner de filtro
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Todos", "Ingreso", "Gasto"}
        );
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(filterAdapter);

        // RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MovementFragmentAdapter(getContext(), filteredList);
        recyclerView.setAdapter(adapter);

        // Botones de filtro
        btnApplyFilter.setOnClickListener(v -> applyFilter());
        btnClearFilter.setOnClickListener(v -> clearFilter());

        // Carga inicial de movimientos
        loadMovements();

        return view;
    }

    private void loadMovements() {
        movementsRef.orderByChild("userId").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        movementList.clear();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Movement m = child.getValue(Movement.class);
                            if (m != null) {
                                m.setId(child.getKey());
                                movementList.add(m);
                            }
                        }

                        // Inicialmente mostramos todos
                        filteredList.clear();
                        filteredList.addAll(movementList);
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void applyFilter() {
        filteredList.clear();

        String categoryFilter = etCategoryFilter.getText().toString().trim().toLowerCase();
        String typeFilter = spinnerTypeFilter.getSelectedItem().toString().trim().toLowerCase();
        String startDate = etStartDateFilter.getText().toString().trim();
        String endDate = etEndDateFilter.getText().toString().trim();

        for (Movement m : movementList) {

            // 🔹 Filtro por categoría
            boolean matchesCategory = categoryFilter.isEmpty() ||
                    (m.getCategory() != null && m.getCategory().toLowerCase().contains(categoryFilter));

            // 🔹 Filtro por tipo (Gasto / Ingreso)
            boolean matchesType = true;

            if (!typeFilter.equals("todos")) {
                String movementType = m.getType() != null ? m.getType().toLowerCase() : "";

                if (typeFilter.contains("gasto")) {
                    matchesType = movementType.equals("expense");
                } else if (typeFilter.contains("ingreso")) {
                    matchesType = movementType.equals("income");
                } else {
                    matchesType = movementType.equals(typeFilter);
                }
            }

            // 🔹 Filtro por fecha
            boolean matchesDate = true;

            if (!startDate.isEmpty() || !endDate.isEmpty()) {
                String movementDate = m.getDate();
                if (movementDate != null) {
                    if (!startDate.isEmpty() && movementDate.compareTo(startDate) < 0) matchesDate = false;
                    if (!endDate.isEmpty() && movementDate.compareTo(endDate) > 0) matchesDate = false;
                } else {
                    matchesDate = false;
                }
            }

            // ✅ Si cumple todo
            if (matchesCategory && matchesType && matchesDate) {
                filteredList.add(m);
            }
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }



    private void clearFilter() {
        etCategoryFilter.setText("");
        spinnerTypeFilter.setSelection(0);
        etStartDateFilter.setText("");
        etEndDateFilter.setText("");

        filteredList.clear();
        filteredList.addAll(movementList);
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        try {
            String[] parts = target.getText().toString().split("-");
            if (parts.length == 3) {
                calendar.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            }
        } catch (Exception ignored) {}

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    target.setText(formatted);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }
}
