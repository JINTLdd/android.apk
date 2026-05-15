import { Audio, InterruptionModeAndroid, InterruptionModeIOS } from "expo-av";
import { getLocalAudioPath } from "./audioDownload";

let currentSound: Audio.Sound | null = null;

export async function configureAudioMode() {
  await Audio.setAudioModeAsync({
    allowsRecordingIOS: false,
    staysActiveInBackground: true,
    playsInSilentModeIOS: true,
    interruptionModeIOS: InterruptionModeIOS.DoNotMix,
    interruptionModeAndroid: InterruptionModeAndroid.DoNotMix,
    shouldDuckAndroid: true,
    playThroughEarpieceAndroid: false,
  });
}

export async function stopCurrent() {
  if (currentSound) {
    try {
      await currentSound.stopAsync();
      await currentSound.unloadAsync();
    } catch {
      // ignore
    }
    currentSound = null;
  }
}

export interface PlayCallbacks {
  onStatus?: (s: { positionMs: number; durationMs: number; isPlaying: boolean }) => void;
  onFinish?: () => void;
}

export async function playAudio(url: string, callbacks?: PlayCallbacks): Promise<Audio.Sound | null> {
  if (!url) return null;
  await stopCurrent();

  // Prefer local downloaded file
  const local = await getLocalAudioPath(url);
  const source = local ? { uri: local } : { uri: url };

  try {
    const { sound } = await Audio.Sound.createAsync(source, { shouldPlay: true });
    currentSound = sound;

    sound.setOnPlaybackStatusUpdate((status: any) => {
      if (!status.isLoaded) return;
      callbacks?.onStatus?.({
        positionMs: status.positionMillis || 0,
        durationMs: status.durationMillis || 0,
        isPlaying: !!status.isPlaying,
      });
      if (status.didJustFinish) {
        callbacks?.onFinish?.();
      }
    });

    return sound;
  } catch (e) {
    console.warn("playAudio error", e);
    return null;
  }
}

export async function pauseCurrent() {
  if (currentSound) {
    try {
      await currentSound.pauseAsync();
    } catch {
      // ignore
    }
  }
}

export async function resumeCurrent() {
  if (currentSound) {
    try {
      await currentSound.playAsync();
    } catch {
      // ignore
    }
  }
}

export function getCurrentSound() {
  return currentSound;
}
