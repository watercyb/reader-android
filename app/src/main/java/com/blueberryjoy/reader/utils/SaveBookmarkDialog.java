package com.blueberryjoy.reader.utils;

import androidx.appcompat.app.AlertDialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.blueberryjoy.reader.App;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

import com.blueberryjoy.reader.R;

public class SaveBookmarkDialog {
    final Window window;
    final HashSet<String> types;
    final String url;
    String title;
    final int idx;
    String type;
    ArrayAdapter<String> adapter;
    AutoCompleteTextView autoCompleteTextViewType;
    final SaveBookmarkListener saveBookmarkListener;
    final int dialogType; // 0: add, 1: update divider, 2: update bookmark

    public SaveBookmarkDialog(Window window, HashSet<String> types, String url, String title, int idx, String type, SaveBookmarkListener saveBookmarkListener, int dialogType) {
        this.window = window;
        this.url = url;
        this.title = title;
        this.types = types;
        this.idx = idx;
        this.type = type;
        this.saveBookmarkListener = saveBookmarkListener;
        this.dialogType = dialogType;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void show() {
        if (dialogType == 0) {
            showNewBookmarkDialog(true);
        } else if (dialogType == 1) {
            showUpdateDividerDialog();
        } else if (dialogType == 2) {
            showNewBookmarkDialog(false);
        }
    }

    private void showNewBookmarkDialog(boolean isNewBookmark) {
        View view = window.getLayoutInflater().inflate(R.layout.dialog_bookmark_new, null);
        TextInputEditText textInputEditTextTitle = view.findViewById(R.id.title);
        autoCompleteTextViewType = view.findViewById(R.id.type);

        textInputEditTextTitle.setText(title);
        textInputEditTextTitle.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                title = textInputEditTextTitle.getText() == null ? "" : textInputEditTextTitle.getText().toString().trim();
                if (title.isEmpty()) {
                    textInputEditTextTitle.setError("Title cannot be empty");
                } else {
                    textInputEditTextTitle.clearFocus();
                    InputMethodManager imm = (InputMethodManager) window.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textInputEditTextTitle.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });

        ArrayList<String> typeList = new ArrayList<>();
        typeList.add(App.Companion.getAppContext().getString(R.string.bookmark_no_type));
        ArrayList<String> sortedTypes = new ArrayList<>(types);
        Collections.sort(sortedTypes);
        typeList.addAll(sortedTypes);
        typeList.add(App.Companion.getAppContext().getString(R.string.bookmark_new_type));
        adapter = new ArrayAdapter<>(window.getContext(), R.layout.dialog_bookmark_type_item, typeList);
        autoCompleteTextViewType.setAdapter(adapter);
        autoCompleteTextViewType.setText(type == null ? typeList.get(0) : type, false);

        AlertDialog dialog;
        if (isNewBookmark) {
            dialog = new MaterialAlertDialogBuilder(window.getContext())
                    .setTitle(R.string.bookmark_save)
                    .setView(view)
                    .setNegativeButton(App.Companion.getAppContext().getString(R.string.button_cancel), null)
                    .setPositiveButton(App.Companion.getAppContext().getString(R.string.bookmark_save), null)
                    .create();
        } else {
            dialog = new MaterialAlertDialogBuilder(window.getContext())
                    .setTitle(R.string.bookmark_update)
                    .setView(view)
                    .setNegativeButton(App.Companion.getAppContext().getString(R.string.button_cancel), null)
                    .setPositiveButton(App.Companion.getAppContext().getString(R.string.bookmark_update), null)
                    .create();
        }

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            title = textInputEditTextTitle.getText() == null ? "" : textInputEditTextTitle.getText().toString().trim();
            if (title.isEmpty()) {
                textInputEditTextTitle.setError("Title cannot be empty");
                return;
            }
            saveBookmarkListener.save(url, title, idx, type);
            dialog.dismiss();
        }));

        autoCompleteTextViewType.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = adapter.getItem(position);
            if (App.Companion.getAppContext().getString(R.string.bookmark_new_type).equals(selected)) {
                autoCompleteTextViewType.setText(App.Companion.getAppContext().getString(R.string.bookmark_no_type), false);
                type = null;
                showNewBookmarkTypeDialog();
            } else if (App.Companion.getAppContext().getString(R.string.bookmark_no_type).equals(selected)) {
                type = null;
            } else {
                type = selected;
            }
        });

        dialog.show();
    }

    private void showUpdateDividerDialog() {
        View view = window.getLayoutInflater().inflate(R.layout.dialog_bookmark_update_divider, null);
        autoCompleteTextViewType = view.findViewById(R.id.type);

        ArrayList<String> typeList = new ArrayList<>();
        typeList.add(App.Companion.getAppContext().getString(R.string.bookmark_no_type));
        ArrayList<String> sortedTypes = new ArrayList<>(types);
        Collections.sort(sortedTypes);
        typeList.addAll(sortedTypes);
        typeList.add(App.Companion.getAppContext().getString(R.string.bookmark_new_type));
        adapter = new ArrayAdapter<>(window.getContext(), R.layout.dialog_bookmark_type_item, typeList);
        autoCompleteTextViewType.setAdapter(adapter);
        autoCompleteTextViewType.setText(type == null ? typeList.get(0) : type, false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(window.getContext()).setTitle(R.string.bookmark_update_type).setView(view).setNegativeButton(App.Companion.getAppContext().getString(R.string.button_cancel), null).setPositiveButton(App.Companion.getAppContext().getString(R.string.bookmark_update_type), null).create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            saveBookmarkListener.save(url, title, idx, type);
            dialog.dismiss();
        }));

        autoCompleteTextViewType.setOnItemClickListener((parent, itemView, position, id) -> {
            String selected = adapter.getItem(position);
            if (App.Companion.getAppContext().getString(R.string.bookmark_new_type).equals(selected)) {
                autoCompleteTextViewType.setText(App.Companion.getAppContext().getString(R.string.bookmark_no_type), false);
                type = null;
                showNewBookmarkTypeDialog();
            } else if (App.Companion.getAppContext().getString(R.string.bookmark_no_type).equals(selected)) {
                type = null;
            } else {
                type = selected;
            }
        });

        dialog.show();
    }

    private void showNewBookmarkTypeDialog() {
        View view = window.getLayoutInflater().inflate(R.layout.dialog_bookmark_new_type, null);
        TextInputEditText typeName = view.findViewById(R.id.typeName);

        AlertDialog dialog = new MaterialAlertDialogBuilder(window.getContext()).setTitle(R.string.bookmark_new_type).setView(view).setNegativeButton(App.Companion.getAppContext().getString(R.string.button_cancel), null).setPositiveButton(App.Companion.getAppContext().getString(R.string.button_ok), null).create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = typeName.getText() == null ? "" : typeName.getText().toString().trim();
                if (name.isEmpty()) {
                    typeName.setError("Type name cannot be empty");
                    return;
                }
                if (types.contains(name)) {
                    typeName.setError("This type already exists");
                    return;
                }

                types.add(name);
                ArrayList<String> newList = new ArrayList<>();
                newList.add(App.Companion.getAppContext().getString(R.string.bookmark_no_type));
                ArrayList<String> sorted = new ArrayList<>(types);
                Collections.sort(sorted);
                newList.addAll(sorted);
                newList.add(App.Companion.getAppContext().getString(R.string.bookmark_new_type));
                adapter.clear();
                adapter.addAll(newList);
                adapter.notifyDataSetChanged();
                autoCompleteTextViewType.setText(name, false);
                type = name;

                dialog.dismiss();
            });
            typeName.requestFocus();
            Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });

        dialog.show();
    }

    @FunctionalInterface
    public interface SaveBookmarkListener {
        void save(String url, String title, int idx, String type);
    }
}
