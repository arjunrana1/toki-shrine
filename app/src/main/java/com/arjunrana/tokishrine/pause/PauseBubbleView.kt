package com.arjunrana.tokishrine.pause

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.arjunrana.tokishrine.R
import com.arjunrana.tokishrine.ui.icons.Ph
import com.arjunrana.tokishrine.ui.theme.NocturneAccent
import com.arjunrana.tokishrine.ui.theme.NocturneAccentRamp300
import com.arjunrana.tokishrine.ui.theme.NocturneBg
import com.arjunrana.tokishrine.ui.theme.NocturneNeutral100
import com.arjunrana.tokishrine.ui.theme.NocturneNeutral300
import kotlin.math.abs

/**
 * The draggable pause pill (PRD §6 screen 20): hourglass glyph, the open
 * block's name and the remaining time. Plain Android views, not Compose —
 * a service-owned overlay window has no composition lifecycle — with values
 * from the shared Nocturne tokens and the mock's pill shape.
 *
 * The view owns its overlay windows (the pill plus the non-touchable bottom
 * dismissal hint) and its drag/tap gesture; the host service supplies
 * content and interaction callbacks. Children are non-interactive, so the
 * whole gesture is handled here.
 */
class PauseBubbleView(context: Context) : LinearLayout(context) {

    interface Host {
        fun onBubbleTap(blockId: Long)
        fun onBubbleDragged()

        /**
         * Owner-approved dismissal (Phase 6 addendum): released over the
         * bottom discard zone. Called synchronously at release so the host
         * captures the displayed block and live pause instances before the
         * cosmetic exit animation can change them (P6C-R1).
         */
        fun onBubbleDismissalDecided(displayedBlockId: Long): BubbleDismissalSnapshot

        /** Applies the release-time snapshot once the exit animation completes. */
        fun onBubbleDismissed(snapshot: BubbleDismissalSnapshot)
    }

    private val windowManager: WindowManager? = context.getSystemService(WindowManager::class.java)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    private val iconView = TextView(context).apply {
        typeface = ResourcesCompat.getFont(context, R.font.phosphor_regular)
        text = String(Character.toChars(Ph.HourglassMedium))
        setTextColor(tokenColor(NocturneAccentRamp300))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    }

    private val nameView = TextView(context).apply {
        setTextColor(tokenColor(NocturneNeutral300))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        maxLines = 1
    }

    private val timeView = TextView(context).apply {
        typeface = ResourcesCompat.getFont(context, R.font.inter_medium)
        setTextColor(tokenColor(NocturneNeutral100))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
    }

    // Bottom-edge discard hint (owner refinement, 23 September): a centred,
    // non-touchable overlay label shown while the pill is dragged; it tints
    // accent when the pill is over the discard zone. It sits on a faded,
    // semi-opaque Nocturne chip (owner correction P6C-O7, same day) so the
    // label stays readable over light content behind the overlay.
    private val hintView = TextView(context).apply {
        text = context.getString(R.string.pause_bubble_dismiss)
        typeface = ResourcesCompat.getFont(context, R.font.inter_medium)
        setTextColor(tokenColor(NocturneNeutral100))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(6), dp(14), dp(6))
        background = GradientDrawable().apply {
            setColor(tokenColor(NocturneBg, alpha255 = 200))
            cornerRadius = dp(18).toFloat()
        }
        setShadowLayer(6f, 0f, 0f, Color.BLACK)
        alpha = 0f
    }

    private val hintParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = dp(HINT_BOTTOM_DP)
    }

    private var host: Host? = null
    private var shownBlockId: Long = -1
    private var downRawX = 0f
    private var downRawY = 0f
    private var downX = 0
    private var downY = 0
    private var dragging = false
    private var overDiscardZone = false
    private var hintAttached = false

    init {
        orientation = HORIZONTAL
        setPadding(dp(11), dp(8), dp(14), dp(8))
        background = GradientDrawable().apply {
            // Mock: the bg token at 92% opacity over whatever is behind.
            setColor(tokenColor(NocturneBg, alpha255 = 235))
            cornerRadius = dp(26).toFloat()
            setStroke(dp(1), tokenColor(NocturneAccent))
        }
        elevation = dp(6).toFloat()
        val gap = dp(8)
        addView(
            iconView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                rightMargin = gap
                gravity = Gravity.CENTER_VERTICAL
            },
        )
        addView(
            nameView,
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = gap
                gravity = Gravity.CENTER_VERTICAL
            },
        )
        addView(
            timeView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            },
        )
    }

    /** Adds the overlay window; a failed add (permission, OEM) keeps the pause fully functional without the bubble. */
    fun attach(host: Host): Boolean {
        this.host = host
        val manager = windowManager ?: return false
        params.x = 0
        params.y = dp(INITIAL_TOP_DP)
        val added = runCatching { manager.addView(this, params) }.isSuccess
        if (!added) {
            this.host = null
            return false
        }
        // The hint is cosmetic: a failed add only loses the label, never the
        // pill or the dismissal itself.
        hintAttached = runCatching { manager.addView(hintView, hintParams) }.isSuccess
        // The pill starts at the mock's top-right position; width is
        // measurable only after the first layout pass.
        post {
            params.x = (screenWidth() - width - dp(16)).coerceAtLeast(0)
            runCatching { manager.updateViewLayout(this, params) }
        }
        return true
    }

    fun detach() {
        host = null
        runCatching { windowManager?.removeView(this) }
        if (hintAttached) {
            runCatching { windowManager?.removeView(hintView) }
            hintAttached = false
        }
    }

    fun isAttached(): Boolean = parent != null

    fun showContent(blockId: Long, blockName: String, clockText: String) {
        shownBlockId = blockId
        nameView.text = blockName
        timeView.text = clockText
    }

    fun displayedBlockId(): Long = shownBlockId

    override fun performClick(): Boolean {
        super.performClick()
        if (shownBlockId >= 0) host?.onBubbleTap(shownBlockId)
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                downX = params.x
                downY = params.y
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    dragging = true
                    showHint()
                }
                if (dragging) {
                    params.x = (downX + dx.toInt()).coerceIn(0, (screenWidth() - width).coerceAtLeast(0))
                    params.y = (downY + dy.toInt()).coerceIn(0, (screenHeight() - height).coerceAtLeast(0))
                    runCatching { windowManager?.updateViewLayout(this, params) }
                    setDiscardAffordance(inDiscardZone())
                }
            }
            MotionEvent.ACTION_UP -> {
                if (dragging && inDiscardZone()) {
                    // P6C-R1: the decision — displayed block plus the host's
                    // live pause instances — is captured now, before the
                    // animation; renders during those 140 ms cannot fold a
                    // new pause into the dismissal or shift its attribution.
                    val snapshot = host?.onBubbleDismissalDecided(shownBlockId)
                    animateOutThen { if (snapshot != null) host?.onBubbleDismissed(snapshot) }
                } else if (dragging) {
                    setDiscardAffordance(false)
                    hideHint()
                    host?.onBubbleDragged()
                } else {
                    performClick()
                }
                dragging = false
            }
            MotionEvent.ACTION_CANCEL -> {
                setDiscardAffordance(false)
                hideHint()
                dragging = false
            }
        }
        // The pill is the interaction surface; claiming the stream keeps the
        // gesture whole from DOWN to UP.
        return true
    }

    /**
     * The discard zone is the bottom strip of the screen (Phase 6 owner
     * addendum): releasing the drag there reads as throwing the pill away,
     * matching common floating-button behavior.
     */
    private fun inDiscardZone(): Boolean =
        height > 0 && params.y + height >= screenHeight() - dp(DISCARD_ZONE_DP)

    private fun setDiscardAffordance(over: Boolean) {
        if (over == overDiscardZone) return
        overDiscardZone = over
        pivotX = width / 2f
        pivotY = height / 2f
        // Over the zone the pill goes very transparent (owner refinement,
        // 23 September) so the centred Dismiss label reads as the target.
        val targetScale = if (over) 0.9f else 1f
        val targetAlpha = if (over) 0.2f else 1f
        animate().scaleX(targetScale).scaleY(targetScale).alpha(targetAlpha)
            .setDuration(AFFORDANCE_MS).start()
        hintView.setTextColor(tokenColor(if (over) NocturneAccent else NocturneNeutral100))
    }

    private fun showHint() {
        if (!hintAttached) return
        hintView.animate().alpha(0.95f).setDuration(AFFORDANCE_MS).start()
    }

    private fun hideHint() {
        if (!hintAttached) return
        hintView.animate().alpha(0f).setDuration(AFFORDANCE_MS).start()
    }

    private fun animateOutThen(end: () -> Unit) {
        pivotX = width / 2f
        pivotY = height / 2f
        animate().scaleX(0.85f).scaleY(0.85f).alpha(0f)
            .setDuration(DISMISS_ANIMATION_MS)
            .withEndAction(end)
            .start()
    }

    private fun screenWidth() = resources.displayMetrics.widthPixels
    private fun screenHeight() = resources.displayMetrics.heightPixels

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)
            .toInt()

    /** Nocturne token → view color; the optional alpha carries the mock's 92% pill. */
    private fun tokenColor(token: androidx.compose.ui.graphics.Color, alpha255: Int = 255): Int =
        Color.argb(alpha255, (token.red * 255).toInt(), (token.green * 255).toInt(), (token.blue * 255).toInt())

    private companion object {
        const val INITIAL_TOP_DP = 32
        const val DISCARD_ZONE_DP = 72
        const val HINT_BOTTOM_DP = 28
        const val AFFORDANCE_MS = 80L
        const val DISMISS_ANIMATION_MS = 140L
    }
}
