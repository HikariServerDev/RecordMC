package com.atsukimc.recordmc.recorder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.OggAudioStream;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minecraftのゲーム内音声のみをキャプチャ・合成するソフトウェアオーディオミキサー
 */
public class GameAudioMixer implements SoundInstanceListener {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/Mixer");
    private static final GameAudioMixer INSTANCE = new GameAudioMixer();

    public static final int SAMPLE_RATE = 44100;
    public static final int CHANNELS = 2; // Stereo

    // PCMデータのメモリキャッシュ
    private final Map<Identifier, PcmSoundData> pcmCache = new ConcurrentHashMap<>();
    // 現在再生中のアクティブボイス一覧
    private final List<ActiveVoice> activeVoices = new CopyOnWriteArrayList<>();

    private volatile boolean mixing = false;
    private ExecutorService executor;

    public static GameAudioMixer getInstance() {
        return INSTANCE;
    }

    public void start() {
        activeVoices.clear();
        mixing = true;
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newFixedThreadPool(2);
        }
        LOGGER.info("Game audio mixer started");
    }

    public void stop() {
        mixing = false;
        activeVoices.clear();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        LOGGER.info("Game audio mixer stopped");
    }

    @Override
    public void onSoundPlayed(SoundInstance soundInstance, WeightedSoundSet soundSet, float range) {
        if (!mixing || soundInstance == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        Sound sound = soundInstance.getSound();
        if (sound == null || sound == net.minecraft.client.sound.SoundManager.MISSING_SOUND) {
            return;
        }

        Identifier location = sound.getLocation();

        // 非同期でPCMをロード・キャッシュしてボイスに追加
        loadPcmAsync(location, pcm -> {
            if (pcm == null || !mixing) {
                return;
            }

            float volume = soundInstance.getVolume();
            float pitch = soundInstance.getPitch();
            Vec3d pos = new Vec3d(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ());
            boolean relative = soundInstance.isRelative() || soundInstance.getAttenuationType() == SoundInstance.AttenuationType.NONE;
            boolean looping = soundInstance.isRepeatable();
            SoundCategory category = soundInstance.getCategory();

            ActiveVoice voice = new ActiveVoice(pcm, soundInstance, volume, pitch, pos, relative, looping, category);
            activeVoices.add(voice);
        });
    }

    /**
     * OggファイルからPCMデータを非同期に読み込む
     */
    private void loadPcmAsync(Identifier location, java.util.function.Consumer<PcmSoundData> callback) {
        PcmSoundData cached = pcmCache.get(location);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            callback.accept(null);
            return;
        }

        ResourceManager resourceManager = client.getResourceManager();
        if (resourceManager == null) {
            callback.accept(null);
            return;
        }

        executor.submit(() -> {
            try {
                java.util.Optional<Resource> resOpt = resourceManager.getResource(location);
                if (resOpt.isEmpty()) {
                    callback.accept(null);
                    return;
                }

                Resource resource = resOpt.get();
                try (InputStream is = resource.getInputStream();
                     OggAudioStream stream = new OggAudioStream(is)) {
                    AudioFormat format = stream.getFormat();
                    it.unimi.dsi.fastutil.floats.FloatArrayList list = new it.unimi.dsi.fastutil.floats.FloatArrayList();
                    while (stream.read(list::add)) {}

                    float[] samples = list.toFloatArray();
                    short[] shortSamples = new short[samples.length];
                    for (int i = 0; i < samples.length; i++) {
                        shortSamples[i] = (short) MathHelper.clamp((int) (samples[i] * 32767.0f), -32768, 32767);
                    }

                    PcmSoundData data = new PcmSoundData((int) format.getSampleRate(), format.getChannels(), shortSamples);
                    pcmCache.put(location, data);
                    callback.accept(data);
                }
            } catch (Exception e) {
                LOGGER.debug("Could not load sound PCM for {}: {}", location, e.getMessage());
                callback.accept(null);
            }
        });
    }

    /**
     * 指定されたサンプル数のステレオPCM（16-bit signed, interleaved）をレンダリングする
     *
     * @param frameCount 生成するフレーム数（1フレーム = Left + Right = 4バイト）
     * @param outBytes   出力先バイト配列 (長さ >= frameCount * 4)
     */
    public void renderSamples(int frameCount, byte[] outBytes) {
        float[] mixL = new float[frameCount];
        float[] mixR = new float[frameCount];

        MinecraftClient client = MinecraftClient.getInstance();
        float masterVolume = 1.0f;
        Vec3d listenerPos = Vec3d.ZERO;
        float listenerYaw = 0.0f;

        if (client != null && client.options != null) {
            masterVolume = client.options.getSoundVolume(SoundCategory.MASTER);
            if (client.gameRenderer != null) {
                Camera camera = client.gameRenderer.getCamera();
                if (camera != null) {
                    listenerPos = camera.getCameraPos();
                    listenerYaw = camera.getYaw();
                }
            }
        }

        List<ActiveVoice> deadVoices = new ArrayList<>();

        for (ActiveVoice voice : activeVoices) {
            if (!voice.soundInstance.canPlay()) {
                deadVoices.add(voice);
                continue;
            }

            float catVolume = 1.0f;
            if (client != null && client.options != null) {
                catVolume = client.options.getSoundVolume(voice.category);
            }
            float baseGain = masterVolume * catVolume * voice.volume;
            if (baseGain <= 0.001f) {
                continue;
            }

            float leftGain;
            float rightGain;

            if (voice.relative) {
                leftGain = baseGain;
                rightGain = baseGain;
            } else {
                // 3D音響の距離減衰とパンニング計算
                double dx = voice.pos.x - listenerPos.x;
                double dy = voice.pos.y - listenerPos.y;
                double dz = voice.pos.z - listenerPos.z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                // 距離減衰（最大64ブロック）
                float atten = (float) Math.max(0.0, 1.0 - (dist / 48.0));
                if (atten <= 0.001f) {
                    continue;
                }

                // 水平パンニング（リスナーの向きに対する相対角度）
                double yawRad = Math.toRadians(listenerYaw);
                // リスナーの向きに合わせた回転
                double relX = dx * Math.cos(yawRad) + dz * Math.sin(yawRad);
                double relZ = -dx * Math.sin(yawRad) + dz * Math.cos(yawRad);

                double angle = Math.atan2(relX, relZ);
                float pan = MathHelper.clamp((float) Math.sin(angle), -1.0f, 1.0f);

                leftGain = baseGain * atten * (float) Math.cos((pan + 1.0) * (Math.PI / 4.0));
                rightGain = baseGain * atten * (float) Math.sin((pan + 1.0) * (Math.PI / 4.0));
            }

            double pitchStep = ((double) voice.pcm.sampleRate / SAMPLE_RATE) * Math.max(0.1, voice.pitch);

            for (int i = 0; i < frameCount; i++) {
                if (voice.currentFrame >= voice.pcm.totalFrames) {
                    if (voice.looping) {
                        voice.currentFrame = 0.0;
                    } else {
                        deadVoices.add(voice);
                        break;
                    }
                }

                float sL = voice.pcm.getSample(voice.currentFrame, 0);
                float sR = voice.pcm.getSample(voice.currentFrame, 1);

                mixL[i] += sL * leftGain;
                mixR[i] += sR * rightGain;

                voice.currentFrame += pitchStep;
            }
        }

        if (!deadVoices.isEmpty()) {
            activeVoices.removeAll(deadVoices);
        }

        // float [-1.0, 1.0] -> 16-bit PCM Little Endian
        for (int i = 0; i < frameCount; i++) {
            int valL = MathHelper.clamp((int) (mixL[i] * 32767.0f), -32768, 32767);
            int valR = MathHelper.clamp((int) (mixR[i] * 32767.0f), -32768, 32767);

            int byteIdx = i * 4;
            // Left
            outBytes[byteIdx] = (byte) (valL & 0xFF);
            outBytes[byteIdx + 1] = (byte) ((valL >> 8) & 0xFF);
            // Right
            outBytes[byteIdx + 2] = (byte) (valR & 0xFF);
            outBytes[byteIdx + 3] = (byte) ((valR >> 8) & 0xFF);
        }
    }

    /**
     * 再生中の個々のサウンドインスタンスを表すクラス
     */
    private static class ActiveVoice {
        final PcmSoundData pcm;
        final SoundInstance soundInstance;
        final float volume;
        final float pitch;
        final Vec3d pos;
        final boolean relative;
        final boolean looping;
        final SoundCategory category;
        double currentFrame = 0.0;

        ActiveVoice(PcmSoundData pcm, SoundInstance soundInstance, float volume, float pitch, Vec3d pos, boolean relative, boolean looping, SoundCategory category) {
            this.pcm = pcm;
            this.soundInstance = soundInstance;
            this.volume = volume;
            this.pitch = pitch;
            this.pos = pos;
            this.relative = relative;
            this.looping = looping;
            this.category = category;
        }
    }
}
