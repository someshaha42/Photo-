package com.example.compat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Java compatibility layer providing legacy Android 5 (API 21) through Android 17 (API 37)
 * helpers, ABI/emulator detection, Java pixel fallbacks, and strict HTTPS URL verification.
 */
public final class LegacyCompatBridge {

    public static final int MIN_SUPPORTED_API_LEVEL = 21; // Android 5.0 Lollipop
    public static final int MAX_SUPPORTED_API_LEVEL = 37; // Android 17

    public static final class SdkCompatInfo {
        private final int sdkInt;
        private final String androidVersionName;
        private final List<String> supportedAbis;
        private final String primaryAbi;
        private final boolean isArm64;
        private final boolean isArm32;
        private final boolean isEmulator;
        private final String appVersionName;

        public SdkCompatInfo(
                int sdkInt,
                @NonNull String androidVersionName,
                @NonNull List<String> supportedAbis,
                @NonNull String primaryAbi,
                boolean isArm64,
                boolean isArm32,
                boolean isEmulator,
                @NonNull String appVersionName
        ) {
            this.sdkInt = sdkInt;
            this.androidVersionName = androidVersionName;
            this.supportedAbis = Collections.unmodifiableList(supportedAbis);
            this.primaryAbi = primaryAbi;
            this.isArm64 = isArm64;
            this.isArm32 = isArm32;
            this.isEmulator = isEmulator;
            this.appVersionName = appVersionName;
        }

        public int getSdkInt() {
            return sdkInt;
        }

        @NonNull
        public String getAndroidVersionName() {
            return androidVersionName;
        }

        @NonNull
        public List<String> getSupportedAbis() {
            return supportedAbis;
        }

        @NonNull
        public String getPrimaryAbi() {
            return primaryAbi;
        }

        public boolean isArm64() {
            return isArm64;
        }

        public boolean isArm32() {
            return isArm32;
        }

        public boolean isEmulator() {
            return isEmulator;
        }

        @NonNull
        public String getAppVersionName() {
            return appVersionName;
        }

        public boolean isWithinSupportedRange() {
            return sdkInt >= MIN_SUPPORTED_API_LEVEL;
        }
    }

    private LegacyCompatBridge() {
        // Utility class
    }

    @NonNull
    public static SdkCompatInfo getSdkCompatInfo(@NonNull Context context) {
        int sdkInt = Build.VERSION.SDK_INT;
        List<String> abis = getSupportedAbiList();
        String primaryAbi = abis.isEmpty() ? "armeabi-v7a" : abis.get(0);
        boolean arm64 = abis.contains("arm64-v8a");
        boolean arm32 = abis.contains("armeabi-v7a") || abis.contains("armeabi");
        boolean emulator = isRunningOnEmulator();
        String versionName = resolvePackageVersionName(context);
        return new SdkCompatInfo(
                sdkInt,
                resolveAndroidVersionLabel(sdkInt),
                abis,
                primaryAbi,
                arm64,
                arm32,
                emulator,
                versionName
        );
    }

    @NonNull
    public static String resolveAndroidVersionLabel(int sdkInt) {
        if (sdkInt >= 37) return "Android 17 (API " + sdkInt + ")";
        switch (sdkInt) {
            case 36: return "Android 16 (API 36)";
            case 35: return "Android 15 (API 35)";
            case 34: return "Android 14 (API 34)";
            case 33: return "Android 13 (API 33)";
            case 32:
            case 31: return "Android 12 (API " + sdkInt + ")";
            case 30: return "Android 11 (API 30)";
            case 29: return "Android 10 (API 29)";
            case 28: return "Android 9 (API 28)";
            case 27:
            case 26: return "Android 8 (API " + sdkInt + ")";
            case 25:
            case 24: return "Android 7 (API " + sdkInt + ")";
            case 23: return "Android 6 (API 23)";
            case 22:
            case 21: return "Android 5 (API " + sdkInt + ")";
            default: return "Android API " + sdkInt;
        }
    }

    @NonNull
    public static List<String> getSupportedAbiList() {
        String[] abis = Build.SUPPORTED_ABIS;
        if (abis != null && abis.length > 0) {
            return Arrays.asList(abis);
        }
        return Collections.singletonList("armeabi-v7a");
    }

    public static boolean isRunningOnEmulator() {
        String fingerprint = safeLower(Build.FINGERPRINT);
        String model = safeLower(Build.MODEL);
        String manufacturer = safeLower(Build.MANUFACTURER);
        String brand = safeLower(Build.BRAND);
        String device = safeLower(Build.DEVICE);
        String product = safeLower(Build.PRODUCT);
        String hardware = safeLower(Build.HARDWARE);

        return fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || fingerprint.contains("emu")
                || fingerprint.contains("robolectric")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for")
                || manufacturer.contains("genymotion")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || product.contains("sdk_gphone")
                || product.contains("emulator")
                || product.contains("simulator")
                || product.contains("vbox86p")
                || product.contains("cuttlefish")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("vbox86");
    }

    public static int getColorCompat(@NonNull Context context, @ColorRes int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(colorResId);
        }
        return ContextCompat.getColor(context, colorResId);
    }

    @Nullable
    public static Drawable getDrawableCompat(@NonNull Context context, @DrawableRes int drawableResId) {
        try {
            return ContextCompat.getDrawable(context, drawableResId);
        } catch (Resources.NotFoundException ignored) {
            return null;
        }
    }

    @NonNull
    public static String resolvePackageVersionName(@NonNull Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            String pkg = context.getPackageName();
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= 33) {
                info = pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L));
            } else {
                info = getLegacyPackageInfo(pm, pkg);
            }
            return (info != null && info.versionName != null) ? info.versionName : "1.0";
        } catch (Exception ignored) {
            return "1.0";
        }
    }

    @SuppressWarnings("deprecation")
    private static PackageInfo getLegacyPackageInfo(PackageManager pm, String pkg)
            throws PackageManager.NameNotFoundException {
        return pm.getPackageInfo(pkg, 0);
    }

    /**
     * Validates that a web URL uses strict HTTPS (no cleartext http://, javascript:, or data:).
     */
    public static boolean isStrictHttpsUrl(@Nullable String rawUrl) {
        if (rawUrl == null) {
            return false;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            return scheme != null
                    && "https".equalsIgnoreCase(scheme)
                    && host != null
                    && !host.trim().isEmpty();
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Pure Java fallback for ARGB_8888 adjustment when the C/C++ shared library is unavailable.
     */
    public static void adjustPixelsFallback(
            @NonNull int[] pixels,
            float exposure,
            float brightness,
            float contrast,
            float saturation
    ) {
        float expFactor = (float) Math.pow(2.0, clampf(exposure, -2.0f, 2.0f));
        float cFactor = 1.0f + clampf(contrast, -0.85f, 1.5f);
        float satFactor = clampf(1.0f + saturation, 0.0f, 2.5f);
        float bias = 128.0f * (1.0f - cFactor) + (brightness * 90.0f);
        boolean applySat = Math.abs(satFactor - 1.0f) > 0.001f;

        for (int i = 0; i < pixels.length; i++) {
            int px = pixels[i];
            int a = (px >>> 24) & 0xFF;
            if (a == 0) {
                continue;
            }
            float r = ((px >>> 16) & 0xFF) * expFactor * cFactor + bias;
            float g = ((px >>> 8) & 0xFF) * expFactor * cFactor + bias;
            float b = (px & 0xFF) * expFactor * cFactor + bias;

            if (applySat) {
                float luma = 0.2126f * r + 0.7152f * g + 0.0722f * b;
                r = luma + (r - luma) * satFactor;
                g = luma + (g - luma) * satFactor;
                b = luma + (b - luma) * satFactor;
            }

            int nr = clamp255(r);
            int ng = clamp255(g);
            int nb = clamp255(b);
            pixels[i] = (a << 24) | (nr << 16) | (ng << 8) | nb;
        }
    }

    /**
     * Pure Java 3x3 unsharp-mask sharpen fallback matching pf_sharpen_argb8888.
     */
    public static void sharpenPixelsFallback(
            @NonNull int[] src,
            @NonNull int[] dst,
            int width,
            int height,
            float amount
    ) {
        int expected = width * height;
        if (width <= 0 || height <= 0 || src.length < expected || dst.length < expected) {
            return;
        }
        System.arraycopy(src, 0, dst, 0, expected);
        if (width < 3 || height < 3 || amount <= 0.001f) {
            return;
        }

        float clampedAmount = clampf(amount, 0.0f, 3.0f);
        for (int y = 1; y < height - 1; y++) {
            int row = y * width;
            for (int x = 1; x < width - 1; x++) {
                int c = src[row + x];
                int t = src[row - width + x];
                int b = src[row + width + x];
                int l = src[row + x - 1];
                int r = src[row + x + 1];

                int a = (c >>> 24) & 0xFF;
                int cr = (c >>> 16) & 0xFF;
                int cg = (c >>> 8) & 0xFF;
                int cb = c & 0xFF;

                float avgR = (((t >>> 16) & 0xFF) + ((b >>> 16) & 0xFF) + ((l >>> 16) & 0xFF) + ((r >>> 16) & 0xFF)) * 0.25f;
                float avgG = (((t >>> 8) & 0xFF) + ((b >>> 8) & 0xFF) + ((l >>> 8) & 0xFF) + ((r >>> 8) & 0xFF)) * 0.25f;
                float avgB = ((t & 0xFF) + (b & 0xFF) + (l & 0xFF) + (r & 0xFF)) * 0.25f;

                int nr = clamp255(cr + (cr - avgR) * clampedAmount);
                int ng = clamp255(cg + (cg - avgG) * clampedAmount);
                int nb = clamp255(cb + (cb - avgB) * clampedAmount);
                dst[row + x] = (a << 24) | (nr << 16) | (ng << 8) | nb;
            }
        }
    }

    public static boolean scanOpaqueBoundsFallback(
            @NonNull int[] pixels,
            int width,
            int height,
            int alphaThreshold,
            @NonNull int[] outBounds4
    ) {
        if (width <= 0 || height <= 0 || pixels.length < width * height || outBounds4.length < 4) {
            return false;
        }
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int alpha = (pixels[row + x] >>> 24) & 0xFF;
                if (alpha > alphaThreshold) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return false;
        }
        outBounds4[0] = minX;
        outBounds4[1] = minY;
        outBounds4[2] = maxX;
        outBounds4[3] = maxY;
        return true;
    }

    @NonNull
    public static Bitmap ensureArgb8888(@NonNull Bitmap source) {
        if (source.getConfig() == Bitmap.Config.ARGB_8888) {
            return source;
        }
        Bitmap copy = source.copy(Bitmap.Config.ARGB_8888, false);
        return copy != null ? copy : source;
    }

    private static int clamp255(float v) {
        if (v <= 0.0f) return 0;
        if (v >= 255.0f) return 255;
        return Math.round(v);
    }

    private static float clampf(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @NonNull
    private static String safeLower(@Nullable String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }
}
