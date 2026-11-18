package com.kfasa637.oxygenfingerprint

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private data class FingerStyle(val name: String, val kind: Int, val color: Int)

class MainActivity : FragmentActivity() {
    private val styles = listOf(
        FingerStyle("OxygenOS", 0, Color.rgb(105, 180, 255)),
        FingerStyle("MIUI", 1, Color.rgb(220, 225, 235)),
        FingerStyle("NothingOS", 2, Color.WHITE),
        FingerStyle("OriginOS", 3, Color.rgb(125, 120, 255)),
        FingerStyle("Обводка", 4, Color.rgb(205, 215, 230)),
        FingerStyle("Кольцо", 5, Color.rgb(235, 240, 248)),
        FingerStyle("Рубин", 6, Color.rgb(255, 90, 125)),
        FingerStyle("Спираль", 7, Color.rgb(115, 220, 205))
    )
    private var selected = 0
    private lateinit var animationView: FingerAnimationView
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var selectedLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 11, 15))
        }
        val header = TextView(this).apply {
            text = "Отпечатки"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, dp(22), 0, dp(16))
        }
        root.addView(header, match(WRAP))

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, dp(18), dp(24))
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(matchParent(), 0, 1f))

        selectedLabel = TextView(this).apply {
            text = "Выбрано: OxygenOS"
            textSize = 14f
            setTextColor(Color.rgb(160, 190, 230))
            setPadding(0, dp(8), 0, dp(4))
        }
        body.addView(selectedLabel)
        animationView = FingerAnimationView(this)
        body.addView(animationView, LinearLayout.LayoutParams(matchParent(), dp(230)))

        val previewButton = makeButton("Предпросмотр анимации")
        previewButton.setOnClickListener { animationView.playBurst() }
        body.addView(previewButton, match(dp(50)))

        val checkButton = makeButton("Проверить настоящий отпечаток")
        checkButton.setOnClickListener { authenticate() }
        body.addView(checkButton, LinearLayout.LayoutParams(matchParent(), dp(52)).apply { topMargin = dp(10) })

        status = TextView(this).apply {
            text = "Выбери стиль. Для проверки нажми кнопку выше."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(165, 175, 192))
            setPadding(dp(4), dp(12), dp(4), dp(16))
        }
        body.addView(status)

        val title = TextView(this).apply {
            text = "КАТАЛОГ АНИМАЦИЙ"
            textSize = 13f
            setTextColor(Color.rgb(135, 145, 162))
            setPadding(0, dp(8), 0, dp(8))
        }
        body.addView(title)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(list)
        renderStyles()

        val footer = TextView(this).apply {
            text = "Устройство: ${android.os.Build.MODEL} • Android ${android.os.Build.VERSION.RELEASE}\nКаталог меняет анимацию внутри приложения. Подмена системного экрана блокировки HiOS пока не поддерживается."
            textSize = 12f
            setTextColor(Color.rgb(115, 125, 142))
            setPadding(dp(4), dp(18), dp(4), 0)
        }
        body.addView(footer)
        setContentView(root)
    }

    private fun renderStyles() {
        list.removeAllViews()
        styles.forEachIndexed { index, style ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(if (index == selected) Color.rgb(37, 48, 65) else Color.rgb(27, 29, 34))
                isClickable = true
                setOnClickListener {
                    selected = index
                    selectedLabel.text = "Выбрано: ${style.name}"
                    animationView.setStyle(style)
                    status.text = "Стиль ${style.name} выбран"
                    renderStyles()
                }
            }
            val icon = FingerIconView(this, style)
            row.addView(icon, LinearLayout.LayoutParams(dp(58), dp(58)))
            val name = TextView(this).apply {
                text = "Отпечаток \"${style.name}\""
                textSize = 17f
                setTextColor(if (index == selected) Color.rgb(100, 165, 255) else Color.WHITE)
                setPadding(dp(16), 0, 0, 0)
            }
            row.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
            if (index == selected) {
                row.addView(TextView(this).apply {
                    text = "✓"
                    textSize = 26f
                    setTextColor(Color.rgb(70, 145, 255))
                })
            }
            list.addView(row, LinearLayout.LayoutParams(matchParent(), WRAP).apply { bottomMargin = dp(5) })
        }
    }

    private fun authenticate() {
        val manager = BiometricManager.from(this)
        val can = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            status.text = when (can) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Сначала зарегистрируй отпечаток в настройках телефона."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Сканер временно недоступен."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Биометрический датчик не найден."
                else -> "Проверка отпечатка сейчас недоступна."
            }
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    status.text = "Отпечаток подтверждён • ${styles[selected].name}"
                    animationView.playBurst()
                }
                override fun onAuthenticationFailed() {
                    status.text = "Палец не распознан — попробуй ещё раз."
                    animationView.playReject()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    status.text = "Проверка завершена: $errString"
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Проверка отпечатка")
            .setSubtitle("Приложи палец к датчику")
            .setNegativeButtonText("Отмена")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    private fun makeButton(label: String) = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 15f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(34, 83, 145))
        isClickable = true
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun match(h: Int) = LinearLayout.LayoutParams(matchParent(), h)
    private fun matchParent() = ViewGroup.LayoutParams.MATCH_PARENT
    companion object { private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT }
}

private class FingerIconView(context: android.content.Context, private val style: FingerStyle) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f; val cy = height / 2f; val r = width * .32f
        p.color = style.color
        when (style.kind) {
            1, 3, 7 -> for (i in 0..3) c.drawArc(RectF(cx-r+i*4, cy-r+i*4, cx+r-i*4, cy+r-i*4), 30f+i*20, 290f-i*25, false, p)
            2, 4, 5 -> c.drawCircle(cx, cy, r, p)
            6 -> { p.strokeWidth = 4f; c.drawCircle(cx, cy, r, p); p.strokeWidth = 2f; c.drawCircle(cx, cy, r-7, p) }
            else -> { c.drawCircle(cx, cy, r, p); p.strokeWidth = 1f; c.drawCircle(cx, cy, r+5, p) }
        }
    }
}

private class FingerAnimationView(context: android.content.Context) : View(context) {
    private var style = FingerStyle("OxygenOS", 0, Color.rgb(105, 180, 255))
    private var progress = 0f
    private var active = false
    private var rejecting = false
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE; strokeWidth = 3f }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.style = Paint.Style.STROKE; strokeWidth = 5f }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 11f; textAlign = Paint.Align.CENTER }
    fun setStyle(value: FingerStyle) { style = value; invalidate() }
    fun playBurst() { rejecting = false; animateEffect() }
    fun playReject() { rejecting = true; animateEffect() }
    private fun animateEffect() {
        active = true
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (rejecting) 500 else 1050
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val x = width/2f; val y = height/2f; val r = 48f * resources.displayMetrics.density
        ring.color = style.color; glow.color = style.color
        glow.alpha = 150
        glow.setShadowLayer(18f, 0f, 0f, style.color)
        c.drawCircle(x, y, r, glow)
        c.drawCircle(x, y, r-7, ring)
        if (active) {
            for (i in 0..3) {
                val t = ((progress + i*.18f) % 1f)
                ring.alpha = ((1-t)*220).toInt()
                ring.strokeWidth = (2f + (1-t)*2f) * resources.displayMetrics.density
                c.drawCircle(x, y, r + dp(9) + t * dp(if (rejecting) 20 else 82), ring)
            }
            if (progress >= 1f) active = false
        }
        text.color = Color.rgb(185, 198, 220)
        c.drawText("ANIMATION PREVIEW", x, y+r+dp(42), text)
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
