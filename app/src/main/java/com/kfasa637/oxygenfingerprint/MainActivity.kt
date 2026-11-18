package com.kfasa637.oxygenfingerprint

import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class MainActivity : FragmentActivity() {
    private lateinit var status: TextView
    private lateinit var preview: FingerprintBurstView
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        executor = ContextCompat.getMainExecutor(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(Color.rgb(7, 9, 14))
        }

        root.addView(TextView(this).apply {
            text = "Oxygen Fingerprint"
            textSize = 29f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }, match(WRAP))
        root.addView(TextView(this).apply {
            text = "OxygenOS-inspired fingerprint experience"
            textSize = 14f
            setTextColor(Color.rgb(158, 169, 188))
            setPadding(0, dp(6), 0, dp(4))
        }, match(WRAP))

        preview = FingerprintBurstView(this)
        root.addView(preview, LinearLayout.LayoutParams(matchParent(), dp(270)))

        val device = "УСТРОЙСТВО\n${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\nAndroid ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})"
        root.addView(section(device))

        val hasFingerprint = packageManager.hasSystemFeature("android.hardware.fingerprint")
        val canAuth = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        val biometricText = when (canAuth) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Биометрия готова к проверке"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Отпечаток не зарегистрирован"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Совместимое биометрическое оборудование не найдено"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Датчик временно недоступен"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Требуется обновление безопасности"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Сильная биометрия не поддерживается"
            else -> "Не удалось определить состояние биометрии"
        }
        root.addView(section("ОТПЕЧАТОК: ${if (hasFingerprint) "ОБНАРУЖЕН" else "НЕ ОБНАРУЖЕН"}\n$biometricText"))

        val action = Button(this).apply {
            text = "ПРОВЕРИТЬ ОТПЕЧАТОК"
            isAllCaps = false
            textSize = 16f
            setOnClickListener { startBiometricCheck() }
        }
        root.addView(action, LinearLayout.LayoutParams(matchParent(), dp(54)).apply {
            topMargin = dp(16)
        })

        status = TextView(this).apply {
            text = "Нажми кнопку и приложи зарегистрированный палец к датчику"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(170, 181, 200))
            setPadding(0, dp(14), 0, dp(8))
        }
        root.addView(status, match(WRAP))

        root.addView(TextView(this).apply {
            text = "Анимация запускается после реального ответа Android BiometricPrompt. Приложение не заменяет системный экран блокировки HiOS и не меняет системную анимацию."
            textSize = 12f
            setTextColor(Color.rgb(125, 136, 155))
            setPadding(0, dp(18), 0, 0)
        }, match(WRAP))

        setContentView(root)
    }

    private fun startBiometricCheck() {
        val manager = BiometricManager.from(this)
        val result = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (result != BiometricManager.BIOMETRIC_SUCCESS) {
            status.text = "Биометрия сейчас недоступна. Проверь регистрацию отпечатка в настройках телефона."
            return
        }
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                status.text = "Отпечаток подтверждён"
                preview.playSuccessBurst()
            }

            override fun onAuthenticationFailed() {
                status.text = "Отпечаток не распознан. Попробуй ещё раз."
                preview.playRetryPulse()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                status.text = "Проверка завершена: $errString"
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Oxygen Fingerprint")
            .setSubtitle("Подтверди личность отпечатком пальца")
            .setNegativeButtonText("Отмена")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    private fun section(value: String) = TextView(this).apply {
        text = value
        textSize = 15f
        setTextColor(Color.rgb(215, 223, 237))
        setPadding(0, dp(14), 0, dp(8))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun match(height: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    private fun matchParent() = ViewGroup.LayoutParams.MATCH_PARENT
    companion object { private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT }
}

private class FingerprintBurstView(context: android.content.Context) : View(context) {
    private val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = Color.rgb(95, 105, 125)
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.rgb(100, 175, 255)
        setShadowLayer(18f, 0f, 0f, Color.rgb(35, 115, 255))
    }
    private val ripple = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(125, 190, 255)
    }
    private var progress = 0f
    private var burst = false
    private var pulse = false


    fun playSuccessBurst() {
        burst = true
        pulse = false
        animate().cancel()
        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 1050
        animator.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    fun playRetryPulse() {
        burst = false
        pulse = true
        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 450
        animator.addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = 53f * resources.displayMetrics.density
        canvas.drawCircle(cx, cy, r + dp(9), base)
        canvas.drawCircle(cx, cy, r, glow)
        if (burst) {
            for (i in 0..2) {
                val p = (progress + i * 0.22f) % 1f
                ripple.alpha = ((1f - p) * 220).toInt()
                ripple.strokeWidth = dp(2) * (1f - p * .45f)
                canvas.drawCircle(cx, cy, r + dp(8) + p * dp(75), ripple)
            }
            if (progress >= 1f) burst = false
        } else if (pulse) {
            ripple.alpha = ((1f - progress) * 220).toInt()
            canvas.drawCircle(cx, cy, r + dp(8) + progress * dp(22), ripple)
            if (progress >= 1f) pulse = false
        }
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(210, 222, 242)
            textSize = dp(10).toFloat()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText("OXYGENOS STYLE", cx, cy + r + dp(43), label)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
