package com.graphics.flappy;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.AL10;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

public class SoundEngine {

    private long device;
    private long context;

    private int srcJump1;
    private int srcJump2;
    private int srcJump3;
    private int srcScore;
    private int srcDie1;
    private int srcDie2;
    private int srcDie3;
    private int srcSelect;
    private int srcConfirm;
    private int srcLevelUp;
    private int srcWin;

    public void init() {
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);
        ALCapabilities caps = AL.createCapabilities(deviceCaps);

        // Salto P1: acento agudo tipo "boing" con chirp ascendente
        srcJump1 = createSource(generateChirp(300, 700, 0.12f));
        // Salto P2: tono un poco más alto (personalidad distinta)
        srcJump2 = createSource(generateChirp(400, 900, 0.10f));
        // Salto P3: tono grave y profundo (personalidad terrenal)
        srcJump3 = createSource(generateChirp(180, 420, 0.14f));
        // Score: fanfare de 3 pulsos rápidos ascendentes tipo moneda de Mario
        srcScore = createSource(generateCoinSound());
        // Muerte P1: glissando descendente oscuro
        srcDie1  = createSource(generateDeath(580, 60, 0.45f, 0.9f));
        srcDie2  = createSource(generateDeath(480, 50, 0.50f, 0.85f));
        // Muerte P3: glissando más grave y lento
        srcDie3  = createSource(generateDeath(380, 40, 0.55f, 0.8f));
        srcSelect  = createSource(generateClick(320, 0.06f));
        srcConfirm = createSource(generateConfirmChord());
        srcLevelUp = createSource(generateLevelUpFanfare());
        srcWin     = createSource(generateWinSound());
    }

    public void playJump(int birdIndex) {
        if (birdIndex == 0)      play(srcJump1);
        else if (birdIndex == 1) play(srcJump2);
        else                     play(srcJump3);
    }

    public void playScore()   { play(srcScore); }
    public void playLevelUp() { play(srcLevelUp); }
    public void playWin()     { play(srcWin); }

    public void playDie(int birdIndex) {
        if (birdIndex == 0)      play(srcDie1);
        else if (birdIndex == 1) play(srcDie2);
        else                     play(srcDie3);
    }

    public void playSelect()  { play(srcSelect); }
    public void playConfirm() { play(srcConfirm); }

    // Mantener compatibilidad si se llama sin índice
    public void playJump()    { play(srcJump1); }
    public void playDie()     { play(srcDie1); }

    private void play(int source) {
        AL10.alSourceStop(source);
        AL10.alSourcePlay(source);
    }

    /**
     * Fanfare de subida de nivel: arpegio ascendente C5→E5→G5→C6 retro.
     */
    private int generateLevelUpFanfare() {
        int sampleRate = 44100;
        float[] noteFreqs = {523f, 659f, 784f, 1047f}; // C5, E5, G5, C6
        float noteDur = 0.09f;
        int noteSamples = (int)(sampleRate * noteDur);
        int total = noteSamples * noteFreqs.length;
        ByteBuffer buf = BufferUtils.createByteBuffer(total * 2);

        for (int n = 0; n < noteFreqs.length; n++) {
            float freq = noteFreqs[n];
            int offset = n * noteSamples;
            for (int i = 0; i < noteSamples; i++) {
                double progress = (double) i / noteSamples;
                // Ataque rapido, decay suave
                double env = progress < 0.1 ? (progress / 0.1) : Math.exp(-(progress - 0.1) * 5);
                // Mezcla de onda cuadrada + seno (sonido retro brillante)
                double sq = Math.sin(2 * Math.PI * freq * (offset + i) / sampleRate) >= 0 ? 1.0 : -1.0;
                double si = Math.sin(2 * Math.PI * freq * (offset + i) / sampleRate);
                // Armónico de octava para brillantez
                double si2 = Math.sin(2 * Math.PI * freq * 2 * (offset + i) / sampleRate) * 0.3;
                double val = (sq * 0.35 + si * 0.50 + si2) * env;
                short sample = (short)(val * Short.MAX_VALUE * 0.65);
                buf.put((byte)(sample & 0xFF));
                buf.put((byte)((sample >> 8) & 0xFF));
            }
        }
        buf.flip();
        return makeBuffer(buf, sampleRate);
    }

    /**
     * Fanfarria de Victoria epica: C5 -> E5 -> G5 -> C6 -> E6 -> G6 (Arpegio largo)
     */
    private int generateWinSound() {
        int sampleRate = 44100;
        float[] noteFreqs = {523f, 659f, 784f, 1047f, 1318f, 1568f}; // Do, Mi, Sol, Do, Mi, Sol
        float noteDur = 0.12f;
        int noteSamples = (int)(sampleRate * noteDur);
        int total = noteSamples * noteFreqs.length;
        ByteBuffer buf = BufferUtils.createByteBuffer(total * 2);

        for (int n = 0; n < noteFreqs.length; n++) {
            float freq = noteFreqs[n];
            int offset = n * noteSamples;
            for (int i = 0; i < noteSamples; i++) {
                double progress = (double) i / noteSamples;
                double env = Math.exp(-progress * 2); 
                double val = Math.sin(2 * Math.PI * freq * (offset + i) / sampleRate) * 0.5;
                // Añadir un poco de onda cuadrada para que suene retro
                val += (Math.sin(2 * Math.PI * freq * (offset + i) / sampleRate) >= 0 ? 0.2 : -0.2);
                val *= env;
                short sample = (short)(val * Short.MAX_VALUE * 0.6);
                buf.put((byte)(sample & 0xFF));
                buf.put((byte)((sample >> 8) & 0xFF));
            }
        }
        buf.flip();
        return makeBuffer(buf, sampleRate);
    }

    /**
     * Chirp ascendente (boing de salto): frecuencia que sube linealmente.
     */
    private int generateChirp(float freqStart, float freqEnd, float duration) {
        int sampleRate = 44100;
        int samples = (int)(sampleRate * duration);
        ByteBuffer buf = BufferUtils.createByteBuffer(samples * 2);

        for (int i = 0; i < samples; i++) {
            double progress = (double) i / samples;
            double freq = freqStart + (freqEnd - freqStart) * progress;
            double env = Math.exp(-progress * 4.5); // Ataque rápido, decay suave

            // Mezcla onda cuadrada + seno para un tono retro con cuerpo
            double square = Math.sin(2 * Math.PI * freq * i / sampleRate) >= 0 ? 1.0 : -1.0;
            double sine   = Math.sin(2 * Math.PI * freq * i / sampleRate);
            double val    = (square * 0.4 + sine * 0.6) * env;

            short sample = (short)(val * Short.MAX_VALUE * 0.65);
            buf.put((byte)(sample & 0xFF));
            buf.put((byte)((sample >> 8) & 0xFF));
        }
        buf.flip();
        return makeBuffer(buf, sampleRate);
    }

    /**
     * Sonido tipo "moneda" — dos notas cortas ascendentes (E5 → G5).
     */
    private int generateCoinSound() {
        int sampleRate = 44100;
        float[] freqs  = {659f, 784f};   // E5, G5
        float[] durations = {0.06f, 0.10f};
        int total = 0;
        for (float d : durations) total += (int)(sampleRate * d);

        ByteBuffer buf = BufferUtils.createByteBuffer(total * 2);
        int offset = 0;
        for (int n = 0; n < freqs.length; n++) {
            int segSamples = (int)(sampleRate * durations[n]);
            for (int i = 0; i < segSamples; i++) {
                double progress = (double) i / segSamples;
                double env = (1.0 - progress * progress); // Decay cuadrático
                double square = Math.sin(2 * Math.PI * freqs[n] * (offset + i) / sampleRate) >= 0 ? 1.0 : -1.0;
                double sine   = Math.sin(2 * Math.PI * freqs[n] * (offset + i) / sampleRate);
                double val = (square * 0.5 + sine * 0.5) * env;
                short sample = (short)(val * Short.MAX_VALUE * 0.7);
                buf.put((byte)(sample & 0xFF));
                buf.put((byte)((sample >> 8) & 0xFF));
            }
            offset += segSamples;
        }
        buf.flip();
        return makeBuffer(buf, sampleRate);
    }

    /**
     * Muerte: glissando descendente con vibrato al final para dramatismo.
     */
    private int generateDeath(float freqStart, float freqEnd, float duration, float volume) {
        int sampleRate = 44100;
        int samples = (int)(sampleRate * duration);
        ByteBuffer buf = BufferUtils.createByteBuffer(samples * 2);

        for (int i = 0; i < samples; i++) {
            double progress = (double) i / samples;
            // Frecuencia que baja con una curva cuadrática (más dramático)
            double freq = freqStart * Math.pow((double)freqEnd / freqStart, progress * progress);
            // Vibrato ligero que aumenta al final (agonía)
            double vibrato = 1.0 + Math.sin(2 * Math.PI * 6 * progress * progress * 3) * 0.015;
            freq *= vibrato;

            double env = (1.0 - progress * 0.8); // Fade out largo
            double square = Math.sin(2 * Math.PI * freq * i / sampleRate) >= 0 ? 1.0 : -1.0;
            double sine   = Math.sin(2 * Math.PI * freq * i / sampleRate);
            double val = (square * 0.6 + sine * 0.4) * env;

            short sample = (short)(val * Short.MAX_VALUE * volume);
            buf.put((byte)(sample & 0xFF));
            buf.put((byte)((sample >> 8) & 0xFF));
        }
        buf.flip();
        return makeBuffer(buf, sampleRate);
    }

    /**
     * Click retro de menú: pulso muy breve de ruido filtrado.
     */
    private int generateClick(float freq, float duration) {
        int sampleRate = 44100;
        int samples = (int)(sampleRate * duration);
        ByteBuffer buf = BufferUtils.createByteBuffer(samples * 2);

        for (int i = 0; i < samples; i++) {
            double progress = (double) i / samples;
            double env = Math.exp(-progress * 20); // Decay muy rápido
            double val = Math.sin(2 * Math.PI * freq * i / sampleRate) * env;
            short sample = (short)(val * Short.MAX_VALUE * 0.5);
            buf.put((byte)(sample & 0xFF));
            buf.put((byte)((sample >> 8) & 0xFF));
        }
        buf.flip();
        return makeBuffer(buf, sampleRate);
    }

    /**
     * Acorde de confirmación: raíz + quinta (C5 + G5) tocados juntos, con ataque
     */
    private int generateConfirmChord() {
        int sampleRate = 44100;
        int samples = (int)(sampleRate * 0.18f);
        ByteBuffer buf = BufferUtils.createByteBuffer(samples * 2);
        float[] harmonics = {523f, 659f, 784f}; // C5, E5, G5 — acorde de Do mayor

        for (int i = 0; i < samples; i++) {
            double progress = (double) i / samples;
            double env = progress < 0.05 ? (progress / 0.05) : Math.exp(-(progress - 0.05) * 6);
            double val = 0;
            for (float f : harmonics) {
                val += Math.sin(2 * Math.PI * f * i / sampleRate);
            }
            val = (val / harmonics.length) * env;
            short sample = (short)(val * Short.MAX_VALUE * 0.7);
            buf.put((byte)(sample & 0xFF));
            buf.put((byte)((sample >> 8) & 0xFF));
        }
        buf.flip();
        return makeBuffer(buf, sampleRate);
    }

    private int makeBuffer(ByteBuffer data, int sampleRate) {
        int alBuf = AL10.alGenBuffers();
        AL10.alBufferData(alBuf, AL10.AL_FORMAT_MONO16, data, sampleRate);
        return alBuf;
    }

    private int createSource(int alBuffer) {
        int source = AL10.alGenSources();
        AL10.alSourcei(source, AL10.AL_BUFFER, alBuffer);
        AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
        return source;
    }

    public void cleanup() {
        AL10.alDeleteSources(srcJump1);
        AL10.alDeleteSources(srcJump2);
        AL10.alDeleteSources(srcJump3);
        AL10.alDeleteSources(srcScore);
        AL10.alDeleteSources(srcDie1);
        AL10.alDeleteSources(srcDie2);
        AL10.alDeleteSources(srcDie3);
        AL10.alDeleteSources(srcSelect);
        AL10.alDeleteSources(srcConfirm);
        AL10.alDeleteSources(srcLevelUp);
        AL10.alDeleteSources(srcWin);
        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
    }
}
