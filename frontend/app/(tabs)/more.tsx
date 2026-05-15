import { useState } from "react";
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useTheme, COLORS } from "@/src/context/ThemeContext";
import { miscAdhkar, istiftahAdhkar } from "@/src/data/otherAdhkar";
import { DhikrItem } from "@/src/data/morningAdhkar";
import { playAudio, stopCurrent } from "@/src/utils/audioPlayer";

type Section = "misc" | "istiftah";

export default function MoreScreen() {
  const { colors } = useTheme();
  const [section, setSection] = useState<Section>("misc");
  const [playingId, setPlayingId] = useState<number | null>(null);
  const [loadingId, setLoadingId] = useState<number | null>(null);

  const data = section === "misc" ? miscAdhkar : istiftahAdhkar;

  const handlePlay = async (item: DhikrItem) => {
    if (!item.audioUrl) return;
    if (playingId === item.id) {
      await stopCurrent();
      setPlayingId(null);
      return;
    }
    setLoadingId(item.id);
    await playAudio(item.audioUrl, {
      onStatus: (s) => {
        if (!s.isPlaying) setPlayingId((p) => (p === item.id ? null : p));
      },
      onFinish: () => setPlayingId(null),
    });
    setLoadingId(null);
    setPlayingId(item.id);
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: colors.bg }]} edges={["top"]}>
      <View style={styles.header}>
        <Text style={styles.title}>المزيد من الأذكار</Text>
      </View>

      <View style={styles.tabsRow}>
        <TouchableOpacity
          testID="tab-misc"
          style={[
            styles.tab,
            { backgroundColor: colors.surface },
            section === "misc" && { backgroundColor: COLORS.gold },
          ]}
          onPress={() => setSection("misc")}
        >
          <Text style={[styles.tabText, section === "misc" && { color: "#1a1a1a", fontWeight: "800" }]}>
            أذكار متنوعة
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          testID="tab-istiftah"
          style={[
            styles.tab,
            { backgroundColor: colors.surface },
            section === "istiftah" && { backgroundColor: COLORS.gold },
          ]}
          onPress={() => setSection("istiftah")}
        >
          <Text style={[styles.tabText, section === "istiftah" && { color: "#1a1a1a", fontWeight: "800" }]}>
            أذكار الاستفتاح
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.scroll}>
        {data.map((item) => (
          <View
            key={`${section}-${item.id}`}
            style={[styles.card, { backgroundColor: colors.surface }]}
          >
            <Text style={styles.cardTitle}>{item.title}</Text>
            <View style={styles.divider} />
            <Text style={styles.cardText}>{item.text}</Text>
            {item.audioUrl ? (
              <TouchableOpacity
                testID={`play-${section}-${item.id}`}
                style={styles.playBtn}
                onPress={() => handlePlay(item)}
              >
                {loadingId === item.id ? (
                  <ActivityIndicator color="#1a1a1a" />
                ) : (
                  <Ionicons
                    name={playingId === item.id ? "pause" : "play"}
                    size={22}
                    color="#1a1a1a"
                  />
                )}
                <Text style={styles.playBtnText}>
                  {playingId === item.id ? "إيقاف" : "استمع"}
                </Text>
              </TouchableOpacity>
            ) : null}
          </View>
        ))}
        <View style={{ height: 30 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { padding: 20, paddingBottom: 12 },
  title: { color: "#FFFFFF", fontSize: 24, fontWeight: "800", textAlign: "center" },
  tabsRow: { flexDirection: "row", paddingHorizontal: 20, gap: 10, marginBottom: 12 },
  tab: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 14,
    alignItems: "center",
    borderColor: "rgba(255,255,255,0.08)",
    borderWidth: 1,
  },
  tabText: { color: "#FFFFFF", fontSize: 14, fontWeight: "600" },
  scroll: { padding: 20 },
  card: {
    padding: 18,
    borderRadius: 18,
    marginBottom: 12,
    borderColor: "rgba(255,255,255,0.08)",
    borderWidth: 1,
  },
  cardTitle: { color: COLORS.goldLight, fontSize: 16, fontWeight: "700", textAlign: "center" },
  divider: { height: 1, backgroundColor: "rgba(212,175,55,0.25)", marginVertical: 10 },
  cardText: { color: "#FFFFFF", fontSize: 17, lineHeight: 32, textAlign: "center", marginBottom: 12 },
  playBtn: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: COLORS.gold,
    paddingVertical: 12,
    borderRadius: 12,
    gap: 8,
  },
  playBtnText: { color: "#1a1a1a", fontSize: 15, fontWeight: "800" },
});
