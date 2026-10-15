package com.blueberryjoy.reader.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.blueberryjoy.reader.history.History;
import com.blueberryjoy.reader.history.HistorySuggestionAdapter;
import com.blueberryjoy.reader.history.HistoryUrl;
import com.blueberryjoy.reader.main.listview.ListViewAdapter;
import com.blueberryjoy.reader.main.listview.ListViewItem;
import com.blueberryjoy.reader.main.message.MessageBluetooth;
import com.blueberryjoy.reader.main.message.MessageReader;
import com.blueberryjoy.reader.main.message.MessageSender;
import com.blueberryjoy.reader.main.recyclerview.WindowsRecyclerViewNode;
import com.blueberryjoy.reader.main.utils.JavaScriptHandler;
import com.blueberryjoy.reader.main.utils.MainActivityProvider;
import com.blueberryjoy.reader.main.utils.ReaderController;
import com.blueberryjoy.reader.main.utils.WindowListener;
import com.blueberryjoy.reader.utils.ClickAnimation;
import com.blueberryjoy.reader.utils.SaveBookmarkDialog;
import com.blueberryjoy.reader.sqlite.SQLite;
import com.blueberryjoy.reader.history.BrowsingHistory;
import com.blueberryjoy.reader.main.utils.DispatchTouchArea;
import com.blueberryjoy.reader.main.webview.MyWebView;
import com.blueberryjoy.reader.main.recyclerview.WindowsRecyclerViewAdapter;
import com.blueberryjoy.reader.main.recyclerview.WindowsRecyclerViewItems;
import com.blueberryjoy.reader.main.window.Window;
import com.blueberryjoy.reader.utils.Ad_Blocker;
import com.blueberryjoy.reader.bookmark.Bookmark;
import com.blueberryjoy.reader.BuildConfig;
import com.blueberryjoy.reader.utils.CaptureCrash;
import com.blueberryjoy.reader.downloads.Downloads;
import com.blueberryjoy.reader.utils.Favicon;
import com.blueberryjoy.reader.utils.FileStorage;
import com.blueberryjoy.reader.settings.GeneralSetting;
import com.blueberryjoy.reader.utils.GlobalTipDialog;
import com.blueberryjoy.reader.utils.LogList;
import com.blueberryjoy.reader.utils.Message;
import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.settings.ReaderSettings;
import com.blueberryjoy.reader.settings.VoiceSetting;
import com.blueberryjoy.reader.main.window.WindowList;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity {
    private boolean isReadingMode = false;
    private boolean isBlockNextIdxChange = false;
    private int pauseButtonCount = -1;
    private boolean isReadingAfterLockedScreen = false;
    private int scrollPageDirection = 1;
    private int bluetoothPermission = -1;
    private final HashSet<String> bluetoothDevices = new HashSet<>();

    private Disposable disposable;
    private ActivityResultLauncher<Intent> startActivityIntent;
    private DispatchTouchArea dispatchTouchArea;

    private ListView listView;
    private EditText webUrl;
    private TextView loadingAlert;
    private TextView textView;
    private ImageView readingModeButton;
    private MyWebView webViewReading;
    private LinearLayout find;
    private EditText findEditText;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private AppBarLayout appBarLayout;
    private ImageView next;
    private ImageView prev;
    private ImageView reloadText;
    private LinearLayout linearLayout;
    private ImageView scrollToBottom;
    private ImageView pause;
    private Button copy;
    private Button paste;
    private Button replace;
    private Button clear;
    private Button findPrv;
    private Button findNext;
    private Button findClose;
    private ImageView setting;
    private FrameLayout container;
    private ImageView listViewHeaderForward;
    private TextView listViewWindowsNumberBox;
    @Nullable
    private View windowsView;
    private PopupWindow popupWindow;
    private ListViewItem listViewReadingMode;
    private ListViewItem listViewLoad;
    private MaterialCardView menuContainer;
    private RecyclerView recyclerViewHistory;
    private HistorySuggestionAdapter historySuggestionAdapter;
    private CoordinatorLayout coordinator;
    private final List<ListViewItem> listViewItemList = new ArrayList<>();
    private ImageView listViewHeaderDownload;
    private FrameLayout frameLayoutUrl;
    private ImageView imageViewPrivate;
    private Window windowToRemove = null;
    private LinearLayout linearLayoutUrlButton;
    private TextView numberBox;
    private WindowsRecyclerViewAdapter recyclerViewAdapter = null;
    private long lastNewWindowTime = 0L;

    public final static String ACTIVITY_RESULT_BOOKMARK = "bookmark";
    public final static String ACTIVITY_RESULT_DOWNLOAD = "download";
    public final static String ACTIVITY_RESULT_HISTORY = "history";
    public final static String ACTIVITY_RESULT_GENERAL_SETTING = "generalSetting";

    @SuppressLint({"ClickableViewAccessibility", "DefaultUncaughtExceptionDelegation"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Debug.
        if (BuildConfig.DEBUG) Thread.setDefaultUncaughtExceptionHandler(new CaptureCrash(this));

        // Initialize the Google Mobile Ads SDK on a background thread.
        initializeGoogleAds();

        //Have to run before window setting since it needs ReaderSettings.
        initializeViewsAndClasses();

        initializeSQLite();
        initializeWindowList();
        BrowsingHistory.INSTANCE.initialize();
        initializePermissions();
        initializeTextView();
        initializeListView();
        initializeBluetoothListener();
        initializeOnBackHandler();
        initializeMessageReceiver();
        initializeSearchBar();
        initializeButtons();
        initializeStartActivityIntent();
        initializeSwipeRefresh();
        initializeWebUrl();
        initializeWebViewReading();
        initializeRecyclerViewHistory();
        showTips();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ReaderController.unfocused();
        if (WindowList.isReading()) isReadingAfterLockedScreen = true;
        if (dispatchTouchArea.isListViewVisible()) listViewHide();
        WindowList.setIsInForeground(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        WindowList.setIsInForeground(true);
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        pauseButtonCount = -1;
        ReaderController.resume();
        if ((isReadingAfterLockedScreen || WindowList.isLastIdx()))
            scrollToCurrentText(getCurrentWebView(), false);
        isReadingAfterLockedScreen = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
        if (windowToRemove != null) {
            windowToRemove.remove();
            windowToRemove = null;
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            boolean isFrontLayerOn = false;
            if (dispatchTouchArea.isOutside(ev)) {
                if (dispatchTouchArea.isListViewVisible()) {
                    listViewHide();
                    isFrontLayerOn = true;
                }
                if (dispatchTouchArea.isWebUrlVisible()) {
                    webUrlHide();
                    isFrontLayerOn = true;
                }
            }
            if (isFrontLayerOn) return true;
        }
        //TODO
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        checkDispatchTouchArea();
        int orientation = getResources().getConfiguration().orientation;
        int visibility = textView.getVisibility();
        if (orientation == Configuration.ORIENTATION_LANDSCAPE && visibility == View.VISIBLE) {
            setTextViewVisibility(false);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT && visibility == View.GONE && ReaderSettings.getIsShowReadingText()) {
            setTextViewVisibility(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_TAB) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void initializeOnBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (find.getVisibility() == View.VISIBLE) {
                    closeFinder();
                } else if (dispatchTouchArea.isListViewVisible()) {
                    listViewHide();
                } else if (dispatchTouchArea.isWebUrlVisible()) {
                    webUrlHide();
                } else if (isReadingMode) {
                    readingModeSwitch();
                } else if (isRecycleViewWindowsShown()) {
                    recycleViewWindowsHide();
                } else {
                    backward();
                }
            }
        });
    }

    private void checkDispatchTouchArea() {
        if (dispatchTouchArea.isListViewVisible()) listViewHide();
        if (dispatchTouchArea.isWebUrlVisible()) webUrlHide();
    }

    private void refreshWithoutStop() {
        ReaderController.refreshWithoutStop();
    }

    private void sendNotificationSettingChanged() {
        ReaderController.notificationSettingChanged();
    }

    private void sendStopReadingAndClearNotification() {
        ReaderController.stopReadingAndClearNotification();
    }

    private void listViewHide() {
        menuContainer.setVisibility(View.GONE);
        dispatchTouchArea.setListViewVisible(false);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private void setWebUrl(@Nullable String url) {
        if (url == null) {
            webUrl.setText("");
        } else {
            try {
                webUrl.setText(URLDecoder.decode(url, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                webUrl.setText(url);
            }
        }
    }

    private void webUrlHide() {
        webUrl.clearFocus();
        webUrl.setSingleLine(true);
        linearLayout.setVisibility(View.GONE);
        setWebUrl(WindowList.getUrl());
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(webUrl.getWindowToken(), 0);
        appBarLayout.requestFocus();
        recyclerViewHistory.setVisibility(View.GONE);
        dispatchTouchArea.setWebUrlVisible(false);
        showUrlButton();
    }

    private void readingMode() {
        if (!WindowList.isLoadedText()) WindowList.getArrFromPage(false, false, false, -1, -1, -1);
        MyWebView webView = WindowList.getWebView();
        webView.evaluateJavascript("(function() {return pageReader.readingModeHTML; })();", html -> {
            //The returned HTML is JSONString with \", no need to change format when insert to new page.
            webViewReading.evaluateJavascript("(function() { document.documentElement.innerHTML=" + html + "; })();", _html -> {
                setTextViewVisibility(false);
                webViewReading.setVisibility(View.VISIBLE);
                swipeRefresh.setVisibility(View.INVISIBLE);
                addToPage(webViewReading);
                refreshWithoutStop();
                isReadingMode = true;
                readingModeButton.setImageResource(R.mipmap.ic_readingmode_reading);
                webViewReading.updateThemeColor();
            });
        });
    }

    private void switchScrollButton(boolean toBottom) {
        if (toBottom) {
            scrollToBottom.setImageResource(R.mipmap.ic_scrolltobottom);
            scrollPageDirection = 1;
        } else {
            scrollToBottom.setImageResource(R.mipmap.ic_scrolltotop);
            scrollPageDirection = -1;
        }
    }

    private void normalMode() {
        if (WindowList.getCurrentWindow() == null) return;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            setTextViewVisibility(true);
        swipeRefresh.setVisibility(View.VISIBLE);
        webViewReading.setVisibility(View.INVISIBLE);
        webViewReading.loadUrl("about:blank");
        refreshWithoutStop();
        isReadingMode = false;
        readingModeButton.setImageResource(R.mipmap.ic_readingmode_normal);
    }

    //Only for webViewReading.
    private void addToPage(@NonNull MyWebView webView) {
        JavaScriptHandler.loadJavaScriptCode(webView, FileStorage.getJSCode("target", String.valueOf(webView.getHeight() / webView.getScaleFactor())));
    }

    private void scrollToCurrentText(@NonNull WebView webView, boolean isSmooth) {
        JavaScriptHandler.scrollToCurrentText((MyWebView) webView, isSmooth);
    }

    private void showBookmark() {
        if (WindowList.getCurrentWindow() == null) return;
        Intent intent = new Intent(MainActivity.this, Bookmark.class);
        intent.putExtra("title", WindowList.getTitle());
        intent.putExtra("url", WindowList.getUrl());
        int idx;
        if (WindowList.isLoadedText()) {
            idx = WindowList.getIdx();
        } else {
            idx = WindowList.getCachedIdx();
        }
        intent.putExtra("idx", idx);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIntent.launch(intent);
    }

    private void showDownloads() {
        Intent intent = new Intent(MainActivity.this, Downloads.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIntent.launch(intent);
    }

    private void showHistory() {
        Intent intent = new Intent(MainActivity.this, History.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIntent.launch(intent);
    }

    private void showVoiceSetting() {
        if (WindowList.isReading()) sendPause();
        Intent intent = new Intent(MainActivity.this, VoiceSetting.class);
        intent.putExtra("textSample", WindowList.getCurrentStr());
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }

    private void showGeneralSetting() {
        Intent intent = new Intent(MainActivity.this, GeneralSetting.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIntent.launch(intent);
    }

    private void forward() {
        if (WindowList.forward()) {
            progressBar.setVisibility(View.VISIBLE);
            textView.setText("");
        }
    }

    private void backward() {
        if (WindowList.backward()) {
            progressBar.setVisibility(View.VISIBLE);
            textView.setText("");
        }
    }

    private void listViewShow() {
        List<ListViewItem> list = new ArrayList<>();
        for (ListViewItem listViewItem : listViewItemList) {
            if (listViewItem.getEnable()) list.add(listViewItem);
        }
        ListViewAdapter arrayAdapter = new ListViewAdapter(this, list);
        listView.setAdapter(arrayAdapter);
        if (WindowList.canGoForward()) {
            listViewHeaderForward.setImageResource(R.mipmap.ic_header_forward);
            listViewHeaderForward.setEnabled(true);
        } else {
            listViewHeaderForward.setImageResource(R.mipmap.ic_header_forward_grey);
            listViewHeaderForward.setEnabled(false);
        }
        if (!webUrl.getText().toString().startsWith("file")) {
            listViewHeaderDownload.setImageResource(R.mipmap.ic_header_download);
        } else {
            listViewHeaderDownload.setImageResource(R.mipmap.ic_header_download_grey);
        }
        int size = WindowList.size();
        listViewWindowsNumberBox.setText(String.valueOf(size));
        if (WindowList.isLoadedText()) {
            listViewLoad.setTitle(getString(R.string.menu_load_text_reload));
        } else {
            listViewLoad.setTitle(getString(R.string.menu_load_text));
        }
        arrayAdapter.notifyDataSetChanged();
        listView.setSelectionFromTop(0, 100);
        ViewGroup.LayoutParams params = menuContainer.getLayoutParams();
        int heightPixel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
            heightPixel = metrics.getBounds().height();
        } else {
            heightPixel = getListViewHeight();
        }
        double listViewDefaultHeight = 51.1;
        int listViewDefaultRow = 7;
        double height = getResources().getDisplayMetrics().density * listViewDefaultHeight;
        params.height = Math.min((int) (getResources().getDisplayMetrics().density * listViewDefaultHeight * listViewDefaultRow), (int) ((int) ((double) heightPixel * 0.72 / height) * height));
        menuContainer.setLayoutParams(params);
        menuContainer.setVisibility(View.VISIBLE);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            listViewHide();
            if (position <= 0 || position > list.size()) return;
            list.get(position - 1).onClick(false);
        });
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            listViewHide();
            if (position <= 0 || position > list.size()) return true;
            list.get(position - 1).onClick(true);
            return true;
        });
    }

    @SuppressWarnings("deprecation")
    private int getListViewHeight() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    private void hideAppBarLayout() {
        if (!dispatchTouchArea.isWebUrlVisible()) appBarLayout.setExpanded(false, true);
    }

    private void showAppBarLayout() {
        appBarLayout.setExpanded(true, true);
    }

    @SuppressLint("InflateParams")
    private void initializeListView() {
        listViewItemList.clear();

        //Find in page
        listViewItemList.add(new ListViewItem(getString(R.string.menu_find_in_page), (item, isLongClick) -> openFinder()));

        //Reading mode
        listViewReadingMode = new ListViewItem(getString(R.string.menu_reading_mode), (item, isLongClick) -> {
            if (WindowList.getLoadingState() != 0) return;
            if (find.getVisibility() == View.VISIBLE) closeFinder();
            if (!isReadingMode) {
                readingMode();
                item.setTitle(getString(R.string.menu_reading_mode_true));
            } else {
                normalMode();
                item.setTitle(getString(R.string.menu_reading_mode));
            }
        });
        listViewItemList.add(listViewReadingMode);

        //Desktop site
        ListViewItem desktopMode = new ListViewItem(getString(R.string.menu_desktop_site), (item, isLongClick) -> {
            if (WindowList.getCurrentWindow() != null && !isReadingMode)
                WindowList.setDesktopMode(!WindowList.getIsDesktopMode());
        }, () -> WindowList.getIsDesktopMode() ? getString(R.string.menu_mobile_site) : getString(R.string.menu_desktop_site), () -> WindowList.getCurrentWindow() != null && !isReadingMode);
        listViewItemList.add(desktopMode);

        //Load text
        listViewLoad = new ListViewItem(getString(R.string.menu_load_text), (item, isLongClick) -> WindowList.reloadText(-1, -1, true));
        listViewItemList.add(listViewLoad);

        //History
        listViewItemList.add(new ListViewItem(getString(R.string.menu_history), (item, isLongClick) -> showHistory()));

        //Bookmarks
        listViewItemList.add(new ListViewItem(getString(R.string.menu_bookmark), (item, isLongClick) -> showBookmark()));

        //Downloads
        listViewItemList.add(new ListViewItem(getString(R.string.menu_downloads), (item, isLongClick) -> showDownloads()));

        //Voice setting
        listViewItemList.add(new ListViewItem(getString(R.string.menu_voice_setting), (item, isLongClick) -> showVoiceSetting()));

        //General setting
        listViewItemList.add(new ListViewItem(getString(R.string.menu_general_setting), (item, isLongClick) -> showGeneralSetting()));

        //About
        listViewItemList.add(new ListViewItem(getString(R.string.menu_about), (item, isLongClick) -> showAbout()));

        //Save to server if (BuildConfig.DEBUG) {listViewItemList.add(new ListViewItem("Save to Server", (item, isLongClick) -> volleySet()));}

        //Get from server if (BuildConfig.DEBUG) {listViewItemList.add(new ListViewItem("Get from Server", (item, isLongClick) -> volleyGet()));}

        //Download Page if (BuildConfig.DEBUG) {listViewItemList.add(new ListViewItem("Download Page", (item, isLongClick) -> WindowList.download(true)));}

        //TestLog
        if (BuildConfig.DEBUG) {
            listViewItemList.add(new ListViewItem("TestLog", (item, isLongClick) -> {
                if (!isLongClick) {
                    MainActivity.this.startActivity(new Intent(MainActivity.this, LogList.class));
                } else {
                    File file = new File(FileStorage.getDir() + "/log.txt");
                    if (file.delete()) Message.toast("Logs cleaned");
                }
            }));
        }

        //Test if (BuildConfig.DEBUG) {listViewItemList.add(new ListViewItem("test", (item, isLongClick) -> {}));}

        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View listViewHeader = layoutInflater.inflate(R.layout.listview_main_header, null);
        ImageView imageViewBookmark = listViewHeader.findViewById(R.id.imageViewBookmark);
        imageViewBookmark.setOnClickListener(v -> new SaveBookmarkDialog(this.getWindow(), FileStorage.getTypesFromFile(), WindowList.getUrl(), WindowList.getTitle(), WindowList.getIdx(), null, (url, title, idx, type) -> {
            FileStorage.appendAndSaveBookmarkJSON(url, title, idx, type);
            listViewHide();
        }, 0).show());
        ClickAnimation.INSTANCE.set(imageViewBookmark);
        listViewHeaderForward = listViewHeader.findViewById(R.id.imageViewForward);
        listViewHeaderForward.setOnClickListener(v -> {
            forward();
            listViewHide();
        });
        ClickAnimation.INSTANCE.set(listViewHeaderForward);
        ImageView imageViewReload = listViewHeader.findViewById(R.id.imageViewReload);
        imageViewReload.setOnClickListener(v -> {
            if (isReadingMode) readingModeSwitch();
            WindowList.webViewLoadUrl(null);
            listViewHide();
        });
        ClickAnimation.INSTANCE.set(imageViewReload);
        listViewHeaderDownload = listViewHeader.findViewById(R.id.imageViewDownload);
        listViewHeaderDownload.setOnClickListener(v -> {
            if (!webUrl.getText().toString().startsWith("file")) WindowList.download(false);
            listViewHide();
        });
        ClickAnimation.INSTANCE.set(listViewHeaderDownload);
        listViewWindowsNumberBox = listViewHeader.findViewById(R.id.numberBox);
        listViewHeader.findViewById(R.id.frameLayoutWindows).setOnClickListener(v -> {
            recycleViewWindowsShow();
            listViewHide();
        });
        listView.addHeaderView(listViewHeader);
    }

    private void openFinder() {
        find.setVisibility(View.VISIBLE);
        findEditText.requestFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(findEditText, 0);
    }

    private void closeFinder() {
        getCurrentWebView().clearMatches();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(findEditText.getWindowToken(), 0);
        find.setVisibility(View.GONE);
        findEditText.setText("");
    }

    private void sendPause() {
        ReaderController.pause();
    }

    private void readingModeSwitch() {
        listViewReadingMode.onClick(false);
    }

    private void setTextViewVisibility(boolean visibility) {
        if (visibility && ReaderSettings.getIsShowReadingText()) {
            textView.setVisibility(View.VISIBLE);
        } else {
            textView.setVisibility(View.GONE);
        }
        //Directly call requestLayout does not work (from https://juejin.cn/post/6844903829113143303).
        textView.post(textView::requestLayout);
    }

    private void errorFromReader(int e) {
        if (e == MessageReader.ACTION_ERROR_TTS) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setTitle(getString(R.string.error_title_no_tts)).setMessage(getString(R.string.error_text_no_tts)).setCancelable(false).setPositiveButton(getString(R.string.button_close), (dialog, id) -> {
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    private void initializePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED && !ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.BLUETOOTH_CONNECT))
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 0);
            bluetoothPermission = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            bluetoothPermission = PackageManager.PERMISSION_GRANTED;
        }
    }

    //Add devices that are connected before app is open.
    private void initializeBluetoothListener() {
        if (bluetoothPermission == PackageManager.PERMISSION_GRANTED) {
            BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, @NonNull BluetoothProfile proxy) {
                    if (profile == BluetoothProfile.A2DP) {
                        proxy.getConnectedDevices().stream().map(BluetoothDevice::getAddress).forEach(bluetoothDevices::add);
                        checkBluetoothDevices();
                    }
                }


                //Called when bluetooth disabled.
                @Override
                public void onServiceDisconnected(int profile) {
                    if (profile == BluetoothProfile.A2DP && WindowList.isReading())
                        WindowList.pause();
                }
            };
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager.getAdapter() != null)
                bluetoothManager.getAdapter().getProfileProxy(this, profileListener, BluetoothProfile.A2DP);
        }
    }

    private void initializeMessageReceiver() {
        disposable = MessageSender.getMessageSender().observeOn(AndroidSchedulers.mainThread()).subscribe(message -> {
            if (message instanceof MessageReader) {
                switch (((MessageReader) message).action()) {
                    case MessageReader.ACTION_CONTEXT -> {
                        int direction = ((MessageReader) message).message();
                        JSONArray idxList = WindowList.getPageIdx();
                        if (idxList != null) {
                            if (isBlockNextIdxChange) {
                                getCurrentWebView().evaluateJavascript("javascript: if (window.pageReader) window.pageReader.updateHighlightAndScroll(" + 0 + "," + idxList + "," + getCurrentWebView().getScrollY() / getCurrentWebView().getScaleFactor() + ");", html -> {
                                    if (html.equals("1")) hideAppBarLayout();
                                });
                                isBlockNextIdxChange = false;
                            } else {
                                getCurrentWebView().evaluateJavascript("javascript: if (window.pageReader) window.pageReader.updateHighlightAndScroll(" + direction + "," + idxList + "," + getCurrentWebView().getScrollY() / getCurrentWebView().getScaleFactor() + ");", html -> {
                                    if (html.equals("1")) hideAppBarLayout();
                                });
                            }
                            textView.scrollTo(0, 0);
                            if (WindowList.isValidReadingStatus())
                                textView.setText(MessageFormat.format("{0}({1}/{2})", WindowList.getCurrentStr(), WindowList.getIdx() + 1, WindowList.getReadingArrLength()));
                        }
                    }
                    case MessageReader.ACTION_PAUSE -> {
                        int pauseCount = ((MessageReader) message).message();
                        if (WindowList.getCurrentWindow() != null && pauseCount >= pauseButtonCount) {
                            pauseButtonCount = pauseCount;
                            pause.setPadding(5, 0, 0, 0);
                            pause.setImageResource(R.mipmap.ic_play);
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            WindowList.setReading(false);
                        }
                    }
                    case MessageReader.ACTION_START -> {
                        int startCount = ((MessageReader) message).message();
                        if (WindowList.getCurrentWindow() != null && WindowList.isLoadedText() && startCount >= pauseButtonCount) {
                            pauseButtonCount = startCount;
                            pause.setPadding(0, 0, 0, 0);
                            pause.setImageResource(R.mipmap.ic_pause);
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            WindowList.setReading(true);
                        }
                    }
                    case MessageReader.ACTION_ERROR ->
                            errorFromReader(((MessageReader) message).message());
                }
            } else if (message instanceof MessageBluetooth) {
                switch (((MessageBluetooth) message).action()) {
                    case MessageBluetooth.ACTION_DISCONNECT -> {
                        String disconnectedDevice = ((MessageBluetooth) message).message();
                        if (disconnectedDevice != null && bluetoothDevices.contains(disconnectedDevice)) {
                            bluetoothDevices.remove(disconnectedDevice);
                            if (WindowList.isReading()) WindowList.pause();
                        }
                    }
                    case MessageBluetooth.ACTION_CONNECT -> {
                        String connectedDevice = ((MessageBluetooth) message).message();
                        if (connectedDevice != null) bluetoothDevices.add(connectedDevice);
                        checkBluetoothDevices();
                    }
                }
            }
        }, error -> Message.log("Main message listener: " + error));
    }

    private void initializeSearchBar() {
        copy.setOnClickListener(v -> {
            String str = webUrl.getText().toString();
            int start = Math.max(webUrl.getSelectionStart(), 0);
            int end = Math.max(webUrl.getSelectionEnd(), 0);
            ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip;
            if (start != end) {
                clip = ClipData.newPlainText("", str.substring(start, end));
            } else {
                clip = ClipData.newPlainText("", str);
            }
            clipboard.setPrimaryClip(clip);
        });

        paste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null) {
                String str = clip.getItemAt(0).getText().toString();
                int start = Math.max(webUrl.getSelectionStart(), 0);
                int end = Math.max(webUrl.getSelectionEnd(), 0);
                webUrl.getText().replace(Math.min(start, end), Math.max(start, end), str, 0, str.length());
            }
        });

        replace.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null) {
                ClipData.Item item = clip.getItemAt(0);
                setWebUrl(item.getText().toString());
            }
        });

        clear.setOnClickListener(v -> setWebUrl(""));

        findPrv.setOnClickListener(v -> {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(findEditText.getWindowToken(), 0);
            getCurrentWebView().findNext(false);
        });

        findNext.setOnClickListener(v -> {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(findEditText.getWindowToken(), 0);
            getCurrentWebView().findNext(true);
        });

        findClose.setOnClickListener(v -> closeFinder());

        findEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(@NonNull CharSequence s, int start, int before, int count) {
                if (s.length() > 0)
                    getCurrentWebView().findAllAsync(findEditText.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            if (find.getVisibility() != View.VISIBLE) return insets;
            //TODO check.
            /*
            boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            if (isKeyboardVisible) {
                View decorView = getWindow().getDecorView();
                Rect rect = new Rect();
                decorView.getWindowVisibleDisplayFrame(rect);
                find.setTranslationY(rect.height() - find.getHeight());
            } else {
                find.setTranslationY(getCurrentWebView().getHeight() + (textView.getVisibility() == View.VISIBLE ? textView.getHeight() : 0) - find.getHeight());
            }
            */
            View decorView = getWindow().getDecorView();
            Rect rect = new Rect();
            decorView.getWindowVisibleDisplayFrame(rect);
            find.setTranslationY(rect.height() - find.getHeight());
            return insets;
        });
    }

    private void initializeButtons() {
        prev.setOnClickListener(v -> {
            //v.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.alpha));
            if (WindowList.getLoadingState() == 0 && !isRecycleViewWindowsShown() && WindowList.isChangingIdxEnabled())
                ReaderController.prev();
        });
        ClickAnimation.INSTANCE.set(prev);

        next.setOnClickListener(v -> {
            //v.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.alpha));
            if (WindowList.getLoadingState() == 0 && !isRecycleViewWindowsShown() && WindowList.isChangingIdxEnabled())
                ReaderController.next();
        });
        ClickAnimation.INSTANCE.set(next);

        pause.setOnClickListener(v -> {
            //v.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.alpha));
            if (!isRecycleViewWindowsShown())
                WindowList.pause();
        });
        ClickAnimation.INSTANCE.set(pause);

        readingModeButton.setOnClickListener(v -> {
            //v.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.alpha));
            readingModeSwitch();
        });
        ClickAnimation.INSTANCE.set(readingModeButton);

        setting.setOnClickListener(v -> {
            //v.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.alpha));
            if (menuContainer.getVisibility() == View.VISIBLE) {
                listViewHide();
            } else {
                listViewShow();
                dispatchTouchArea.add(menuContainer);
                dispatchTouchArea.add(setting);
                dispatchTouchArea.setListViewVisible(true);
            }
        });
        ClickAnimation.INSTANCE.set(setting);

        scrollToBottom.setOnClickListener(v -> {
            //v.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.alpha));
            if (WindowList.getLoadingState() != 0) return;
            if (scrollPageDirection == -1) {
                hideAppBarLayout();
                JavaScriptHandler.scrollTo(getCurrentWebView(), 0);
                switchScrollButton(true);
            } else if (scrollPageDirection == 1) {
                hideAppBarLayout();
                int scrollHeight = (int) (getCurrentWebView().getContentHeight() * getCurrentWebView().getScaleFactor());
                JavaScriptHandler.scrollTo(getCurrentWebView(), scrollHeight);
                switchScrollButton(false);
            } else {
                scrollToCurrentText(getCurrentWebView(), true);
            }
        });
        ClickAnimation.INSTANCE.set(scrollToBottom);

        scrollToBottom.setLongClickable(true);

        scrollToBottom.setOnLongClickListener(v -> {
            if (WindowList.isLoadedText()) scrollToCurrentText(getCurrentWebView(), true);
            return true;
        });

        reloadText.setOnClickListener(v -> {
            //v.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), R.anim.alpha));
            WindowList.reloadText(-1, -1, true);
        });
        ClickAnimation.INSTANCE.set(reloadText);

        numberBox.setOnClickListener(v -> recycleViewWindowsShow());
        ClickAnimation.INSTANCE.set(numberBox);
    }

    private MyWebView getCurrentWebView() {
        if (isReadingMode) return webViewReading;
        return WindowList.getWebView();
    }

    private void initializeStartActivityIntent() {
        startActivityIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Intent intent = result.getData();
                switch (Objects.requireNonNull(intent.getAction())) {
                    case ACTIVITY_RESULT_BOOKMARK:
                    case ACTIVITY_RESULT_DOWNLOAD:
                    case ACTIVITY_RESULT_HISTORY:
                        String url = intent.getStringExtra("link");
                        if (url != null) {
                            if (intent.getBooleanExtra("new page", false)) {
                                boolean isPrivate = WindowList.getCurrentWindow() != null && WindowList.getCurrentWindow().isPrivate();
                                WindowList.createAndInsert(url, intent.getIntExtra("idx", 0), true, isPrivate);
                            } else {
                                if (isReadingMode) readingModeSwitch();
                                WindowList.addPageToCache(url, intent.getIntExtra("idx", 0), "");
                                WindowList.webViewLoadUrl(url);
                            }
                        }
                        break;
                    case ACTIVITY_RESULT_GENERAL_SETTING:
                        boolean[] isChanged = intent.getBooleanArrayExtra("isChanged");
                        if (isChanged != null) {
                            //isShowReadingText
                            if (isChanged[0]) {
                                if (ReaderSettings.getIsShowReadingText()) {
                                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && textView.getVisibility() == View.GONE && !isReadingMode)
                                        setTextViewVisibility(true);
                                } else {
                                    setTextViewVisibility(false);
                                }
                            }
                            //isShowContentInNotification
                            if (isChanged[1]) sendNotificationSettingChanged();
                        }
                        break;
                    default:
                }
            }
        });
    }

    private void initializeSwipeRefresh() {
        swipeRefresh.setProgressViewOffset(false, -150, 75);

        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(true);
            WindowList.webViewLoadUrl(null);
            new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefresh.setRefreshing(false), 1000);
        });
    }

    private void initializeWebUrl() {
        webUrl.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, @NonNull Menu menu) {
                menu.clear();
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        });

        webUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @SuppressWarnings("CharsetObjectCanBeUsed")
            @Override
            public void afterTextChanged(@NonNull Editable s) {
                for (int i = s.length() - 1; i >= 0; i--) {
                    if (s.charAt(i) == '\n') {
                        s.delete(i, i + 1);
                        String url;
                        try {
                            url = URLDecoder.decode(webUrl.getText().toString(), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            url = webUrl.getText().toString();
                        }
                        webUrlHide();
                        setWebUrl(url);
                        if (isReadingMode) readingModeSwitch();
                        WindowList.webViewLoadUrl(url);
                        return;
                    }
                }

                if (webUrl.isFocused()) {
                    ArrayList<HistoryUrl> list = BrowsingHistory.INSTANCE.searchHistory(webUrl.getText().toString());
                    historySuggestionAdapter.addList(list);
                }
            }
        });

        webUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (dispatchTouchArea.isWebUrlVisible()) return;
                webUrl.setSingleLine(false);
                linearLayout.setVisibility(View.VISIBLE);
                recyclerViewHistory.setVisibility(View.VISIBLE);
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(webUrl, 0);
                dispatchTouchArea.add(webUrl);
                dispatchTouchArea.add(linearLayout);
                dispatchTouchArea.add(recyclerViewHistory);
                dispatchTouchArea.setWebUrlVisible(true);
                hideUrlButton();
            }
        });
    }

    private void initializeWebViewReading() {
        //Page will be opened on system browser without this.
        webViewReading.setWebViewClient(new WebViewClient() {
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                return true;
            }
        });
        //To enable window.alert.
        webViewReading.setWebChromeClient(new WebChromeClient());
        webViewReading.loadUrl("about:blank");

        webViewReading.onDoubleTapListener = (idx, x, y) -> {
            if (WindowList.getCurrentWindow() == null) return;
            if (idx >= 0) {
                idx = WindowList.getReadingIdx(idx);
                if (idx >= 0) {
                    ReaderController.idx(idx);
                }
            }
        };

        webViewReading.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (isReadingMode) JavaScriptHandler.updateWindowHeight(getCurrentWebView());
        });

        webViewReading.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
            if (isDoneCounting) {
                if (numberOfMatches == 0) {
                    Message.toastFindInPage(getString(R.string.find_text_not_found));
                } else {
                    Message.toastFindInPage((activeMatchOrdinal + 1) + "/" + numberOfMatches);
                }
            }
        });
    }

    private void initializeTextView() {
        textView.setMovementMethod(new ScrollingMovementMethod());
        setTextViewVisibility(ReaderSettings.getIsShowReadingText());
    }

    private void initializeViewsAndClasses() {
        listView = findViewById(R.id.listView);
        webUrl = findViewById(R.id.editTextUrl);
        loadingAlert = findViewById(R.id.textViewLoading);
        textView = findViewById(R.id.textViewReadingText);
        readingModeButton = findViewById(R.id.buttonReadingMode);
        webViewReading = findViewById(R.id.webViewReading);
        find = findViewById(R.id.linearLayoutFindBar);
        findEditText = findViewById(R.id.editTextFind);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        appBarLayout = findViewById(R.id.appbar);
        next = findViewById(R.id.buttonNext);
        prev = findViewById(R.id.buttonPrev);
        reloadText = findViewById(R.id.buttonReloadText);
        linearLayout = findViewById(R.id.linearLayoutAppbar);
        scrollToBottom = findViewById(R.id.buttonScroll);
        pause = findViewById(R.id.buttonPlay);
        copy = findViewById(R.id.buttonCopy);
        paste = findViewById(R.id.buttonPaste);
        replace = findViewById(R.id.buttonReplace);
        clear = findViewById(R.id.buttonClear);
        findPrv = findViewById(R.id.buttonFindPrev);
        findNext = findViewById(R.id.buttonFindNext);
        findClose = findViewById(R.id.buttonFindClose);
        setting = findViewById(R.id.imageButtonSetting);
        container = findViewById(R.id.frameLayoutWebViewContainer);
        menuContainer = findViewById(R.id.menuContainer);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        coordinator = findViewById(R.id.coordinator);
        imageViewPrivate = findViewById(R.id.imageViewPrivate);
        linearLayoutUrlButton = findViewById(R.id.linearLayoutUrlButton);
        frameLayoutUrl = findViewById(R.id.frameLayoutUrl);
        numberBox = findViewById(R.id.numberBox);

        dispatchTouchArea = new DispatchTouchArea();

        ReaderSettings.initial();
        Favicon.initial();
        FileStorage.setDir(getBaseContext().getFilesDir().getAbsolutePath());
        Ad_Blocker.initialize();

        appBarLayout.addOnLayoutChangeListener((View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) -> {
            if (recyclerViewHistory.getVisibility() == View.VISIBLE) {
                appBarLayout.post(() -> {
                    recyclerViewHistory.setTranslationY(bottom);
                    ViewGroup.LayoutParams params = recyclerViewHistory.getLayoutParams();
                    params.height = coordinator.getHeight() - bottom;
                    recyclerViewHistory.setLayoutParams(params);
                });
            }
        });

        appBarLayout.addOnOffsetChangedListener((layout, offset) -> {
            if (progressBar.getVisibility() == View.VISIBLE)
                progressBar.setTranslationY(appBarLayout.getHeight() + offset - (float) progressBar.getHeight() / 2);
        });
    }

    private void initializeWindowList() {
        MainActivityProvider mainActivityProvider = new MainActivityProvider() {
            @NonNull
            @Override
            public String getWebUrl() {
                return webUrl.getText().toString();
            }

            @Override
            public boolean isReadingMode() {
                return isReadingMode;
            }

            @NonNull
            @Override
            public MyWebView createWebView(boolean isPrivate) {
                return createWebview(isPrivate);
            }
        };

        WindowListener windowListener = new WindowListener() {
            @Override
            public void onProgressChanged(int newProgress) {
                progressBar.setProgress(newProgress);
                if (!isReadingMode && newProgress < 100 && progressBar.getVisibility() == View.INVISIBLE) {
                    progressBar.setVisibility(View.VISIBLE);
                    showAppBarLayout();
                }
                if (newProgress == 100) progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onReceivedIcon(@NonNull String url, @NonNull Bitmap icon) {
                FileStorage.saveFavicon(icon, url);
            }

            @Override
            public void onLoadedTextStateChanged(boolean state) {
                if (state) {
                    prev.setVisibility(View.VISIBLE);
                    next.setVisibility(View.VISIBLE);
                    reloadText.setImageResource(R.mipmap.ic_reload_text);
                    GlobalTipDialog.show(MainActivity.this, reloadText, getString(R.string.tip_reload), 5, 0, 32, Gravity.BOTTOM | Gravity.START, false);
                } else {
                    prev.setVisibility(View.GONE);
                    next.setVisibility(View.GONE);
                    reloadText.setImageResource(R.mipmap.ic_load_text);
                    switchScrollButton(true);
                }
            }

            @Override
            public void onAlertStatusChanged(boolean enable) {
                if (enable) {
                    loadingAlert.setVisibility(View.VISIBLE);
                } else {
                    loadingAlert.setVisibility(View.GONE);
                }
            }

            @Override
            public void onUrlChanged(String url) {
                setWebUrl(url);
            }

            @Override
            public void onResetTextBoxRequired() {
                textView.setText("");
            }

            @Override
            public void onSwipeRefreshRequired(boolean enable) {
                swipeRefresh.setEnabled(enable);
            }

            @Override
            public void onAppBarLayoutRequired() {
                showAppBarLayout();
            }

            @Override
            public void onReceivedSslError(@NonNull String url) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setTitle(getString(R.string.error_title_ssl)).setMessage(getString(R.string.error_text_ssl)).setPositiveButton(getString(R.string.button_access), (dialog, id) -> WindowList.acceptSSLError(url)).setNegativeButton(getString(R.string.button_cancel), (dialog, id) -> dialog.cancel());
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }

            @Override
            public void onLongClick(@NonNull String url, boolean isPrivate) {
                new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.message_title_open_new_tab)).setMessage(url).setPositiveButton(getString(R.string.message_button_open), (dialog, id) -> {
                    WindowList.createAndInsert(url, 0, false, isPrivate);
                    Message.toastMSG(getString(R.string.message_text_link_opened));
                }).setCancelable(true).show();
            }

            @Override
            public void onWindowNumberChanged(int number) {
                MainActivity.this.setNumberBox(number);
            }

            @Override
            public void onReadingModeSwitchingRequired(boolean isSwitchToReadingMode) {
                if (isReadingMode != isSwitchToReadingMode) readingModeSwitch();
            }

            @Override
            public void onCurrentWindowChanged(int idx, boolean isPrivate) {
                if (recyclerViewAdapter == null) return;
                int size = recyclerViewAdapter.getItemCount();
                if (idx >= 0 && idx < size) recyclerViewAdapter.notifyItemChanged(idx);
                if (isPrivate) {
                    imageViewPrivate.setVisibility(View.VISIBLE);
                } else {
                    imageViewPrivate.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTextNotMatching() {
                if (!isReadingMode) {
                    reloadText.setImageResource(R.mipmap.ic_reload_text_warning);
                    GlobalTipDialog.show(MainActivity.this, reloadText, getString(R.string.tip_text_unmatch), 13, 64, 32, Gravity.BOTTOM | Gravity.START, true);
                }
            }

            @Override
            public void onPageScroll(int position) {
                switchScrollButton(position < 99);
            }
        };

        WindowList.initializeWindowList(mainActivityProvider, windowListener);
    }

    private void recycleViewWindowsShow() {
        if (isRecycleViewWindowsShown()) return;
        if (isReadingMode) readingModeSwitch();
        sendStopReadingAndClearNotification();

        ConstraintLayout main = findViewById(R.id.main);
        windowsView = LayoutInflater.from(this).inflate(R.layout.popup_main_windows, main, false);
        windowsView.setId(View.generateViewId());
        main.addView(windowsView);
        ConstraintSet set = new ConstraintSet();

        set.clone(main);
        set.connect(windowsView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
        set.connect(windowsView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
        set.connect(windowsView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
        set.connect(windowsView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
        set.constrainWidth(windowsView.getId(), ConstraintSet.MATCH_CONSTRAINT);
        set.constrainHeight(windowsView.getId(), ConstraintSet.MATCH_CONSTRAINT);
        set.applyTo(main);
        ReaderController.canPlay(false);

        WindowsRecyclerViewItems windowsRecyclerViewItems = WindowList.getWindowsRecyclerViewItems();
        RecyclerView recyclerView = windowsView.findViewById(R.id.recyclerViewWindows);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new WindowsRecyclerViewAdapter(this, windowsRecyclerViewItems);
        recyclerViewAdapter.setClickListener((position) -> {
            WindowList.switchWindow(windowsRecyclerViewItems.get(position).window());
            recycleViewWindowsHide();
        });
        recyclerView.setAdapter(recyclerViewAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), LinearLayoutManager.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                WindowList.move(from, to);
                recyclerViewAdapter.move(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (swipeDir == 4) {
                    Window removedWindow = recyclerViewAdapter.get(viewHolder.getLayoutPosition()).window();
                    if (windowToRemove != null) {
                        windowToRemove.remove();
                        windowToRemove = null;
                    }
                    if (WindowList.removeWindow(removedWindow)) {
                        windowToRemove = removedWindow;
                        recyclerViewAdapter.remove(viewHolder.getLayoutPosition());
                        if (viewHolder.getLayoutPosition() == 0 && recyclerViewAdapter.getItemCount() > 0)
                            recyclerViewAdapter.notifyItemChanged(0);
                        initializeRecycleViewWindowsPopup(recyclerViewAdapter, removedWindow);
                    }
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
        ImageView imageButtonAdd = windowsView.findViewById(R.id.imageButtonAdd);
        imageButtonAdd.setOnClickListener((v) -> {
            long now = System.currentTimeMillis();
            if (now - lastNewWindowTime < 500) return;
            lastNewWindowTime = now;
            recyclerViewAdapter.add(WindowList.createAndInsert("about:newtab", 0, false, false));
            WindowsRecyclerViewNode windowsRecyclerViewNode = windowsRecyclerViewItems.getLast();
            if (windowsRecyclerViewNode != null) {
                WindowList.switchWindow(windowsRecyclerViewNode.window());
                recycleViewWindowsHide();
            }
        });
        ClickAnimation.INSTANCE.set(imageButtonAdd);
        ImageView imageButtonAddPrivate = windowsView.findViewById(R.id.imageButtonAddPrivate);
        imageButtonAddPrivate.setOnClickListener((v) -> {
            recyclerViewAdapter.add(WindowList.createAndInsert("about:newtab", 0, false, true));
            WindowsRecyclerViewNode windowsRecyclerViewNode = windowsRecyclerViewItems.getLast();
            if (windowsRecyclerViewNode != null) {
                WindowList.switchWindow(windowsRecyclerViewNode.window());
                recycleViewWindowsHide();
            }
        });
        ClickAnimation.INSTANCE.set(imageButtonAddPrivate);
    }

    private void initializeRecycleViewWindowsPopup(@NonNull WindowsRecyclerViewAdapter recyclerViewAdapter, @NonNull Window removedWindow) {
        if (popupWindow != null && popupWindow.isShowing()) popupWindow.dismiss();
        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams") View popupView = inflater.inflate(R.layout.popup_undo, null);
        popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.setElevation(10f);
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0);
        ((TextView) popupView.findViewById(R.id.undo_notice)).setText(getString(R.string.window_deleted_item, removedWindow.getTitle()));

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = () -> {
            if (popupWindow.isShowing()) popupWindow.dismiss();
            if (windowToRemove != null && windowToRemove.getId() == removedWindow.getId()) {
                windowToRemove = null;
                removedWindow.remove();
            }
        };
        handler.postDelayed(runnable, 10000);

        popupView.findViewById(R.id.btn_undo).setOnClickListener(v -> {
            handler.removeCallbacks(runnable);
            windowToRemove = null;
            WindowsRecyclerViewNode node = WindowList.restore(removedWindow);
            setNumberBox(WindowList.getWindowsRecyclerViewItems().size());
            int idx = removedWindow.getRemovedWindowIdx();
            if (idx < 0 || idx > recyclerViewAdapter.getItemCount())
                idx = recyclerViewAdapter.getItemCount();
            recyclerViewAdapter.add(idx, node);
            Message.toastMSG(getString(R.string.message_text_window_restored));
            popupWindow.dismiss();
        });
    }

    @NonNull
    public MyWebView createWebview(boolean isPrivate) {
        MyWebView webView = new MyWebView(MainActivity.this, isPrivate);
        webView.setVisibility(View.GONE);
        webView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(webView);
        return webView;
    }

    private boolean isRecycleViewWindowsShown() {
        return windowsView != null;
    }

    private void recycleViewWindowsHide() {
        if (WindowList.getCurrentWindow() == null)
            WindowList.createAndInsert("about:newtab", 0, false, false);
        ((ConstraintLayout) findViewById(R.id.main)).removeView(this.windowsView);
        this.windowsView = null;
        ReaderController.canPlay(true);
    }

    private void showTips() {
        View[] views = {webUrl, setting, pause, scrollToBottom};
        int[] ids = {0, 6, 1, 12};
        int[] xs = {0, 0, 0, 0};
        int[] ys = {32, 32, 32, 32};
        int[] gravities = {Gravity.TOP | Gravity.START, Gravity.BOTTOM | Gravity.END, Gravity.BOTTOM | Gravity.CENTER, Gravity.BOTTOM | Gravity.START};
        String[] contents = {getString(R.string.tip_enter_url), getString(R.string.tip_voice_setting), getString(R.string.tip_play_button), getString(R.string.tip_scroll_button)};
        GlobalTipDialog.show(MainActivity.this, views, contents, ids, xs, ys, gravities, 0, true);
    }

    private void initializeSQLite() {
        SQLite.initialize();
    }

    private void initializeRecyclerViewHistory() {
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        historySuggestionAdapter = new HistorySuggestionAdapter(new ArrayList<>(), item -> {
            String url = item.getUrl();
            setWebUrl(url);
            if (isReadingMode) readingModeSwitch();
            WindowList.webViewLoadUrl(url);
            webUrlHide();
            return Unit.INSTANCE;
        }, item -> {
            WindowList.createAndInsert(item.getUrl(), -1, true, false);
            return true;
        });
        recyclerViewHistory.setAdapter(historySuggestionAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    historySuggestionAdapter.remove(position);
                    GlobalTipDialog.show(MainActivity.this, null, getString(R.string.tip_suggestion), 11, 0, 96, Gravity.TOP | Gravity.START, false);
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerViewHistory);
    }

    private void showUrlButton() {
        linearLayoutUrlButton.setVisibility(View.VISIBLE);
        frameLayoutUrl.setVisibility(View.GONE);
    }

    private void hideUrlButton() {
        linearLayoutUrlButton.setVisibility(View.GONE);
        frameLayoutUrl.setVisibility(View.VISIBLE);
    }

    private void setNumberBox(int number) {
        numberBox.setText(String.valueOf(number));
    }

    private void showAbout() {
        View view = getLayoutInflater().inflate(R.layout.dialog_main_about, null);

        TextView version = view.findViewById(R.id.textViewVersion);
        if (BuildConfig.DEBUG) {
            version.setText(String.format("%s%s (debug)", getString(R.string.about_version), BuildConfig.VERSION_NAME));
        } else {
            version.setText(String.format("%s%s", getString(R.string.about_version), BuildConfig.VERSION_NAME));
        }
        TextView contact = view.findViewById(R.id.textViewContact);
        contact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:blueberryjoy24@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Read Aloud");
            startActivity(intent);
        });

        new AlertDialog.Builder(this).setView(view).setPositiveButton(android.R.string.ok, null).show();
    }

    @Nullable
    private List<String> getAudioDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            List<String> res = new ArrayList<>();
            for (AudioDeviceInfo device : devices) {
                String address = device.getAddress();
                if (!address.isEmpty()) res.add(address);
            }
            return res;
        } else {
            return null;
        }
    }

    private void checkBluetoothDevices() {
        //TODO needs?
        //new Handler(Looper.getMainLooper()).postDelayed(() -> {
        //List<String> list = getAudioDevices();
        //if (list != null) bluetoothDevices.removeIf(item -> !list.contains(item));
        //}, 5000);
    }

    private void initializeGoogleAds() {
        new Thread(() -> MobileAds.initialize(this, initializationStatus -> {
        })).start();
    }

    //test code
    final String getUrl = "https://blueberryjoy.alwaysdata.net/api/app_storage/get.php";
    final String setUrl = "https://blueberryjoy.alwaysdata.net/api/app_storage/set.php";

    @SuppressWarnings("unused")
    private void volleyGet() {
        if (WindowList.getCurrentWindow() == null) return;
        try {
            JSONObject JSONObj = new JSONObject().put("name", "reader").put("key", "bookmark");
            RequestQueue que = Volley.newRequestQueue(this);
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, getUrl, JSONObj, response -> {
                try {
                    if (response.has("value")) {
                        response = response.getJSONObject("value");
                        if (response.has("url") && response.has("idx")) {
                            if (isReadingMode) readingModeSwitch();
                            if (WindowList.isReading()) sendPause();
                            String url = Objects.requireNonNull(response.getString("url"));
                            WindowList.addPageToCache(url, response.getInt("idx"), null);
                            WindowList.webViewLoadUrl(url);
                        }
                    }
                } catch (JSONException e) {
                    Message.toast(e.toString());
                }
            }, e -> Message.toast(e.toString()));
            que.add(req);
        } catch (JSONException e) {
            Message.toast(e.toString());
        }
    }

    @SuppressWarnings("unused")
    private void volleySet() {
        if (WindowList.getCurrentWindow() == null) return;
        try {
            JSONObject JSONObj = new JSONObject().put("name", "reader").put("key", "bookmark").put("value", new JSONObject().put("url", WindowList.getUrl()).put("idx", WindowList.isLoadedText() ? WindowList.getIdx() : WindowList.getCachedIdx()));
            RequestQueue que = Volley.newRequestQueue(this);
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, setUrl, JSONObj, response -> {
            }, e -> Message.toast(e.toString()));
            que.add(req);
        } catch (JSONException e) {
            Message.toast(e.toString());
        }
    }
}