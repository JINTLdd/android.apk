import { useEffect, useRef, useState } from "react";
import { View, Text, StyleSheet, TouchableOpacity, Animated } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import * as Haptics from "expo-haptics";
import { useTheme, COLORS } from "@/src/context/ThemeContext";
import { tasbihDhikrs } from "@/src/data/otherAdhkar";
import { storage } from "@/src/utils/storage";

export default function TasbihScreen() {
  const { colors } = useTheme();
  const [index, setIndex] = useState(0);
  const [count, setCount] = useState(0);
  const [totalToday, setTotalToday] = useState(0);
  const [hapticEnabled, setHapticEnabled] = useState(true);
  const scaleRef = useRef(new Animated.Value(1));
  const scale = scaleRef.current;

  const current = tasbihDhikrs[index];
  const progress = Math.min(count / current.target, 1);

  useEffect(() => {
    (async () => {
      const savedTotal = await storage.getItem<number>("tasbih_total", 0);
      setTotalToday(savedTotal || 0);
      const haptic = await storage.getItem<boolean>("settings_vibration", true);
      setHapticEnabled(haptic !== false);
    })();
  }, []);

  const handleTap = async () => {
    Animated.sequence([
      Animated.timing(scale, { toValue: 0.92, duration: 80, useNativeDriver: true }),
      Animated.timing(scale, { toValue: 1, duration: 120, useNativeDriver: true }),
    ]).start();

    const newCount = count + 1;
    const newTotal = totalToday + 1;
    setTotalToday(newTotal);
    storage.setItem("tasbih_total", newTotal);

    if (hapticEnabled) {
      try {
        await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      } catch {
        // ignore
      }
    }

    if (newCount >= current.target) {
      // Reached target
      if (hapticEnabled) {
        try {
          await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
          setTimeout(() => Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => {}), 200);
        } catch {
          // ignore
        }
      }
      if (index === tasbihDhikrs.length - 1) {
        // Done all
        setCount(newCount);
        setTimeout(() => {
          setIndex(0);
          setCount(0);
        }, 1500);
      } else {
        setTimeout(() => {
          setIndex(index + 1);
          setCount(0);
        }, 600);
      }
    } else {
      setCount(newCount);
    }
  };

  const handleReset = () => {
    setIndex(0);
    setCount(0);
  };

  const handleResetTotal = () => {
    setTotalToday(0);
    storage.setItem("tasbih_total", 0);
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.bg }]} edges={["top"]}>
      <View style={styles.header}>
        <Text style={styles.title}>المسبحة الإلكترونية</Text>
        <View style={styles.statsRow}>
          <View style={[styles.statChip, { backgroundColor: colors.surface }]}>
            <Text style={styles.statLabel}>الذكر الحالي</Text>
            <Text style={styles.statValue}>
              {index + 1} / {tasbihDhikrs.length}
            </Text>
          </View>
          <View style={[styles.statChip, { backgroundColor: colors.surface }]}>
            <Text style={styles.statLabel}>المجموع اليوم</Text>
            <Text style={styles.statValue}>{totalToday}</Text>
          </View>
        </View>
      </View>

      <View style={[styles.dhikrLabel, { backgroundColor: colors.surface }]}>
        <Text style={styles.dhikrLabelText}>{current.text}</Text>
        <Text style={styles.dhikrLabelTarget}>الهدف: {current.target}</Text>
      </View>

      <View style={styles.counterArea}>
        <Animated.View style={{ transform: [{ scale }] }}>
          <TouchableOpacity
            testID="tasbih-button"
            style={[
              styles.bigButton,
              { backgroundColor: colors.surface, borderColor: COLORS.gold },
            ]}
            onPress={handleTap}
            activeOpacity={0.9}
          >
            <View style={styles.progressRing}>
              <View style={[styles.progressFill, { width: `${progress * 100}%` }]} />
            </View>
            <Text style={styles.bigNumber}>{count}</Text>
            <Text style={styles.bigTarget}>من {current.target}</Text>
          </TouchableOpacity>
        </Animated.View>
      </View>

      <View style={styles.bottomButtons}>
        <TouchableOpacity
          testID="reset-tasbih-btn"
          style={[styles.actionBtn, { backgroundColor: colors.surface }]}
          onPress={handleReset}
        >
          <Ionicons name="refresh" size={20} color={COLORS.gold} />
          <Text style={styles.actionBtnText}>إعادة من البداية</Text>
        </TouchableOpacity>
        <TouchableOpacity
          testID="reset-total-btn"
          style={[styles.actionBtn, { backgroundColor: colors.surface }]}
          onPress={handleResetTotal}
        >
          <Ionicons name="trash-outline" size={20} color={COLORS.gold} />
          <Text style={styles.actionBtnText}>تصفير المجموع</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { padding: 20, paddingBottom: 12 },
  title: { color: "#FFFFFF", fontSize: 24, fontWeight: "800", textAlign: "center", marginBottom: 14 },
  statsRow: { flexDirection: "row", gap: 10 },
  statChip: {
    flex: 1,
    padding: 14,
    borderRadius: 16,
    alignItems: "center",
    borderColor: "rgba(212,175,55,0.25)",
    borderWidth: 1,
  },
  statLabel: { color: "#CBD5E1", fontSize: 12, marginBottom: 4 },
  statValue: { color: COLORS.goldLight, fontSize: 22, fontWeight: "800" },
  dhikrLabel: {
    marginHorizontal: 20,
    padding: 20,
    borderRadius: 20,
    alignItems: "center",
    borderColor: "rgba(212,175,55,0.3)",
    borderWidth: 1,
  },
  dhikrLabelText: { color: "#FFFFFF", fontSize: 28, fontWeight: "700", textAlign: "center" },
  dhikrLabelTarget: { color: "#CBD5E1", fontSize: 13, marginTop: 6 },
  counterArea: { flex: 1, alignItems: "center", justifyContent: "center" },
  bigButton: {
    width: 220,
    height: 220,
    borderRadius: 110,
    aspectRatio: 1,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 4,
    overflow: "hidden",
    elevation: 8,
  },
  progressRing: { ...StyleSheet.absoluteFillObject, backgroundColor: "rgba(212,175,55,0.15)" },
  progressFill: { ...StyleSheet.absoluteFillObject, backgroundColor: "rgba(212,175,55,0.4)" },
  bigNumber: { color: "#FFFFFF", fontSize: 64, fontWeight: "800" },
  bigTarget: { color: "#E2E8F0", fontSize: 16, marginTop: -4 },
  bottomButtons: { flexDirection: "row", padding: 20, gap: 10 },
  actionBtn: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    padding: 14,
    borderRadius: 14,
    gap: 8,
    borderColor: "rgba(255,255,255,0.08)",
    borderWidth: 1,
  },
  actionBtnText: { color: "#FFFFFF", fontSize: 14, fontWeight: "600" },
});
