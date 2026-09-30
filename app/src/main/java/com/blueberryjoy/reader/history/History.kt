package com.blueberryjoy.reader.history

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blueberryjoy.reader.R
import com.blueberryjoy.reader.main.MainActivity
import com.blueberryjoy.reader.utils.GlobalTipDialog
import com.blueberryjoy.reader.utils.Message
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class History : AppCompatActivity() {
    var recyclerView: RecyclerView? = null
    var historyAdapter: HistoryAdapter? = null
    var popupWindow: PopupWindow? = null
    var deleted: HistoryUrl? = null
    var deletedPosition = -1
    var deletedActualIndex = -1
    var filter: EditText? = null
    var popupNum = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerView)
        filter = findViewById(R.id.filter)
        val searchButton = findViewById<ImageView?>(R.id.imageButtonSearch)
        val clearButton = findViewById<ImageView?>(R.id.imageButtonClear)

        initializeRecyclerView()

        searchButton!!.setOnClickListener { v: View ->
            v.startAnimation(AnimationUtils.loadAnimation(baseContext, R.anim.alpha))
            if (filter!!.isGone) {
                filterShow()
            } else {
                filterHide()
            }
        }

        filter!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                historyAdapter?.filter?.filter(s)
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        clearButton!!.setOnClickListener { v: View ->
            v.startAnimation(AnimationUtils.loadAnimation(baseContext, R.anim.alpha))
            AlertDialog.Builder(this)
                .setTitle(R.string.history_clear_title)
                .setMessage(R.string.history_clear_text)
                .setPositiveButton(getString(R.string.url_clear)) { _, _ ->
                    BrowsingHistory.clear()
                    initializeRecyclerView()
                    Message.toastMSG(getString(R.string.message_text_history_cleared))
                }
                .setNegativeButton(getString(R.string.button_cancel), null)
                .show()
        }

        GlobalTipDialog.show(
            this@History,
            recyclerView,
            getString(R.string.tip_history_new_tab),
            10,
            0,
            96,
            Gravity.TOP or Gravity.START,
            true
        )

        initializeAd()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && filter!!.isVisible) {
            filterHide()
            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }

    private fun initializeAd() {
        val adView = findViewById<AdView>(R.id.adView)

        adView.adListener = object : AdListener() {

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Message.log("Ad failed to load: ${adError.message}")
            }

            override fun onAdLoaded() {
                val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager ?: return

                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

                if (lastVisiblePosition == historyAdapter?.itemCount?.minus(1)) {
                    recyclerView!!.post {
                        layoutManager.scrollToPositionWithOffset(lastVisiblePosition, 0)
                    }
                }
            }
        }

        adView.loadAd(AdRequest.Builder().build())
    }

    private fun initializePopup() {
        if (deleted == null) return
        if (popupWindow != null && popupWindow!!.isShowing) popupWindow!!.dismiss()
        val inflater = LayoutInflater.from(this)
        @SuppressLint("InflateParams") val popupView = inflater.inflate(R.layout.popup_undo, null)
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow?.isOutsideTouchable = true
        popupWindow?.isFocusable = false
        popupWindow?.elevation = 10f
        popupWindow?.showAtLocation(findViewById(android.R.id.content), Gravity.BOTTOM, 0, 0)
        (popupView.findViewById<View?>(R.id.undo_notice) as TextView).text = getString(
            R.string.history_deleted_item,
            deleted!!.title
        )
        popupView.findViewById<View>(R.id.btn_undo)
            .setOnClickListener { _: View? ->
                if (deleted != null) {
                    historyAdapter?.recover(deleted!!, deletedPosition, deletedActualIndex)
                    Message.toastMSG(getString(R.string.message_text_history_restored))
                }
                deleted = null
                popupWindow?.dismiss()
            }

        val currentNum = ++popupNum
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentNum == popupNum && popupWindow?.isShowing == true) popupWindow?.dismiss()
        }, 10000)
    }

    private fun filterShow() {
        filter!!.visibility = View.VISIBLE
        filter!!.requestFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(filter, 0)
    }

    private fun filterHide() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            filter!!.windowToken,
            0
        )
        filter!!.setText("")
        filter!!.visibility = View.GONE
    }

    private fun initializeRecyclerView() {
        recyclerView?.setLayoutManager(LinearLayoutManager(this))
        historyAdapter = HistoryAdapter({ item: HistoryUrl? ->
            val intent = Intent(MainActivity.ACTIVITY_RESULT_HISTORY)
            intent.putExtra("link", item?.url)
            intent.putExtra("idx", -1)
            intent.putExtra("new page", false)
            setResult(RESULT_OK, intent)
            finish()
        }, { view: View, item: HistoryUrl?, x: Float, y: Float ->
            showUrlMenu(view, item!!.url, x, y)
            true
        })
        recyclerView?.setAdapter(historyAdapter)

        val itemTouchHelper =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun getSwipeDirs(
                    recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
                ): Int {
                    val position = viewHolder.getBindingAdapterPosition()
                    if (historyAdapter?.getItemViewType(position) == historyAdapter?.header) return 0
                    return super.getSwipeDirs(recyclerView, viewHolder)
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.getBindingAdapterPosition()
                    if (position != RecyclerView.NO_POSITION) {
                        val removed = historyAdapter?.remove(position)
                        deleted = removed?.first
                        deletedActualIndex = removed?.second!!
                        deletedPosition = position
                        initializePopup()
                    }
                }
            })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    @SuppressLint("InflateParams")
    private fun showUrlMenu(anchor: View, url: String, x: Float, y: Float) {
        val menuView = layoutInflater.inflate(R.layout.popup_url_menu, null)

        val popupWindow = PopupWindow(
            menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        menuView.findViewById<TextView>(R.id.textCopyUrl).setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("URL", url))
            popupWindow.dismiss()
        }

        menuView.findViewById<TextView>(R.id.textOpen).setOnClickListener {
            openUrl(url, false)
            popupWindow.dismiss()
        }

        menuView.findViewById<TextView>(R.id.textOpenNewWindow).setOnClickListener {
            openUrl(url, true)
            popupWindow.dismiss()
        }

        menuView.findViewById<TextView>(R.id.textOpenChrome).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
            popupWindow.dismiss()
        }

        menuView.findViewById<View>(R.id.textEditBorder).visibility = View.GONE
        menuView.findViewById<TextView>(R.id.textEdit).visibility = View.GONE

        popupWindow.showAtLocation(
            anchor,
            Gravity.TOP or Gravity.START,
            x.toInt(),
            y.toInt()
        )
    }

    private fun openUrl(url: String, newWindow: Boolean) {
        val intent = Intent(MainActivity.ACTIVITY_RESULT_HISTORY)
        intent.putExtra("link", url)
        intent.putExtra("idx", -1)
        intent.putExtra("new page", newWindow)
        setResult(RESULT_OK, intent)
        finish()
    }
}