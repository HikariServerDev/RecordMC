package com.atsukimc.recordmc.recorder;

/**
 * デコード済みPCMオーディオサンプルを保持するデータクラス
 */
public class PcmSoundData {
    public final int sampleRate;
    public final int channels;
    public final short[] samples; // stereoの場合は [L0, R0, L1, R1, ...]、monoの場合は [M0, M1, ...]
    public final int totalFrames;

    public PcmSoundData(int sampleRate, int channels, short[] samples) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.samples = samples;
        this.totalFrames = samples.length / Math.max(1, channels);
    }

    /**
     * 指定したフレーム位置のサンプル（-1.0f 〜 1.0f）を線形補間で取得する
     *
     * @param frameIndex フレーム位置（浮動小数点数）
     * @param channel    チャンネル（0: Left / Mono, 1: Right）
     * @return 正規化されたサンプル値 (-1.0f 〜 1.0f)
     */
    public float getSample(double frameIndex, int channel) {
        int index0 = (int) Math.floor(frameIndex);
        if (index0 < 0 || index0 >= totalFrames) {
            return 0.0f;
        }

        int index1 = index0 + 1;
        float frac = (float) (frameIndex - index0);

        float s0 = getRawSample(index0, channel);
        float s1 = (index1 < totalFrames) ? getRawSample(index1, channel) : 0.0f;

        return (s0 + (s1 - s0) * frac) / 32768.0f;
    }

    private float getRawSample(int frameIndex, int channel) {
        if (channels == 1) {
            return samples[frameIndex];
        } else {
            int ch = Math.min(channel, channels - 1);
            return samples[frameIndex * channels + ch];
        }
    }
}
