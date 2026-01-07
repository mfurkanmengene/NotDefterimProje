package com.example.mobilsistemlerproje;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText etNote, etSearch;
    private Spinner spCategory; // Ana ekranda kategori seçimi
    private Button btnPickDate, btnSave;
    private TextView tvSelectedDate;
    private ListView listView;

    private DBHelper dbHelper;
    private SimpleCursorAdapter adapter;
    private String selectedDate; // yyyy-MM-dd

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private String[] categories = new String[]{"Genel", "Is", "Okul", "Diger"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        etSearch = findViewById(R.id.etSearch);
        etNote = findViewById(R.id.etNote);
        spCategory = findViewById(R.id.spCategory);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnSave = findViewById(R.id.btnSave);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        listView = findViewById(R.id.listView);

        // Kategori spinner doldur
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories
        );
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        // Varsayılan: bugün
        selectedDate = sdf.format(Calendar.getInstance().getTime());
        tvSelectedDate.setText(selectedDate);

        // Tarih seçimi
        btnPickDate.setOnClickListener(v -> showDatePicker());

        // Kaydet
        btnSave.setOnClickListener(v -> saveNote());

        // Liste ve adapter
        setupListAndAdapter();

        // Liste item tıklama → Sil/Güncelle
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Cursor c = (Cursor) adapter.getItem(position);
            long noteId = c.getLong(c.getColumnIndexOrThrow(DBHelper.COL_ID));
            String noteText = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_TEXT));
            String noteDate = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_DATE));
            String noteCategory = c.getString(c.getColumnIndexOrThrow(DBHelper.COL_CATEGORY));
            showItemActionsDialog(noteId, noteText, noteDate, noteCategory);
        });

        // Arama
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void showDatePicker() {
        final Calendar cal = Calendar.getInstance();
        try {
            String[] parts = selectedDate.split("-");
            cal.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            cal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
        } catch (Exception ignored) {}

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth);
                    selectedDate = sdf.format(chosen.getTime());
                    tvSelectedDate.setText(selectedDate);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void saveNote() {
        String text = etNote.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Not metni boş olamaz!", Toast.LENGTH_SHORT).show();
            return;
        }
        String category = (String) spCategory.getSelectedItem();
        long rowId = dbHelper.insertNote(text, selectedDate, category);
        if (rowId != -1) {
            Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show();
            etNote.setText("");
            // tarihi bugüne çevir
            selectedDate = sdf.format(Calendar.getInstance().getTime());
            tvSelectedDate.setText(selectedDate);
            // aramayı temiz değilse, listeyi tüm notlarla yenile
            if (TextUtils.isEmpty(etSearch.getText().toString())) {
                refreshCursor();
            } else {
                performSearch(etSearch.getText().toString());
            }
        } else {
            Toast.makeText(this, "Kaydetme hatası!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListAndAdapter() {
        Cursor cursor = dbHelper.getAllNotes();
        adapter = new SimpleCursorAdapter(
                this,
                R.layout.item_note,
                cursor,
                new String[]{DBHelper.COL_TEXT, DBHelper.COL_DATE, DBHelper.COL_CATEGORY},
                new int[]{R.id.tvItemText, R.id.tvItemDate, R.id.tvItemCategory},
                0
        );
        listView.setAdapter(adapter);
    }

    private void refreshCursor() {
        Cursor newCursor = dbHelper.getAllNotes();
        adapter.changeCursor(newCursor);
    }

    private void performSearch(String q) {
        Cursor c = TextUtils.isEmpty(q) ? dbHelper.getAllNotes() : dbHelper.searchNotes(q);
        adapter.changeCursor(c);
    }

    private void showItemActionsDialog(long noteId, String noteText, String noteDate, String noteCategory) {
        String[] options = {"Sil", "Güncelle"};
        new AlertDialog.Builder(this)
                .setTitle("Seçenekler")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        int deleted = dbHelper.deleteNote(noteId);
                        if (deleted > 0) {
                            Toast.makeText(this, "Silindi", Toast.LENGTH_SHORT).show();
                            if (TextUtils.isEmpty(etSearch.getText().toString())) {
                                refreshCursor();
                            } else {
                                performSearch(etSearch.getText().toString());
                            }
                        } else {
                            Toast.makeText(this, "Silme hatası!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showUpdateDialog(noteId, noteText, noteDate, noteCategory);
                    }
                })
                .show();
    }

    private void showUpdateDialog(long noteId, String currentText, String currentDate, String currentCategory) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        final EditText etUpdateText = new EditText(this);
        etUpdateText.setHint("Not metni");
        etUpdateText.setText(currentText);

        final TextView tvUpdateDate = new TextView(this);
        tvUpdateDate.setText(currentDate);
        tvUpdateDate.setPadding(0, pad / 2, 0, pad / 2);

        final Button btnUpdatePickDate = new Button(this);
        btnUpdatePickDate.setText("Tarih Seç");

        // Kategori Spinner
        final Spinner spUpdateCategory = new Spinner(this);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories
        );
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUpdateCategory.setAdapter(catAdapter);
        int pos = 0;
        if ("Is".equals(currentCategory)) pos = 1;
        else if ("Okul".equals(currentCategory)) pos = 2;
        else if ("Diger".equals(currentCategory)) pos = 3;
        spUpdateCategory.setSelection(pos);

        container.addView(etUpdateText);
        container.addView(tvUpdateDate);
        container.addView(btnUpdatePickDate);
        container.addView(spUpdateCategory);

        final String[] selectedDateHolder = {currentDate};

        btnUpdatePickDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try {
                String[] parts = selectedDateHolder[0].split("-");
                cal.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                cal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            } catch (Exception ignored) {}

            DatePickerDialog dp = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, dayOfMonth);
                        String newDate = sdf.format(chosen.getTime());
                        selectedDateHolder[0] = newDate;
                        tvUpdateDate.setText(newDate);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            dp.show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Notu Güncelle")
                .setView(container)
                .setPositiveButton("Kaydet", (d, w) -> {
                    String newText = etUpdateText.getText().toString().trim();
                    String newDate = selectedDateHolder[0];
                    String newCategory = (String) spUpdateCategory.getSelectedItem();
                    if (TextUtils.isEmpty(newText)) {
                        Toast.makeText(this, "Not metni boş olamaz!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int updated = dbHelper.updateNote(noteId, newText, newDate, newCategory);
                    if (updated > 0) {
                        Toast.makeText(this, "Güncellendi", Toast.LENGTH_SHORT).show();
                        if (TextUtils.isEmpty(etSearch.getText().toString())) {
                            refreshCursor();
                        } else {
                            performSearch(etSearch.getText().toString());
                        }
                    } else {
                        Toast.makeText(this, "Güncelleme hatası!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Cursor c = adapter != null ? adapter.getCursor() : null;
        if (c != null && !c.isClosed()) c.close();
    }
}
