package com.blueberryjoy.reader.downloads;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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
import com.blueberryjoy.reader.utils.FileStorage;
import com.blueberryjoy.reader.utils.GlobalTipDialog;
import com.blueberryjoy.reader.utils.Message;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class Downloads extends AppCompatActivity {
    private DownloadsRecyclerViewAdapter recyclerViewAdapter;
    private RecyclerView downloads;
    @Nullable
    private DownloadsRecyclerViewNode deleted;
    private PopupWindow popupWindow;
    private DownloadsRecyclerViewItems downloadsRecyclerViewItems;
    private TextView textViewSort;
    private int popupNum = 0;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_downloads);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        downloadsRecyclerViewItems = FileStorage.getDownloadsList();

        downloads = findViewById(R.id.recyclerView);
        textViewSort = findViewById(R.id.textViewSort);

        downloads.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new DownloadsRecyclerViewAdapter(this, downloadsRecyclerViewItems);
        recyclerViewAdapter.setClickListener(new DownloadsRecyclerViewAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(MainActivity.ACTIVITY_RESULT_DOWNLOAD);
                intent.putExtra("link", "file://" + getApplicationContext().getFilesDir().getAbsolutePath() + "/downloads/" + downloadsRecyclerViewItems.get(position).getFilename());
                intent.putExtra("idx", downloadsRecyclerViewItems.get(position).getIdx());
                intent.putExtra("new page", false);
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onItemLongClick(View view, int position, float x, float y) {
                showUrlMenu(view, position, x, y);
            }
        });
        downloads.setAdapter(recyclerViewAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(downloads.getContext(), LinearLayoutManager.VERTICAL);
        downloads.addItemDecoration(dividerItemDecoration);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (swipeDir == 4) {
                    int idx = viewHolder.getLayoutPosition();
                    DownloadsRecyclerViewNode node = downloadsRecyclerViewItems.get(idx);
                    downloadsRecyclerViewItems.remove(idx);
                    recyclerViewAdapter.notifyItemRemoved(idx);
                    String content = FileStorage.deleteDownloadedFile(getApplicationContext(), node.getFilename());
                    if (content != null) {
                        node.setContent(content);
                        deleted = node;
                        initializePopup(node.getFilename());
                    }
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(downloads);

        textViewSort.setOnClickListener((view) -> {
            if (textViewSort.getText().equals(getString(R.string.download_sort_by_name))) {
                textViewSort.setText(getString(R.string.download_sort_by_date));
                downloadsRecyclerViewItems.sortByDate();

            } else {
                textViewSort.setText(getString(R.string.download_sort_by_name));
                downloadsRecyclerViewItems.sortByName();
            }
            recyclerViewAdapter.notifyDataSetChanged();
        });

        findViewById(R.id.imageViewSymbol).setOnClickListener((view) -> {
            if (textViewSort.getText().equals(getString(R.string.download_sort_by_name))) {
                textViewSort.setText(getString(R.string.download_sort_by_date));
                downloadsRecyclerViewItems.sortByDate();

            } else {
                textViewSort.setText(getString(R.string.download_sort_by_name));
                downloadsRecyclerViewItems.sortByName();
            }
            recyclerViewAdapter.notifyDataSetChanged();
        });

        GlobalTipDialog.show(Downloads.this, downloads, getString(R.string.tip_download_new_tab), 4, 0, 64, Gravity.TOP | Gravity.START, true);

        initializeAd();
    }

    private void initializeAd() {
        AdView adView = findViewById(R.id.adView);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Message.log("Ad failed to load: " + adError.getMessage());
            }

            @Override
            public void onAdLoaded() {
                //Get the last visible item position before height change
                LinearLayoutManager layoutManager = (LinearLayoutManager) downloads.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    super.onAdLoaded();
                    //Wait for the layout to update
                    if ((lastVisiblePosition == recyclerViewAdapter.getItemCount() - 1)) {
                        downloads.post(() -> {
                            //Scroll back to the last visible item to keep it at the bottom
                            layoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0);
                        });
                    }
                }
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void initializePopup(String filename) {
        if (popupWindow != null && popupWindow.isShowing()) popupWindow.dismiss();
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_undo, null);
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.setElevation(10f);
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0);
        ((TextView) popupView.findViewById(R.id.undo_notice)).setText(getString(R.string.bookmark_deleted_item, filename));
        popupView.findViewById(R.id.btn_undo).setOnClickListener(v -> {
            if (deleted != null && FileStorage.undoDownloadDeletion(deleted.getFilename(), deleted.getContent())) {
                downloadsRecyclerViewItems.insert(deleted);
                sort();
                recyclerViewAdapter.notifyDataSetChanged();
                deleted.setContent(null);
                deleted = null;
                Message.toastMSG(getString(R.string.message_text_download_restored));
            } else {
                Message.toastErr(getString(R.string.error_text_unable_restore_file));
            }
            popupWindow.dismiss();
        });

        int currentNum = ++popupNum;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentNum == popupNum && popupWindow.isShowing()) popupWindow.dismiss();
        }, 10000);
    }

    private void sort() {
        if (textViewSort.getText().equals(getString(R.string.download_sort_by_name))) {
            downloadsRecyclerViewItems.sortByName();
        } else {
            downloadsRecyclerViewItems.sortByDate();
        }
    }

    private void showUrlMenu(View anchor, int originalIndex, float x, float y) {
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams") View menuView = inflater.inflate(R.layout.popup_url_menu, null);

        PopupWindow popupWindow = new PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        menuView.findViewById(R.id.textCopyUrl).setVisibility(View.GONE);
        menuView.findViewById(R.id.textCopyUrlBorder).setVisibility(View.GONE);

        menuView.findViewById(R.id.textOpen).setOnClickListener(view -> {
            openUrl(originalIndex, false);
            popupWindow.dismiss();
        });

        menuView.findViewById(R.id.textOpenNewWindow).setOnClickListener(view -> {
            openUrl(originalIndex, true);
            popupWindow.dismiss();
        });

        menuView.findViewById(R.id.textOpenChrome).setVisibility(View.GONE);
        menuView.findViewById(R.id.textOpenNewWindowBorder).setVisibility(View.GONE);

        menuView.findViewById(R.id.textEditBorder).setVisibility(View.GONE);
        menuView.findViewById(R.id.textEdit).setVisibility(View.GONE);

        popupWindow.showAtLocation(anchor, Gravity.TOP | Gravity.START, (int) x, (int) y);
    }

    private void openUrl(int position, boolean newWindow) {
        Intent intent = new Intent(MainActivity.ACTIVITY_RESULT_DOWNLOAD);
        intent.putExtra("link", "file://" + getApplicationContext().getFilesDir().getAbsolutePath() + "/downloads/" + downloadsRecyclerViewItems.get(position).getFilename());
        intent.putExtra("idx", downloadsRecyclerViewItems.get(position).getIdx());
        intent.putExtra("new page", newWindow);
        setResult(RESULT_OK, intent);
        finish();
    }
}

