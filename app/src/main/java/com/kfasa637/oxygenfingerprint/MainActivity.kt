package com.kfasa637.oxygenfingerprint

import android.app.Activity
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.biometric.BiometricManager

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(Color.rgb(8, 10, 16))
        }
        root.addView(TextView(this).apply {
            text = "Oxygen Fingerprint"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        })
        root.addView(TextView(this).apply {
            text = "HiOS compatibility prototype"
            textSize = 14f
            setTextColor(Color.rgb(165, 174, 190))
            setPadding(0, 8, 0, 18)
        })
        root.addView(FingerprintPreview(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 300
        ))
        val deviceInfo = "DEVICE\n${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\nAndroid ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})"
        root.addView(TextView(this).apply {
            text = deviceInfo
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 18, 0, 14)
        })
        val hasFingerprint = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val biometricStatus = when (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Strong biometric authentication is available."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Hardware/API available, but no biometric is enrolled."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Android reports no supported biometric hardware."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is temporarily unavailable."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "A security update is required."
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Strong biometric authentication is not supported."
            else -> "Biometric status could not be determined."
        }
        root.addView(TextView(this).apply {
            text = "FINGERPRINT FEATURE: ${if (hasFingerprint) "YES" else "NOT REPORTED"}\n\nBIOMETRIC CHECK\n$biometricStatus"
            textSize = 14f
            setTextColor(Color.rgb(205, 214, 228))
            setPadding(0, 8, 0, 16)
        })
        root.addView(TextView(this).apply {
            text = "Preview only. This app does not replace the HiOS lock-screen fingerprint interface or simulate a successful unlock."
            textSize = 12f
            setTextColor(Color.rgb(145, 155, 174))
        })
        setContentView(root)
    }
}

private class FingerprintPreview(context: android.content.Context) : View(context) {
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
        color = Color.rgb(125, 133, 148)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.rgb(76, 155, 255)
        setShadowLayer(18f, 0f, 0f, Color.rgb(38, 120, 255))
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
        color = Color.rgb(95, 174, 255)
    }
    private var phase = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 2400L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = 66f
        canvas.drawCircle(cx, cy, radius + 12f, ringPaint)
        canvas.drawCircle(cx, cy, radius, glowPaint)
        val bounds = RectF(cx - radius - 12f, cy - radius - 12f, cx + radius + 12f, cy + radius + 12f)
        canvas.drawArc(bounds, phase, 105f, false, arcPaint)
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 230, 245)
            textSize = 12f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        canvas.drawText("ANIMATION PREVIEW", cx, cy + radius + 48f, label)
    }
}
