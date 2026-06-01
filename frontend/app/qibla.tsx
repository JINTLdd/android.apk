import { useState } from "react";
import { View, Text, StyleSheet, TouchableOpacity, ActivityIndicator, Alert, Platform } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { WebView } from "react-native-webview";
import { useTheme, COLORS } from "@/src/context/ThemeContext";

const QIBLA_URL = "https://qiblafinder.withgoogle.com/intl/ar/finder/ar";

export default function QiblaScreen() {
  const { colors } = useTheme();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.bg }]} edges={["top"]}>
      <View style={styles.topBar}>
        <TouchableOpacity testID="qibla-back-btn" onPress={() => router.back()} hitSlop={10}>
          <Ionicons name="chevron-forward" size={28} color="#FFFFFF" />
        </TouchableOpacity>
        <View style={styles.topBarTextWrap}>
          <Text style={styles.topBarTitle}>اتجاه القبلة</Text>
          <Text style={styles.topBarSub}>Google Qibla Finder</Text>
        </View>
        <TouchableOpacity
          testID="qibla-reload-btn"
          onPress={() => {
            setError(false);
            setLoading(true);
          }}
          hitSlop={10}
        >
          <Ionicons name="refresh" size={24} color={COLORS.gold} />
        </TouchableOpacity>
      </View>

      <View style={styles.webWrap}>
        {Platform.OS === "web" ? (
          <View style={styles.fallback}>
            <Ionicons name="compass" size={64} color={COLORS.gold} />
            <Text style={styles.fallbackTitle}>اتجاه القبلة</Text>
            <Text style={styles.fallbackText}>
              لاستخدام مكتشف القبلة بالكامل، يرجى تجربة التطبيق على هاتفك عبر Expo Go أو APK المنشور.
            </Text>
            <TouchableOpacity
              testID="open-external-btn"
              style={styles.openBtn}
              onPress={() => {
                if (typeof window !== "undefined") window.open(QIBLA_URL, "_blank");
              }}
            >
              <Text style={styles.openBtnText}>فتح في المتصفح</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <>
            <WebView
              source={{ uri: QIBLA_URL }}
              originWhitelist={["*"]}
              style={styles.webview}
              allowsInlineMediaPlayback
              javaScriptEnabled
              domStorageEnabled
              geolocationEnabled
              mediaPlaybackRequiresUserAction={false}
              onLoadStart={() => setLoading(true)}
              onLoadEnd={() => setLoading(false)}
              onError={() => {
                setError(true);
                setLoading(false);
              }}
              onHttpError={() => {
                setError(true);
                setLoading(false);
              }}
              setSupportMultipleWindows={false}
              userAgent={
                Platform.OS === "android"
                  ? "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"
                  : undefined
              }
            />
            {loading && (
              <View style={styles.loadingOverlay} pointerEvents="none">
                <ActivityIndicator size="large" color={COLORS.gold} />
                <Text style={styles.loadingText}>جاري التحميل...</Text>
              </View>
            )}
            {error && (
              <View style={styles.errorBox}>
                <Ionicons name="cloud-offline" size={48} color={COLORS.gold} />
                <Text style={styles.errorTitle}>تعذر التحميل</Text>
                <Text style={styles.errorText}>تأكد من اتصالك بالإنترنت ثم أعد المحاولة</Text>
              </View>
            )}
          </>
        )}
      </View>

      <Text style={styles.hint}>
        💡 ابتعد عن الأجهزة المعدنية والمغناطيسية واسمح بالوصول للموقع للحصول على اتجاه دقيق
      </Text>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  topBar: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 12,
  },
  topBarTextWrap: { flex: 1, alignItems: "center" },
  topBarTitle: { color: "#FFFFFF", fontSize: 20, fontWeight: "700" },
  topBarSub: { color: "#CBD5E1", fontSize: 12, marginTop: 2 },
  webWrap: { flex: 1, backgroundColor: "#FFFFFF", marginHorizontal: 12, borderRadius: 16, overflow: "hidden" },
  webview: { flex: 1 },
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(0,0,0,0.55)",
    alignItems: "center",
    justifyContent: "center",
  },
  loadingText: { color: "#FFFFFF", fontSize: 14, marginTop: 12 },
  errorBox: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(45,36,56,0.95)",
    alignItems: "center",
    justifyContent: "center",
    padding: 24,
  },
  errorTitle: { color: "#FFFFFF", fontSize: 20, fontWeight: "700", marginTop: 14 },
  errorText: { color: "#CBD5E1", fontSize: 14, marginTop: 8, textAlign: "center" },
  hint: { color: "#CBD5E1", fontSize: 12, textAlign: "center", padding: 12 },
  fallback: { flex: 1, alignItems: "center", justifyContent: "center", padding: 32, backgroundColor: "#1a1a1a" },
  fallbackTitle: { color: "#FFFFFF", fontSize: 24, fontWeight: "800", marginTop: 16 },
  fallbackText: { color: "#CBD5E1", fontSize: 15, marginTop: 12, textAlign: "center", lineHeight: 24 },
  openBtn: {
    backgroundColor: COLORS.gold,
    paddingVertical: 14,
    paddingHorizontal: 32,
    borderRadius: 14,
    marginTop: 24,
  },
  openBtnText: { color: "#1a1a1a", fontSize: 16, fontWeight: "800" },
});
