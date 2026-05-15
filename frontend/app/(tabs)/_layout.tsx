import { Tabs } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useTheme, COLORS } from "@/src/context/ThemeContext";
import { Platform } from "react-native";

export default function TabsLayout() {
  const { colors } = useTheme();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: COLORS.gold,
        tabBarInactiveTintColor: "rgba(255,255,255,0.55)",
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: "rgba(212,175,55,0.25)",
          borderTopWidth: 1,
          height: Platform.OS === "ios" ? 88 : 68,
          paddingBottom: Platform.OS === "ios" ? 28 : 10,
          paddingTop: 8,
        },
        tabBarLabelStyle: { fontSize: 12, fontWeight: "600" },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "الرئيسية",
          tabBarIcon: ({ color, size }) => <Ionicons name="book" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="adhan"
        options={{
          title: "الأذان",
          tabBarIcon: ({ color, size }) => <Ionicons name="alarm" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="tasbih"
        options={{
          title: "المسبحة",
          tabBarIcon: ({ color, size }) => <Ionicons name="ellipse" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="more"
        options={{
          title: "المزيد",
          tabBarIcon: ({ color, size }) => <Ionicons name="apps" color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="settings"
        options={{
          title: "الإعدادات",
          tabBarIcon: ({ color, size }) => <Ionicons name="settings" color={color} size={size} />,
        }}
      />
    </Tabs>
  );
}
