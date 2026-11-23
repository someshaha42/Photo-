package com.example.compat

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DeviceFormFactor(val displayName: String) {
    PHONE("Phone"),
    TABLET("Tablet"),
    WEAR_OS("Wear OS"),
    TELEVISION("Android TV"),
    AUTOMOTIVE("Android Automotive"),
    XR("Android XR"),
    VIRTUAL_EMULATOR("Virtual Android")
}

@Immutable
data class DeviceEnvironmentSnapshot(
    val primaryFormFactor: DeviceFormFactor,
    val isWearOs: Boolean,
    val isTelevision: Boolean,
    val isAutomotive: Boolean,
    val isXrDevice: Boolean,
    val isTablet: Boolean,
    val isEmulator: Boolean,
    val sdkInt: Int,
    val androidVersionLabel: String,
    val supportedAbis: List<String>,
    val nativeBackendLabel: String,
    val isRoundScreen: Boolean,
    val recommendedMaxContentWidth: Dp,
    val needsInAppBackButton: Boolean,
    val compactChrome: Boolean
) {
    val supportsArm64: Boolean get() = "arm64-v8a" in supportedAbis
    val supportsArm32: Boolean get() = "armeabi-v7a" in supportedAbis || "armeabi" in supportedAbis
    val isWideScreen: Boolean
        get() = primaryFormFactor == DeviceFormFactor.TABLET ||
            primaryFormFactor == DeviceFormFactor.TELEVISION ||
            primaryFormFactor == DeviceFormFactor.AUTOMOTIVE ||
            primaryFormFactor == DeviceFormFactor.XR
}

object DeviceEnvironment {

    private const val FEATURE_AUTOMOTIVE = "android.hardware.type.automotive"
    private const val FEATURE_LEANBACK = "android.software.leanback"
    private const val FEATURE_WATCH = "android.hardware.type.watch"
    private const val FEATURE_XR_SPATIAL = "android.hardware.xr.api.spatial"
    private const val FEATURE_XR_OPENXR = "android.hardware.xr.api.openxr"

    fun detect(
        context: Context,
        availWidthDp: Float,
        availHeightDp: Float
    ): DeviceEnvironmentSnapshot {
        val pm = context.packageManager
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val uiModeType = uiModeManager?.currentModeType ?: Configuration.UI_MODE_TYPE_NORMAL
        val config = context.resources.configuration

        val hasWatch = hasFeature(pm, FEATURE_WATCH) ||
            uiModeType == Configuration.UI_MODE_TYPE_WATCH
        val hasTv = hasFeature(pm, FEATURE_LEANBACK) ||
            uiModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasAuto = hasFeature(pm, FEATURE_AUTOMOTIVE) ||
            uiModeType == Configuration.UI_MODE_TYPE_CAR
        val hasXr = hasFeature(pm, FEATURE_XR_SPATIAL) ||
            hasFeature(pm, FEATURE_XR_OPENXR)
        val isRound = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            config.isScreenRound
        } else {
            false
        }
        val emulator = LegacyCompatBridge.isRunningOnEmulator()
        val compat = LegacyCompatBridge.getSdkCompatInfo(context)

        return classify(
            availWidthDp = availWidthDp,
            availHeightDp = availHeightDp,
            hasWatchFeature = hasWatch,
            hasTvFeature = hasTv,
            hasAutomotiveFeature = hasAuto,
            hasXrFeature = hasXr,
            isEmulator = emulator,
            isRoundScreen = isRound,
            sdkInt = compat.sdkInt,
            androidVersionLabel = compat.androidVersionName,
            supportedAbis = compat.supportedAbis,
            nativeBackendLabel = NativePixelEngine.backendSummary()
        )
    }

    /**
     * Pure classification function enabling deterministic unit testing across all device targets.
     */
    fun classify(
        availWidthDp: Float,
        availHeightDp: Float,
        hasWatchFeature: Boolean = false,
        hasTvFeature: Boolean = false,
        hasAutomotiveFeature: Boolean = false,
        hasXrFeature: Boolean = false,
        isEmulator: Boolean = false,
        isRoundScreen: Boolean = false,
        sdkInt: Int = Build.VERSION.SDK_INT,
        androidVersionLabel: String = LegacyCompatBridge.resolveAndroidVersionLabel(sdkInt),
        supportedAbis: List<String> = LegacyCompatBridge.getSupportedAbiList(),
        nativeBackendLabel: String = NativePixelEngine.backendSummary()
    ): DeviceEnvironmentSnapshot {
        val minDim = minOf(availWidthDp, availHeightDp)
        val isCompactWatch = hasWatchFeature || minDim < 270f
        val isTablet = !isCompactWatch && !hasTvFeature && !hasAutomotiveFeature && !hasXrFeature && availWidthDp >= 600f

        val primary = when {
            hasXrFeature -> DeviceFormFactor.XR
            hasWatchFeature || minDim < 270f -> DeviceFormFactor.WEAR_OS
            hasAutomotiveFeature -> DeviceFormFactor.AUTOMOTIVE
            hasTvFeature -> DeviceFormFactor.TELEVISION
            isTablet -> DeviceFormFactor.TABLET
            isEmulator -> DeviceFormFactor.VIRTUAL_EMULATOR
            else -> DeviceFormFactor.PHONE
        }

        val maxContentWidth = when (primary) {
            DeviceFormFactor.WEAR_OS -> 280.dp
            DeviceFormFactor.XR,
            DeviceFormFactor.TELEVISION,
            DeviceFormFactor.AUTOMOTIVE,
            DeviceFormFactor.TABLET -> 840.dp
            DeviceFormFactor.PHONE,
            DeviceFormFactor.VIRTUAL_EMULATOR -> 600.dp
        }

        return DeviceEnvironmentSnapshot(
            primaryFormFactor = primary,
            isWearOs = isCompactWatch,
            isTelevision = hasTvFeature,
            isAutomotive = hasAutomotiveFeature,
            isXrDevice = hasXrFeature,
            isTablet = isTablet,
            isEmulator = isEmulator,
            sdkInt = sdkInt,
            androidVersionLabel = androidVersionLabel,
            supportedAbis = supportedAbis,
            nativeBackendLabel = nativeBackendLabel,
            isRoundScreen = isRoundScreen,
            recommendedMaxContentWidth = maxContentWidth,
            needsInAppBackButton = hasAutomotiveFeature || hasTvFeature || hasXrFeature,
            compactChrome = isCompactWatch
        )
    }

    private fun hasFeature(pm: PackageManager, feature: String): Boolean =
        runCatching { pm.hasSystemFeature(feature) }.getOrDefault(false)
}

@Composable
fun rememberDeviceEnvironment(availWidthDp: Float, availHeightDp: Float): DeviceEnvironmentSnapshot {
    val context = LocalContext.current
    return remember(context, availWidthDp, availHeightDp) {
        DeviceEnvironment.detect(context, availWidthDp, availHeightDp)
    }
}
