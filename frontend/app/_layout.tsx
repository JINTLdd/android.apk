import { Stack } from "expo-router";
import { I18nManager, StatusBar, AppState } from "react-native";
import { useEffect, useRef } from "react";
import { ThemeProvider } from "@/src/context/ThemeContext";
import { configureAudioMode } from "@/src/utils/audioPlayer";
import { ensureNotificationPermissions } from "@/src/utils/notifications";
import { checkAndAutoPlay } from "@/src/utils/autoPlay";
import { checkForUpdates, promptUpdate } from "@/src/utils/updateCheck";

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
  const appState = useRef(AppState.currentState);

  useEffect(() => {
    configureAudioMode().catch(() => {});
    ensureNotificationPermissions().catch(() => {});
    // Initial autoplay + update check
    checkAndAutoPlay().catch(() => {});
    checkForUpdates()
      .then((r) => {
        if (r?.shouldUpdate) promptUpdate(r.message, r.url, r.force);
      })
      .catch(() => {});

    // Re-check when app comes back to foreground
    const sub = AppState.addEventListener("change", (next) => {
      if (appState.current.match(/inactive|background/) && next === "active") {
        checkAndAutoPlay().catch(() => {});
      }
      appState.current = next;
    });
    return () => sub.remove();
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
