import * as Notifications from "expo-notifications";
import { Platform } from "react-native";
import { storage } from "@/src/utils/storage";
import { getPrayerTimes, PRAYER_LABELS_AR, PrayerName } from "./prayerTimes";

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

export async function ensureNotificationPermissions(): Promise<boolean> {
  const { status } = await Notifications.getPermissionsAsync();
  if (status === "granted") return true;
  const req = await Notifications.requestPermissionsAsync();
  if (Platform.OS === "android") {
    await Notifications.setNotificationChannelAsync("adhkari-default", {
      name: "أذكاري",
      importance: Notifications.AndroidImportance.MAX,
      sound: "default",
      vibrationPattern: [0, 250, 250, 250],
    });
    await Notifications.setNotificationChannelAsync("adhan-channel", {
      name: "الأذان",
      importance: Notifications.AndroidImportance.MAX,
      sound: "default",
      vibrationPattern: [0, 500, 250, 500],
    });
  }
  return req.status === "granted";
}

export async function cancelAll() {
  await Notifications.cancelAllScheduledNotificationsAsync();
}

interface ScheduleSettings {
  morningEnabled: boolean;
  eveningEnabled: boolean;
  sleepEnabled: boolean;
  wakeupEnabled: boolean;
  duhaEnabled: boolean;
  mondayThursdayFastingEnabled: boolean;
  quranReminderEnabled: boolean;
  prayerEnabled: boolean;
  prayerOffsetMin: number;
  inactiveReminderEnabled: boolean;
  cityLat: number;
  cityLng: number;
}

export const DEFAULT_SETTINGS: ScheduleSettings = {
  morningEnabled: true,
  eveningEnabled: true,
  sleepEnabled: true,
  wakeupEnabled: true,
  duhaEnabled: true,
  mondayThursdayFastingEnabled: true,
  quranReminderEnabled: true,
  prayerEnabled: true,
  prayerOffsetMin: 10,
  inactiveReminderEnabled: true,
  cityLat: 12.7855, // Aden default
  cityLng: 45.0187,
};

async function scheduleDaily(hour: number, minute: number, title: string, body: string, id: string) {
  await Notifications.scheduleNotificationAsync({
    identifier: id,
    content: {
      title,
      body,
      sound: "default",
      ...(Platform.OS === "android" ? { channelId: "adhkari-default" } : {}),
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.CALENDAR,
      hour,
      minute,
      repeats: true,
    },
  });
}

async function scheduleWeekday(weekday: number, hour: number, minute: number, title: string, body: string, id: string) {
  // weekday: 1=Sunday..7=Saturday (Expo)
  await Notifications.scheduleNotificationAsync({
    identifier: id,
    content: {
      title,
      body,
      sound: "default",
      ...(Platform.OS === "android" ? { channelId: "adhkari-default" } : {}),
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.CALENDAR,
      weekday,
      hour,
      minute,
      repeats: true,
    },
  });
}

async function scheduleDate(date: Date, title: string, body: string, id: string, channel = "adhan-channel") {
  if (date.getTime() <= Date.now()) return;
  await Notifications.scheduleNotificationAsync({
    identifier: id,
    content: {
      title,
      body,
      sound: "default",
      ...(Platform.OS === "android" ? { channelId: channel } : {}),
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.DATE,
      date,
    },
  });
}

export async function rescheduleAll(settings: ScheduleSettings) {
  await cancelAll();

  if (settings.morningEnabled) {
    await scheduleDaily(5, 0, "أذكار الصباح 🌅", "حان وقت أذكار الصباح، ابدأ يومك بذكر الله", "morning-adhkar");
  }
  if (settings.eveningEnabled) {
    await scheduleDaily(17, 0, "أذكار المساء 🌇", "حان وقت أذكار المساء", "evening-adhkar");
  }
  if (settings.sleepEnabled) {
    await scheduleDaily(21, 0, "أذكار النوم 🌙", "اقرأ أذكار النوم قبل أن تنام", "sleep-adhkar");
  }
  if (settings.wakeupEnabled) {
    await scheduleDaily(5, 30, "أذكار الاستيقاظ ☀️", "الحمد لله الذي أحيانا، ابدأ يومك بالذكر", "wakeup-adhkar");
  }
  if (settings.quranReminderEnabled) {
    await scheduleDaily(20, 0, "تذكير قراءة القرآن 📖", "خذ ورداً من القرآن اليوم", "quran-reminder");
  }
  if (settings.mondayThursdayFastingEnabled) {
    // Monday=2, Thursday=5 (Expo weekday: 1=Sunday)
    await scheduleWeekday(2, 4, 0, "صيام الإثنين 🌙", "اليوم الإثنين، تذكر صيام التطوع", "fasting-mon");
    await scheduleWeekday(5, 4, 0, "صيام الخميس 🌙", "اليوم الخميس، تذكر صيام التطوع", "fasting-thu");
  }

  // Prayer-time based: schedule today + next 6 days
  const now = new Date();
  for (let dayOffset = 0; dayOffset < 7; dayOffset++) {
    const date = new Date(now.getTime() + dayOffset * 24 * 60 * 60 * 1000);
    const t = getPrayerTimes(settings.cityLat, settings.cityLng, date);
    const prayers: { p: PrayerName; t: Date }[] = [
      { p: "fajr", t: t.fajr },
      { p: "dhuhr", t: t.dhuhr },
      { p: "asr", t: t.asr },
      { p: "maghrib", t: t.maghrib },
      { p: "isha", t: t.isha },
    ];
    for (const { p, t: time } of prayers) {
      if (settings.prayerEnabled) {
        await scheduleDate(
          time,
          `حان وقت صلاة ${PRAYER_LABELS_AR[p]} 🕌`,
          "حي على الصلاة، حي على الفلاح",
          `prayer-${p}-${dayOffset}`,
          "adhan-channel"
        );
        if (settings.prayerOffsetMin > 0) {
          const preTime = new Date(time.getTime() - settings.prayerOffsetMin * 60 * 1000);
          await scheduleDate(
            preTime,
            `تذكير: صلاة ${PRAYER_LABELS_AR[p]} بعد ${settings.prayerOffsetMin} دقيقة`,
            "استعد للصلاة",
            `prayer-pre-${p}-${dayOffset}`
          );
        }
      }
    }
    // Duha at ~20 min after sunrise
    if (settings.duhaEnabled) {
      await scheduleDate(
        t.duha,
        "صلاة الضحى ☀️",
        "وقت صلاة الضحى، صلِّ ركعتين",
        `duha-${dayOffset}`
      );
    }
  }

  // Inactive user reminder: schedule 24h from now
  if (settings.inactiveReminderEnabled) {
    const inactiveDate = new Date(Date.now() + 24 * 60 * 60 * 1000);
    await scheduleDate(
      inactiveDate,
      "نفتقدك في أذكاري 💚",
      "لا تنسَ ذكر الله، فبذكر الله تطمئن القلوب",
      "inactive-reminder",
      "adhkari-default"
    );
  }
}

export async function refreshInactiveReminder(enabled: boolean) {
  await Notifications.cancelScheduledNotificationAsync("inactive-reminder").catch(() => {});
  if (!enabled) return;
  const inactiveDate = new Date(Date.now() + 24 * 60 * 60 * 1000);
  await Notifications.scheduleNotificationAsync({
    identifier: "inactive-reminder",
    content: {
      title: "نفتقدك في أذكاري 💚",
      body: "لا تنسَ ذكر الله، فبذكر الله تطمئن القلوب",
      sound: "default",
      ...(Platform.OS === "android" ? { channelId: "adhkari-default" } : {}),
    },
    trigger: {
      type: Notifications.SchedulableTriggerInputTypes.DATE,
      date: inactiveDate,
    },
  });
}

export async function loadSettings(): Promise<ScheduleSettings> {
  const data: ScheduleSettings = { ...DEFAULT_SETTINGS };
  for (const key of Object.keys(DEFAULT_SETTINGS) as (keyof ScheduleSettings)[]) {
    const v = await storage.getItem(`settings_${key}`, null as any);
    if (v !== null) (data as any)[key] = v;
  }
  return data;
}

export async function saveSettings(settings: ScheduleSettings) {
  for (const key of Object.keys(settings) as (keyof ScheduleSettings)[]) {
    await storage.setItem(`settings_${key}`, settings[key] as any);
  }
}

export type { ScheduleSettings };
