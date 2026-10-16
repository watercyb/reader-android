package com.blueberryjoy.reader.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.bookmark.BookmarkItem;
import com.blueberryjoy.reader.bookmark.BookmarkList;
import com.blueberryjoy.reader.downloads.DownloadsRecyclerViewItems;
import com.blueberryjoy.reader.downloads.DownloadsRecyclerViewNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class FileStorage {
    private static final String[] illegalChar = {"/", "\n", "\r", "\t", "\0", "\f", "`", "?", "*", "\\", "<", ">", "|", "\"", ":", "<", ">", "+", "[", "]", ".", " "};
    private static String dir = "";
    @NonNull
    private static String JSCode = "";

    public static void setDir(String folder) {
        dir = folder;
    }

    public static String getDir() {
        return dir;
    }

    private static boolean checkFileExist(@NonNull String fullFilename) {
        File file = new File(fullFilename);
        return file.exists();
    }

    public static boolean checkDirExist(@NonNull String fullDir) {
        File dir = new File(fullDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return dir.mkdirs();
        }
        return true;
    }

    private static String getNameFromUri(@NonNull Context context, @NonNull Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String fileName = cursor.getString(nameIndex);
            cursor.close();
            return fileName;
        } else {
            return context.getString(R.string.error_text_unable_retrieve_file_name);
        }
    }

    @NonNull
    private static String fileNameValidation(String filename) {
        for (String str : illegalChar) {
            filename = filename.replace(str, "");
        }
        return filename;
    }

    @Nullable
    public static String deleteDownloadedFile(@NonNull Context context, String filename) {
        File file = new File(dir + "/downloads/" + filename);
        if (!file.exists()) return null;
        String content = getDownloadedFileContent(file);
        if (content == null) return null;
        if (new File(dir + "/downloads/" + filename).delete()) {
            Message.toastMSG(context.getString(R.string.message_text_download_deleted));
            return content;
        } else {
            Message.toastErr(context.getString(R.string.error_unable_delete_file));
            return null;
        }
    }

    @Nullable
    private static String getDownloadedFileContent(File file) {
        StringBuilder SB = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                SB.append(line).append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return SB.toString();
    }

    public static void DownloadPage(@NonNull Context context, @NonNull String url, String title, @NonNull String[] readingArr, int idx, boolean isAppend) {
        try {
            FileStorage.checkDirExist(dir + "/downloads/");
            File file = new File(dir + "/downloads/" + idx + '@' + FileStorage.fileNameValidation(title) + '@' + FileStorage.urlToFilename(url) + ".html");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file, isAppend));
            StringBuilder SB = new StringBuilder();
            SB.append("<title>").append(title).append("</title>");
            for (String str : readingArr) {
                SB.append(str).append("<br>");
            }
            outputStreamWriter.write(SB.toString());
            outputStreamWriter.close();
            Message.toastMSG(context.getString(R.string.message_text_downloaded));
        } catch (IOException e) {
            Message.toastErr(context.getString(R.string.error_text_unable_download_file));
        }
    }

    public static boolean undoDownloadDeletion(@NonNull String filename, @NonNull String content) {
        try {
            FileStorage.checkDirExist(dir + "/downloads/");
            File file = new File(dir + "/downloads/" + filename);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file, false));
            outputStreamWriter.write(content);
            outputStreamWriter.close();
            return true;
        } catch (IOException e) {
            Message.toastErr("undo error");
            return false;
        }
    }

    @NonNull
    public static DownloadsRecyclerViewItems getDownloadsList() {
        DownloadsRecyclerViewItems items = new DownloadsRecyclerViewItems();
        if (checkDirExist(dir + "/downloads/")) {
            for (File file : Objects.requireNonNull(new File(dir + "/downloads/").listFiles())) {
                String filename = file.getName();
                if (filename.length() <= 6) continue;
                String[] detail = filename.substring(0, filename.length() - 5).split("@");
                if (detail.length == 3) {
                    String itemName;
                    int idx;
                    if (TextUtils.isDigitsOnly(detail[0])) {
                        itemName = detail[1] + " (" + detail[0] + ")";
                        idx = Integer.parseInt(detail[0]);
                    } else {
                        itemName = detail[1] + " (0)";
                        idx = 0;
                    }
                    items.insert(new DownloadsRecyclerViewNode(itemName, filename, detail[1], Favicon.getIconFromK(detail[2]), idx, file.lastModified()));
                }
            }
            items.sortByName();
        }
        return items;
    }

    @NonNull
    private static JSONArray getBookmarkJSONFromFile() {
        try {
            if (!checkFileExist(dir + "/bookmark.txt")) return new JSONArray();
            InputStream inputStream = App.Companion.getAppContext().openFileInput("bookmark.txt");
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = bufferedReader.readLine();
                inputStream.close();
                return new JSONArray(receiveString);
            }
        } catch (FileNotFoundException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_unable_locate_file));
        } catch (IOException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_unable_access_file));
        } catch (JSONException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_unsupported_file_format));
        }
        return new JSONArray();
    }

    @NonNull
    private static JSONObject getJSONObject(String url, String title, int idx, @Nullable String type) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url", url);
        jsonObject.put("title", title);
        jsonObject.put("idx", idx);
        if (type != null) jsonObject.put("type", type);
        return jsonObject;
    }

    @NonNull
    private static JSONObject getJSONObject(@NonNull BookmarkItem bookmarkItem) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url", bookmarkItem.getLink());
        jsonObject.put("title", bookmarkItem.getTitle());
        jsonObject.put("idx", bookmarkItem.getIdx());
        if (bookmarkItem.getType() != null) jsonObject.put("type", bookmarkItem.getType());
        return jsonObject;
    }

    //Called from bookmark.
    public static void appendAndSaveBookmarkJSON(@NonNull BookmarkList bookmarkList, @NonNull String url, String title, int idx, String type) {
        BookmarkItem bookmarkItem = new BookmarkItem(title + " (" + idx + ")", Favicon.getIconFromUrl(url), url, idx, type);
        bookmarkList.add(bookmarkItem);
        if (saveBookmarkJSONToFile(getJSONArray(bookmarkList.getItems())))
            Message.toastMSG(App.Companion.getAppContext().getString(R.string.message_text_bookmark_saved));
    }

    //Restore deleted bookmark.
    public static boolean appendAndSaveBookmarkJSON(@NonNull BookmarkList bookmarkList, @NonNull BookmarkItem deleted) {
        int restoreIdx = deleted.getOriIdx();
        deleted.setOriIdx(-1);
        if (restoreIdx < 0 || restoreIdx > bookmarkList.length())
            restoreIdx = bookmarkList.length();
        bookmarkList.add(restoreIdx, deleted);
        return saveBookmarkJSONToFile(getJSONArray(bookmarkList.getItems()));
    }

    //Called from main.
    public static void appendAndSaveBookmarkJSON(String url, String title, int idx, String type) {
        try {
            JSONArray jsonArray = getBookmarkJSONFromFile();
            jsonArray.put(getJSONObject(url, title, idx, type));
            if (saveBookmarkJSONToFile(jsonArray))
                Message.toastMSG(App.Companion.getAppContext().getString(R.string.message_text_bookmark_saved));
        } catch (JSONException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_invalid_format));
        }
    }

    public static boolean saveBookmarkJSON(@NonNull BookmarkList bookmarkList) {
        return saveBookmarkJSONToFile(getJSONArray(bookmarkList.getItems()));
    }

    public static boolean saveBookmarkJSONToFile(@NonNull List<BookmarkItem> bookmarkItems) {
        return saveBookmarkJSONToFile(getJSONArray(bookmarkItems));
    }

    private static boolean saveBookmarkJSONToFile(@NonNull JSONArray jsonArray) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(App.Companion.getAppContext().openFileOutput("bookmark.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(jsonArray.toString());
            outputStreamWriter.close();
            return true;
        } catch (IOException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_failed_save_file));
        }
        return false;
    }

    public static void importBookmarkJSONRewrite(@NonNull JSONArray jsonArrayNew) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(App.Companion.getAppContext().openFileOutput("bookmark.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(jsonArrayNew.toString());
            outputStreamWriter.close();
            Message.toastMSG(App.Companion.getAppContext().getString(R.string.message_done));
        } catch (IOException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_invalid_format));
        }
    }

    public static void importBookmarkJSONAppend(@NonNull JSONArray jsonArrayNew) {
        try {
            JSONArray jsonArray = FileStorage.getBookmarkJSONFromFile();
            for (int i = 0; i < jsonArrayNew.length(); i++) {
                jsonArray.put(jsonArrayNew.getJSONObject(i));
            }
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(App.Companion.getAppContext().openFileOutput("bookmark.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(jsonArray.toString());
            outputStreamWriter.close();
            Message.toastMSG(App.Companion.getAppContext().getString(R.string.message_done));
        } catch (IOException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_failed_save_file));
        } catch (JSONException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_invalid_format));
        }
    }

    @Nullable
    public static JSONArray importBookmarkJSON(@NonNull Uri uri) {
        try {
            InputStream inputStream = App.Companion.getAppContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                char[] buffer = new char[1024];
                StringBuilder SB = new StringBuilder();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                int size = inputStreamReader.read(buffer, 0, buffer.length);
                while (size >= 0) {
                    SB.append(buffer, 0, size);
                    size = inputStreamReader.read(buffer, 0, buffer.length);
                }
                inputStream.close();
                JSONArray jsonArrayFromFile = new JSONArray(SB.toString());
                JSONArray jsonArrayNew = new JSONArray();
                for (int i = 0; i < jsonArrayFromFile.length(); i++) {
                    JSONObject jsonObject = jsonArrayFromFile.getJSONObject(i);
                    if (jsonObject.length() >= 3 && jsonObject.has("url") && jsonObject.get("url") instanceof String && jsonObject.has("title") && jsonObject.get("title") instanceof String && jsonObject.has("idx") && jsonObject.get("idx") instanceof Integer)
                        jsonArrayNew.put(jsonObject);
                }
                if (jsonArrayNew.length() == 0) {
                    Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_no_bookmarks_found));
                    return null;
                }
                return jsonArrayNew;
            }
        } catch (IOException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_unable_access_file));
        } catch (JSONException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_invalid_format));
        }
        return null;
    }

    public static void exportBookmarkJSON(@NonNull Context context, @NonNull Uri uri) {
        try {
            App.Companion.getAppContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            OutputStream outputStream = App.Companion.getAppContext().getContentResolver().openOutputStream(uri, "rwt");
            JSONArray jsonArray = getBookmarkJSONFromFile();
            if (outputStream != null) {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                outputStreamWriter.write(jsonArray.toString());
                outputStreamWriter.flush();
                outputStream.close();
            }
            Message.toastMSG(App.Companion.getAppContext().getString(R.string.message_text_bookmarks_exported_to) + getNameFromUri(context, uri) + ".");
        } catch (IOException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_failed_save_file));
        }
    }

    @NonNull
    public static BookmarkList getBookmarkListFromFile() {
        BookmarkList bookmarkList = new BookmarkList();
        List<BookmarkItem> list = new ArrayList<>();
        try {
            JSONArray jsonArray = getBookmarkJSONFromFile();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.length() >= 3 && jsonObject.has("url") && jsonObject.get("url") instanceof String && jsonObject.has("title") && jsonObject.get("title") instanceof String && jsonObject.has("idx") && jsonObject.get("idx") instanceof Integer) {
                    int idx = jsonObject.getInt("idx");
                    String title = jsonObject.getString("title") + " (" + idx + ")";
                    String url = jsonObject.getString("url");
                    Bitmap icon = Favicon.getIconFromUrl(url);
                    String type = null;
                    if (jsonObject.has("type") && jsonObject.get("type") instanceof String)
                        type = jsonObject.getString("type");
                    BookmarkItem bookmarkItem = new BookmarkItem(title, icon, url, idx, type);
                    list.add(bookmarkItem);
                    jsonObject.put("hashcode", bookmarkItem.hashCode());
                }
            }
            bookmarkList.setItems(list);
        } catch (JSONException e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_invalid_format));
        }
        return bookmarkList;
    }

    @NonNull
    public static HashSet<String> getTypesFromFile() {
        HashSet<String> types = new HashSet<>();
        JSONArray jsonArray = getBookmarkJSONFromFile();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (jsonObject.length() == 4 && jsonObject.has("type") && jsonObject.get("type") instanceof String) {
                    types.add(jsonObject.getString("type"));
                }
            } catch (JSONException ignored) {
            }
        }
        return types;
    }

    @NonNull
    public static JSONArray getJSONArray(@NonNull List<BookmarkItem> list) {
        JSONArray jsonArray = new JSONArray();
        boolean hasFormatError = false;
        for (BookmarkItem bookmarkItem : list) {
            try {
                if (bookmarkItem.getItem() != null) jsonArray.put(getJSONObject(bookmarkItem));
            } catch (JSONException e) {
                hasFormatError = true;
            }
        }
        if (hasFormatError)
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_bookmark_invalid_format));
        return jsonArray;
    }

    public static void saveFavicon(@NonNull Bitmap icon, @NonNull String url) {
        try {
            if (!checkDirExist(dir + "/favicons/")) throw new Exception();
            icon = resizeFavicon(icon);
            String filename = urlToFilename(url);
            Favicon.setIcon(filename, icon);
            File file = new File(dir + "/favicons/" + filename + ".png");
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            icon.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_unable_save_icon));
        }
    }

    @NonNull
    private static Bitmap resizeFavicon(@NonNull Bitmap bitmap) {
        int maxWidth = 128;
        int maxHeight = 128;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxWidth && height <= maxHeight) return bitmap;
        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    @Nullable
    public static Bitmap getIconFromFile(String filename) {
        String path = dir + "/favicons/" + filename + ".png";
        if (checkFileExist(path)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            return BitmapFactory.decodeFile(path, options);
        } else {
            return null;
        }
    }

    @NonNull
    public static String urlToFilename(@NonNull String url) {
        try {
            if (url.startsWith("file") && url.length() > 5) {
                String[] detail = url.substring(0, url.length() - 5).split("@");
                if (detail.length == 3) return detail[2];
            }
            String[] strArr = new URL(url).getHost().split("\\.");
            if (strArr.length <= 1) return "null";
            return FileStorage.fileNameValidation(strArr[strArr.length - 1] + strArr[strArr.length - 2]);
        } catch (MalformedURLException e) {
            return "null";
        }
    }

    @NonNull
    public static String getJSCode(@NonNull String target, @NonNull String num) {
        if (JSCode.isEmpty()) {
            try {
                InputStream stream = App.Companion.getAppContext().getAssets().open("reader.js");
                InputStreamReader inputStreamReader = new InputStreamReader(stream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;
                StringBuilder SB = new StringBuilder();
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains("//")) line = line.split("//")[1];
                    SB.append(line).append(System.lineSeparator());
                }
                bufferedReader.close();
                inputStreamReader.close();
                stream.close();
                JSCode = SB.toString();
            } catch (IOException e) {
                return JSCode = e.toString();
            }
        }
        return JSCode.replaceFirst(target, num);
    }
}
