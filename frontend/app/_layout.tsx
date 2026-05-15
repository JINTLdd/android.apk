import { Stack } from "expo-router";
import { I18nManager, StatusBar } from "react-native";
import { useEffect } from "react";
import { ThemeProvider } from "@/src/context/ThemeContext";
import { configureAudioMode } from "@/src/utils/audioPlayer";
import { ensureNotificationPermissions } from "@/src/utils/notifications";

// Force RTL
if (!I18nManager.isRTL) {
  try {
    I18nManager.allowRTL(true);
    I18nManager.forceRTL(true);
  } catch {
    // ignore
  }
}

export default function RootLayout() {
  useEffect(() => {
    configureAudioMode().catch(() => {});
    ensureNotificationPermissions().catch(() => {});
  }, []);

  return (
    <ThemeProvider>
      <StatusBar barStyle="light-content" />
      <Stack screenOptions={{ headerShown: false, animation: "fade" }}>
        <Stack.Screen name="index" />
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="session/[category]" options={{ presentation: "card" }} />
      </Stack>
    </ThemeProvider>
  );
}
