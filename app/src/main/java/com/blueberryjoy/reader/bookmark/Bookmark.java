package com.blueberryjoy.reader.bookmark;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.main.MainActivity;
import com.blueberryjoy.reader.utils.GlobalTipDialog;
import com.blueberryjoy.reader.utils.FileStorage;
import com.blueberryjoy.reader.utils.Message;
import com.blueberryjoy.reader.utils.SaveBookmarkDialog;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

import org.json.JSONArray;

public class Bookmark extends AppCompatActivity {
    private RecyclerViewAdapterWithFilter recyclerViewAdapter;
    private RecyclerView bookmark;
    private BookmarkList bookmarkList;
    @Nullable
    private BookmarkItem deleted;
    private PopupWindow popupWindow;
    private EditText filter;
    private RecyclerViewAdapterWithFilter.ItemClickListener itemClickListener;
    private int popupNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bookmark);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String title;
        String url;
        int idx;

        bookmarkList = FileStorage.getBookmarkListFromFile();

        ImageView addButton = findViewById(R.id.imageButton);
        bookmark = findViewById(R.id.recyclerView);
        ImageView exportButton = findViewById(R.id.imageButton1);
        ImageView importButton = findViewById(R.id.imageButton2);
        filter = findViewById(R.id.filter);
        ImageView searchButton = findViewById(R.id.imageButton3);
        LinearLayout linearLayout = findViewById(R.id.linearLayout);

        ActivityResultLauncher<Intent> startActivityIntentImport = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    JSONArray jsonArrayNew = FileStorage.importBookmarkJSON(uri);
                    if (jsonArrayNew != null) {
                        String msg;
                        if (jsonArrayNew.length() == 1) {
                            msg = jsonArrayNew.length() + getString(R.string.bookmark_text_rewrite_or_append);
                        } else {
                            msg = jsonArrayNew.length() + getString(R.string.bookmark_text_rewrite_or_append_p);
                        }
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Bookmark.this);
                        alertDialogBuilder.setTitle(getString(R.string.bookmark_title_import)).setMessage(msg).setCancelable(false).setNeutralButton(getString(R.string.bookmark_text_rewrite), (dialog, id) -> {
                            FileStorage.importBookmarkJSONRewrite(jsonArrayNew);
                            refresh();
                        }).setPositiveButton(getString(R.string.bookmark_text_append), (dialog, id) -> {
                            FileStorage.importBookmarkJSONAppend(jsonArrayNew);
                            refresh();
                        }).setNegativeButton(getString(R.string.button_cancel), (dialog, id) -> dialog.cancel());
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                }
            }
        });

        ActivityResultLauncher<Intent> startActivityIntentExport = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) FileStorage.exportBookmarkJSON(getApplicationContext(), uri);
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            title = extras.getString("title", "");
            url = extras.getString("url", "");
            idx = extras.getInt("idx", 0);
        } else {
            title = "";
            url = "";
            idx = 0;
        }

        itemClickListener = new RecyclerViewAdapterWithFilter.ItemClickListener() {
            @Override
            public void onItemClick(int originalIndex, boolean isItem) {
                if (isItem) {
                    Intent intent = new Intent(MainActivity.ACTIVITY_RESULT_BOOKMARK);
                    BookmarkItem bookmarkItem = bookmarkList.getItems().get(originalIndex);
                    if (bookmarkItem != null) {
                        intent.putExtra("link", bookmarkItem.getLink());
                        intent.putExtra("idx", bookmarkItem.getIdx());
                    }
                    intent.putExtra("new page", false);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    bookmarkList.toggleDivider(originalIndex);
                    recyclerViewAdapter.getFilter().filter(filter.getText().toString());
                }
            }

            @Override
            public void onItemLongClick(View view, int originalIndex, float x, float y, boolean isItem) {
                if (isItem) {
                    showUrlMenu(view, originalIndex, x, y);
                } else {
                    BookmarkItem divider = bookmarkList.get(originalIndex);
                    new SaveBookmarkDialog(Bookmark.this.getWindow(), bookmarkList.getTypes(), null, null, -1, divider.getType(), (url, title, idx, type) -> {
                        bookmarkList.updateDivider(divider, type);
                        recyclerViewAdapter.getFilter().filter(filter.getText().toString());
                        if (FileStorage.saveBookmarkJSON(bookmarkList)) {
                            Message.toastMSG(getString(R.string.message_text_type_updated));
                        } else {
                            Message.toastErr(getString(R.string.error_text_unable_update_type));
                        }
                    }, 1).show();
                }
            }
        };
        bookmark.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new RecyclerViewAdapterWithFilter(this, bookmarkList);
        recyclerViewAdapter.setClickListener(itemClickListener);
        bookmark.setAdapter(recyclerViewAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(bookmark.getContext(), LinearLayoutManager.VERTICAL);
        bookmark.addItemDecoration(dividerItemDecoration);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder.getItemViewType() == RecyclerViewAdapterWithFilter.TYPE_DIVIDER)
                    return makeMovementFlags(0, 0);
                return super.getMovementFlags(recyclerView, viewHolder);
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (swipeDir == 4) {
                    int filteredIdx = viewHolder.getLayoutPosition(); // index in displayed (filtered) list
                    int originalIdx = recyclerViewAdapter.displayIndexes.get(filteredIdx); // true index in original list
                    deleted = bookmarkList.get(originalIdx);
                    deleted.setOriIdx(originalIdx);
                    recyclerViewAdapter.remove(filteredIdx);
                    bookmarkList.updateDividerCount(deleted.getType(), -1);
                    recyclerViewAdapter.notifyDataSetChanged();
                    if (FileStorage.saveBookmarkJSONToFile(bookmarkList.getItems())) {
                        initializePopup();
                    } else {
                        deleted = null;
                    }
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(bookmark);

        addButton.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha));
            new SaveBookmarkDialog(Bookmark.this.getWindow(), bookmarkList.getTypes(), url, title, idx, null, (newUrl, newTitle, newIdx, type) -> {
                FileStorage.appendAndSaveBookmarkJSON(bookmarkList, newUrl, newTitle, newIdx, type);
                recyclerViewAdapter.getFilter().filter(filter.getText().toString());
            }, 0).show();
        });

        importButton.setOnClickListener(v -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Bookmark.this);
            alertDialogBuilder.setTitle(getString(R.string.bookmark_title_import)).setMessage(getString(R.string.bookmark_text_import)).setCancelable(false).setPositiveButton(getString(R.string.button_ok), (dialog, id) -> {
                v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha));
                startActivityIntentImport.launch(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        });

        exportButton.setOnClickListener(v -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Bookmark.this);
            alertDialogBuilder.setTitle(getString(R.string.bookmark_title_export)).setMessage(getString(R.string.bookmark_text_export)).setCancelable(false).setPositiveButton(getString(R.string.button_ok), (dialog, id) -> {
                v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha));
                startActivityIntentExport.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").putExtra(Intent.EXTRA_TITLE, "bookmark.txt"));
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        });

        searchButton.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.alpha));
            if (filter.getVisibility() == View.GONE) {
                filterShow();
            } else {
                filterHide();
            }
        });

        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recyclerViewAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        View[] views = {linearLayout, bookmark};
        int[] ids = {2, 3};
        int[] xs = {0, 0};
        int[] ys = {32, 64};
        int[] gravities = {Gravity.TOP | Gravity.END, Gravity.TOP | Gravity.START};
        String[] contents = {getString(R.string.tip_import_export_bookmark), getString(R.string.tip_bookmark_new_tab)};
        GlobalTipDialog.show(Bookmark.this, views, contents, ids, xs, ys, gravities, 0, true);

        initializeAd();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (popupWindow != null && popupWindow.isShowing()) popupWindow.dismiss();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && filter.getVisibility() == View.VISIBLE) {
            filterHide();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void refresh() {
        bookmarkList = FileStorage.getBookmarkListFromFile();
        recyclerViewAdapter = new RecyclerViewAdapterWithFilter(this, bookmarkList);
        recyclerViewAdapter.setClickListener(itemClickListener);
        bookmark.setAdapter(recyclerViewAdapter);
        recyclerViewAdapter.getFilter().filter(filter.getText().toString());
    }

    private void initializeAd() {
        AdView adView = findViewById(R.id.adView);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                //Message.log("Ad failed to load: " + adError.getMessage());
            }

            @Override
            public void onAdLoaded() {
                // Get the last visible item position before height change
                LinearLayoutManager layoutManager = (LinearLayoutManager) bookmark.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    super.onAdLoaded();
                    // Wait for the layout to update
                    if ((lastVisiblePosition == recyclerViewAdapter.getItemCount() - 1)) {
                        bookmark.post(() -> {
                            // Scroll back to the last visible item to keep it at the bottom
                            layoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
                        });
                    }
                }
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }

    private void initializePopup() {
        if (popupWindow != null && popupWindow.isShowing()) popupWindow.dismiss();
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_undo, null);
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.setElevation(10f);
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0);
        assert deleted != null;
        ((TextView) popupView.findViewById(R.id.undo_notice)).setText(getString(R.string.bookmark_deleted_item, deleted.getTitle()));
        popupView.findViewById(R.id.btn_undo).setOnClickListener(v -> {
            if (deleted != null && FileStorage.appendAndSaveBookmarkJSON(bookmarkList, deleted)) {
                bookmarkList.updateDividerCount(deleted.getType(), 1);
                recyclerViewAdapter.getFilter().filter(filter.getText().toString());
                Message.toastMSG(getString(R.string.message_text_bookmark_restored));
            } else {
                Message.toastErr(getString(R.string.error_text_unable_restore_file));
            }
            deleted = null;
            popupWindow.dismiss();
        });

        int currentNum = ++popupNum;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentNum == popupNum && popupWindow.isShowing()) popupWindow.dismiss();
        }, 10000);
    }

    private void filterShow() {
        filter.setVisibility(View.VISIBLE);
        filter.requestFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(filter, 0);
    }

    private void filterHide() {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(filter.getWindowToken(), 0);
        filter.setText("");
        filter.setVisibility(View.GONE);
    }

    private void showUrlMenu(View anchor, int originalIndex, float x, float y) {
        BookmarkItem bookmarkItem = bookmarkList.getItems().get(originalIndex);
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams") View menuView = inflater.inflate(R.layout.popup_url_menu, null);
        PopupWindow popupWindow = new PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        String url = bookmarkItem.getLink();
        menuView.findViewById(R.id.textCopyUrl).setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("URL", url));
            popupWindow.dismiss();
        });
        menuView.findViewById(R.id.textOpen).setOnClickListener(view -> {
            openUrl(originalIndex, false);
            popupWindow.dismiss();
        });
        menuView.findViewById(R.id.textOpenNewWindow).setOnClickListener(view -> {
            openUrl(originalIndex, true);
            popupWindow.dismiss();
        });
        Uri uri = Uri.parse(url);
        menuView.findViewById(R.id.textOpenChrome).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            popupWindow.dismiss();
        });
        menuView.findViewById(R.id.textEdit).setOnClickListener(view -> new SaveBookmarkDialog(Bookmark.this.getWindow(), bookmarkList.getTypes(), url, bookmarkItem.getTitle(), bookmarkItem.getIdx(), bookmarkItem.getType(), (newUrl, title, idx, type) -> {
            bookmarkList.updateBookmark(bookmarkItem, title, type);
            recyclerViewAdapter.getFilter().filter(filter.getText().toString());
            if (FileStorage.saveBookmarkJSON(bookmarkList)) {
                Message.toastMSG(getString(R.string.message_text_bookmark_updated));
            } else {
                Message.toastErr(getString(R.string.error_text_unable_update_bookmark));
            }
            popupWindow.dismiss();
        }, 2).show());
        popupWindow.showAtLocation(anchor, Gravity.TOP | Gravity.START, (int) x, (int) y);
    }

    private void openUrl(int originalIndex, boolean newWindow) {
        Intent intent = new Intent(MainActivity.ACTIVITY_RESULT_BOOKMARK);
        BookmarkItem bookmarkItem = bookmarkList.getItems().get(originalIndex);
        if (bookmarkItem != null) {
            intent.putExtra("link", bookmarkItem.getLink());
            intent.putExtra("idx", bookmarkItem.getIdx());
        }
        intent.putExtra("new page", newWindow);
        setResult(RESULT_OK, intent);
        finish();
    }
}

