import { useEffect, useRef, useState } from "react";
import { View, Text, StyleSheet, TouchableOpacity, Animated, Platform, Linking } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import * as Location from "expo-location";
import { useTheme, COLORS } from "@/src/context/ThemeContext";
import { PatternBackground } from "@/src/components/PatternBackground";

// Kaaba in Makkah
const KAABA_LAT = 21.4225;
const KAABA_LNG = 39.8262;

function toRad(deg: number): number {
  return (deg * Math.PI) / 180;
}
function toDeg(rad: number): number {
  return (rad * 180) / Math.PI;
}

function calculateQiblaBearing(lat: number, lng: number): number {
  const phiK = toRad(KAABA_LAT);
  const lambdaK = toRad(KAABA_LNG);
  const phi = toRad(lat);
  const lambda = toRad(lng);
  const y = Math.sin(lambdaK - lambda);
  const x = Math.cos(phi) * Math.tan(phiK) - Math.sin(phi) * Math.cos(lambdaK - lambda);
  let bearing = toDeg(Math.atan2(y, x));
  if (bearing < 0) bearing += 360;
  return bearing;
}

function distanceKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export default function QiblaScreen() {
  const { colors } = useTheme();
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null);
  const [heading, setHeading] = useState(0);
  const [qiblaBearing, setQiblaBearing] = useState(0);
  const [permissionError, setPermissionError] = useState<string | null>(null);
  const [sensorAvailable, setSensorAvailable] = useState(true);
  const rotation = useRef(new Animated.Value(0)).current;
  const lastHeadingRef = useRef(0);

  // Get user location and request location permission
  useEffect(() => {
    (async () => {
      const perm = await Location.requestForegroundPermissionsAsync();
      if (perm.status !== "granted") {
        setPermissionError("location");
        return;
      }
      try {
        const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
        const lat = loc.coords.latitude;
        const lng = loc.coords.longitude;
        setCoords({ lat, lng });
        setQiblaBearing(calculateQiblaBearing(lat, lng));
      } catch {
        setPermissionError("location");
      }
    })();
  }, []);

  // Subscribe to device heading via Location API (more reliable cross-platform
  // than raw Magnetometer; uses calibrated compass sensor fusion)
  useEffect(() => {
    let sub: Location.LocationSubscription | null = null;
    (async () => {
      try {
        sub = await Location.watchHeadingAsync((h) => {
          // Use trueHeading if available, else magHeading
          const v = h.trueHeading >= 0 ? h.trueHeading : h.magHeading;
          if (v < 0) return;
          const diff = Math.abs(v - lastHeadingRef.current);
          if (diff > 1.5 || diff > 350) {
            lastHeadingRef.current = v;
            setHeading(v);
          }
        });
      } catch {
        setSensorAvailable(false);
      }
    })();
    return () => {
      sub?.remove();
    };
  }, []);

  // Animate rotation: arrow shows qiblaBearing relative to current heading
  useEffect(() => {
    const target = qiblaBearing - heading;
    Animated.timing(rotation, {
      toValue: target,
      duration: 250,
      useNativeDriver: true,
    }).start();
  }, [heading, qiblaBearing, rotation]);

  const rotateStyle = {
    transform: [
      {
        rotate: rotation.interpolate({
          inputRange: [-720, 720],
          outputRange: ["-720deg", "720deg"],
        }),
      },
    ],
  };

  const compassRoseStyle = {
    transform: [
      {
        rotate: `${-heading}deg`,
      },
    ],
  };

  const dist = coords ? Math.round(distanceKm(coords.lat, coords.lng, KAABA_LAT, KAABA_LNG)) : 0;
  const angleDiff = Math.round(((qiblaBearing - heading + 360) % 360));
  const isAligned = angleDiff < 5 || angleDiff > 355;

  return (
    <PatternBackground>
      <SafeAreaView style={styles.container} edges={["top", "bottom"]}>
        <View style={styles.topBar}>
          <TouchableOpacity testID="qibla-back-btn" onPress={() => router.back()} hitSlop={10}>
            <Ionicons name="chevron-forward" size={28} color="#FFFFFF" />
          </TouchableOpacity>
          <View style={styles.topBarTextWrap}>
            <Text style={styles.topBarTitle}>اتجاه القبلة</Text>
            <Text style={styles.topBarSub}>بوصلة الكعبة المشرفة</Text>
          </View>
          <View style={{ width: 28 }} />
        </View>

        {permissionError === "location" ? (
          <View style={styles.errorBox}>
            <Ionicons name="location-outline" size={64} color={COLORS.gold} />
            <Text style={styles.errorTitle}>إذن الموقع مطلوب</Text>
            <Text style={styles.errorText}>لحساب اتجاه القبلة بدقة من موقعك</Text>
            <TouchableOpacity
              style={styles.openBtn}
              onPress={() => Linking.openSettings().catch(() => {})}
            >
              <Text style={styles.openBtnText}>فتح الإعدادات</Text>
            </TouchableOpacity>
          </View>
        ) : !sensorAvailable ? (
          <View style={styles.errorBox}>
            <Ionicons name="compass-outline" size={64} color={COLORS.gold} />
            <Text style={styles.errorTitle}>البوصلة غير متوفرة</Text>
            <Text style={styles.errorText}>جهازك لا يحتوي على حساس مغناطيسي</Text>
          </View>
        ) : (
          <>
            <View style={styles.infoRow}>
              <View style={styles.infoChip}>
                <Text style={styles.infoLabel}>اتجاه القبلة</Text>
                <Text style={styles.infoValue}>{Math.round(qiblaBearing)}°</Text>
              </View>
              <View style={styles.infoChip}>
                <Text style={styles.infoLabel}>المسافة للكعبة</Text>
                <Text style={styles.infoValue}>{dist} كم</Text>
              </View>
            </View>

            <View style={styles.compassArea}>
              {/* Compass rose (cardinal directions, rotates with phone heading) */}
              <Animated.View style={[styles.compassRose, compassRoseStyle]}>
                <Text style={[styles.cardinal, styles.cardinalN]}>ش</Text>
                <Text style={[styles.cardinal, styles.cardinalE]}>ق</Text>
                <Text style={[styles.cardinal, styles.cardinalS]}>ج</Text>
                <Text style={[styles.cardinal, styles.cardinalW]}>غ</Text>
                <View style={styles.tickRing} />
              </Animated.View>

              {/* Qibla arrow */}
              <Animated.View style={[styles.arrow, rotateStyle]}>
                <View style={[styles.arrowHead, { borderBottomColor: isAligned ? "#22C55E" : COLORS.gold }]} />
                <View style={[styles.arrowBody, { backgroundColor: isAligned ? "#22C55E" : COLORS.gold }]} />
                <View style={styles.arrowCenter}>
                  <Text style={styles.kaabaEmoji}>🕋</Text>
                </View>
              </Animated.View>
            </View>

            <View style={[styles.statusCard, isAligned && { borderColor: "#22C55E" }]}>
              <Ionicons
                name={isAligned ? "checkmark-circle" : "compass"}
                size={28}
                color={isAligned ? "#22C55E" : COLORS.gold}
              />
              <View style={{ flex: 1, marginHorizontal: 10 }}>
                <Text style={[styles.statusTitle, isAligned && { color: "#22C55E" }]}>
                  {isAligned ? "أنت تواجه القبلة الآن" : "وجّه جهازك حتى يصبح السهم أخضر"}
                </Text>
                <Text style={styles.statusSub}>الانحراف: {Math.min(angleDiff, 360 - angleDiff)}°</Text>
              </View>
            </View>

            <Text style={styles.hint}>
              💡 ابتعد عن الأجهزة المعدنية والمغناطيسية، وحرّك جهازك على شكل رقم 8 لمعايرة البوصلة
            </Text>
          </>
        )}
      </SafeAreaView>
    </PatternBackground>
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
  infoRow: { flexDirection: "row", paddingHorizontal: 20, gap: 12, marginTop: 6, marginBottom: 12 },
  infoChip: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.28)",
    borderColor: "rgba(212,175,55,0.35)",
    borderWidth: 1,
    borderRadius: 16,
    paddingVertical: 14,
    alignItems: "center",
  },
  infoLabel: { color: "#CBD5E1", fontSize: 12 },
  infoValue: { color: COLORS.goldLight, fontSize: 22, fontWeight: "800", marginTop: 2 },
  compassArea: { flex: 1, alignItems: "center", justifyContent: "center" },
  compassRose: {
    width: 300,
    height: 300,
    borderRadius: 150,
    borderColor: "rgba(212,175,55,0.35)",
    borderWidth: 2,
    alignItems: "center",
    justifyContent: "center",
    position: "absolute",
  },
  tickRing: {
    position: "absolute",
    width: 270,
    height: 270,
    borderRadius: 135,
    borderColor: "rgba(212,175,55,0.15)",
    borderWidth: 1,
  },
  cardinal: { position: "absolute", color: COLORS.goldLight, fontSize: 22, fontWeight: "800" },
  cardinalN: { top: 8 },
  cardinalE: { left: 8 },
  cardinalS: { bottom: 8 },
  cardinalW: { right: 8 },
  arrow: {
    width: 60,
    height: 240,
    alignItems: "center",
    justifyContent: "flex-start",
    position: "absolute",
  },
  arrowHead: {
    width: 0,
    height: 0,
    borderLeftWidth: 22,
    borderRightWidth: 22,
    borderBottomWidth: 36,
    borderLeftColor: "transparent",
    borderRightColor: "transparent",
    borderBottomColor: COLORS.gold,
  },
  arrowBody: { width: 6, height: 90, backgroundColor: COLORS.gold, marginTop: -2 },
  arrowCenter: {
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: "rgba(0,0,0,0.45)",
    borderColor: COLORS.gold,
    borderWidth: 2,
    alignItems: "center",
    justifyContent: "center",
    marginTop: 12,
  },
  kaabaEmoji: { fontSize: 38 },
  statusCard: {
    flexDirection: "row",
    alignItems: "center",
    marginHorizontal: 20,
    backgroundColor: "rgba(0,0,0,0.32)",
    borderColor: "rgba(212,175,55,0.4)",
    borderWidth: 1.5,
    borderRadius: 16,
    padding: 14,
    marginBottom: 8,
  },
  statusTitle: { color: "#FFFFFF", fontSize: 16, fontWeight: "700" },
  statusSub: { color: "#CBD5E1", fontSize: 12, marginTop: 2 },
  hint: { color: "#CBD5E1", fontSize: 12, textAlign: "center", padding: 12 },
  errorBox: { flex: 1, alignItems: "center", justifyContent: "center", padding: 24 },
  errorTitle: { color: "#FFFFFF", fontSize: 20, fontWeight: "700", marginTop: 14 },
  errorText: { color: "#CBD5E1", fontSize: 14, marginTop: 8, textAlign: "center" },
  openBtn: { backgroundColor: COLORS.gold, paddingVertical: 14, paddingHorizontal: 32, borderRadius: 14, marginTop: 20 },
  openBtnText: { color: "#1a1a1a", fontSize: 16, fontWeight: "800" },
});
