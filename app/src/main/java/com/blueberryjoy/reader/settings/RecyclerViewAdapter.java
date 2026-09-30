package com.blueberryjoy.reader.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.speech.tts.Voice;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.utils.TextToSpeechManager;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    @NonNull
    private final List<RecyclerViewItem> recyclerViewList = new ArrayList<>();
    private final HashMap<String, Boolean> isShown = new HashMap<>();
    private final LayoutInflater layoutInflater;
    private ItemClickListener itemClickListener;
    public static final int TYPE_DIVIDER = 0;
    public static final int TYPE_ITEM = 1;

    // This list holds the positions from the original list that should be displayed
    @NonNull
    final
    List<Integer> displayIndexes = new ArrayList<>();
    private final Bitmap icon;
    private int voiceDisplayIdx;
    private int voiceActualIdx;

    RecyclerViewAdapter(Context context) {
        this.layoutInflater = LayoutInflater.from(context);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        icon = BitmapFactory.decodeResource(App.Companion.getAppContext().getResources(), R.mipmap.ic_voicesetting_star, options);
        update();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_ITEM) {
            view = layoutInflater.inflate(R.layout.recyclerview_bookmark_item, parent, false);
            return new ViewHolder(view);
        } else {
            view = layoutInflater.inflate(R.layout.recyclerview_bookmark_divider, parent, false);
            return new ViewHolderDivider(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return recyclerViewList.get(displayIndexes.get(position)).isItem() ? TYPE_ITEM : TYPE_DIVIDER;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int actualIndex = displayIndexes.get(position);
        if (actualIndex == voiceActualIdx) voiceDisplayIdx = holder.getBindingAdapterPosition();
        RecyclerViewItem item = recyclerViewList.get(actualIndex);

        if (holder instanceof ViewHolder) {
            if (item != null) {
                ((ViewHolder) holder).textView.setText(item.title);
                ((ViewHolder) holder).imageView.setImageBitmap(item.icon);
                ((ViewHolder) holder).category.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(view -> {
                if (itemClickListener != null && item != null) {
                    recyclerViewList.get(voiceActualIdx).icon = null;
                    item.icon = icon;
                    itemClickListener.onItemClick(item.voiceIdx);
                    notifyItemChanged(voiceDisplayIdx);
                    notifyItemChanged(holder.getBindingAdapterPosition());
                    voiceActualIdx = actualIndex;
                    voiceDisplayIdx = holder.getBindingAdapterPosition();
                }
            });
        } else {
            if (item != null) {
                ((ViewHolderDivider) holder).type.setText(App.Companion.getAppContext().getResources().getString(getLanguageNameRes(item.category)));
                ((ViewHolderDivider) holder).count.setText(String.valueOf(item.count));
                ((ViewHolderDivider) holder).image.setImageResource(Boolean.TRUE.equals(isShown.getOrDefault(item.category, false)) ? R.mipmap.ic_bookmark_open : R.mipmap.ic_bookmark_close);
            }
            holder.itemView.setOnClickListener(view -> {
                assert item != null;
                String category = item.category;
                if (isShown.containsKey(category)) {
                    isShown.put(category, Boolean.FALSE.equals(isShown.get(item.category)));
                    refreshDisplayIndexes();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayIndexes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;
        final ImageView imageView;
        final View category;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            imageView = itemView.findViewById(R.id.imageView);
            category = itemView.findViewById(R.id.category);
        }
    }

    public static class ViewHolderDivider extends RecyclerView.ViewHolder {
        final TextView type;
        final TextView count;
        final ImageView image;

        ViewHolderDivider(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.type);
            count = itemView.findViewById(R.id.count);
            image = itemView.findViewById(R.id.image);
        }
    }

    public void setClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshDisplayIndexes() {
        displayIndexes.clear();
        for (int i = 0; i < recyclerViewList.size(); i++) {
            RecyclerViewItem item = recyclerViewList.get(i);
            if (!item.isItem() || Boolean.TRUE.equals(isShown.getOrDefault(item.category, false)))
                displayIndexes.add(i);
        }
        notifyDataSetChanged();
    }

    public int getVoiceIdx() {
        return voiceDisplayIdx;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void update() {
        List<Voice> voiceList = TextToSpeechManager.getVoiceList();

        Context context = App.Companion.getAppContext();
        List<Pair<Voice, String>> sorted = new ArrayList<>(voiceList.size());
        for (Voice voice : voiceList) {
            String locale = voice.getLocale().toString();
            String languageCode = locale.split("_")[0];
            String key = context.getString(getLanguageNameRes(languageCode)) + locale + ": " + voice.getName();
            sorted.add(new Pair<>(voice, key));
        }
        Locale appLocale = AppCompatDelegate.getApplicationLocales().get(0);
        if (appLocale == null)
            appLocale = Locale.getDefault();
        Collator collator = Collator.getInstance(appLocale);
        sorted.sort(Comparator.comparing(a -> a.second, collator));

        voiceList.clear();
        for (Pair<Voice, String> pair : sorted) {
            voiceList.add(pair.first);
        }

        String voiceName = ReaderSettings.getVoiceName();
        recyclerViewList.clear();
        isShown.clear();
        RecyclerViewItem prv = null;
        int count = 0;
        for (int i = 0; i < voiceList.size(); i++) {
            Voice voice = voiceList.get(i);
            String category = voice.getLocale().toString().split("_")[0];
            if (prv == null || !prv.category.equals(category)) {
                if (prv != null)
                    prv.count = count;
                prv = new RecyclerViewItem(voice, null, -1, 0);
                recyclerViewList.add(prv);
                isShown.put(category, false);
                count = 0;
            }
            if (voice.getName().equals(voiceName)) {
                recyclerViewList.add(new RecyclerViewItem(voice, icon, i, 0));
                prv.icon = icon;
                isShown.put(category, true);
            } else {
                recyclerViewList.add(new RecyclerViewItem(voice, null, i, 0));
            }
            count++;
        }
        if (prv != null) prv.count = count;
        displayIndexes.clear();
        voiceDisplayIdx = 0;
        voiceActualIdx = 0;
        for (int i = 0; i < recyclerViewList.size(); i++) {
            RecyclerViewItem item = recyclerViewList.get(i);
            if (!item.isItem() || Boolean.TRUE.equals(isShown.getOrDefault(item.category, false))) {
                displayIndexes.add(i);
                if (item.name.equals(voiceName)) {
                    voiceActualIdx = i;
                    voiceDisplayIdx = displayIndexes.size() - 1;
                }
            }
        }
        notifyDataSetChanged();
    }

    private int getLanguageNameRes(String languageCode) {
        return switch (languageCode) {
            case "ar" -> R.string.language_ar;
            case "as" -> R.string.language_as;
            case "bg" -> R.string.language_bg;
            case "bn" -> R.string.language_bn;
            case "brx" -> R.string.language_brx;
            case "bs" -> R.string.language_bs;
            case "ca" -> R.string.language_ca;
            case "cs" -> R.string.language_cs;
            case "cy" -> R.string.language_cy;
            case "da" -> R.string.language_da;
            case "de" -> R.string.language_de;
            case "doi" -> R.string.language_doi;
            case "el" -> R.string.language_el;
            case "en" -> R.string.language_en;
            case "es" -> R.string.language_es;
            case "et" -> R.string.language_et;
            case "fi" -> R.string.language_fi;
            case "fil" -> R.string.language_fil;
            case "fr" -> R.string.language_fr;
            case "gu" -> R.string.language_gu;
            case "he" -> R.string.language_he;
            case "hi" -> R.string.language_hi;
            case "hr" -> R.string.language_hr;
            case "hu" -> R.string.language_hu;
            case "id" -> R.string.language_id;
            case "is" -> R.string.language_is;
            case "it" -> R.string.language_it;
            case "ja" -> R.string.language_ja;
            case "jv" -> R.string.language_jv;
            case "km" -> R.string.language_km;
            case "kn" -> R.string.language_kn;
            case "ko" -> R.string.language_ko;
            case "kok" -> R.string.language_kok;
            case "ks" -> R.string.language_ks;
            case "lt" -> R.string.language_lt;
            case "lv" -> R.string.language_lv;
            case "mai" -> R.string.language_mai;
            case "ml" -> R.string.language_ml;
            case "mni" -> R.string.language_mni;
            case "mr" -> R.string.language_mr;
            case "ms" -> R.string.language_ms;
            case "nb" -> R.string.language_nb;
            case "ne" -> R.string.language_ne;
            case "nl" -> R.string.language_nl;
            case "or" -> R.string.language_or;
            case "pa" -> R.string.language_pa;
            case "pl" -> R.string.language_pl;
            case "pt" -> R.string.language_pt;
            case "ro" -> R.string.language_ro;
            case "ru" -> R.string.language_ru;
            case "sa" -> R.string.language_sa;
            case "sat" -> R.string.language_sat;
            case "sd" -> R.string.language_sd;
            case "si" -> R.string.language_si;
            case "sk" -> R.string.language_sk;
            case "sl" -> R.string.language_sl;
            case "sq" -> R.string.language_sq;
            case "sr" -> R.string.language_sr;
            case "su" -> R.string.language_su;
            case "sv" -> R.string.language_sv;
            case "sw" -> R.string.language_sw;
            case "ta" -> R.string.language_ta;
            case "te" -> R.string.language_te;
            case "th" -> R.string.language_th;
            case "tr" -> R.string.language_tr;
            case "uk" -> R.string.language_uk;
            case "ur" -> R.string.language_ur;
            case "vi" -> R.string.language_vi;
            case "yue" -> R.string.language_yue;
            case "zh" -> R.string.language_zh;
            default -> R.string.language_unknown;
        };
    }

    public interface ItemClickListener {
        void onItemClick(int idx);
    }
}
