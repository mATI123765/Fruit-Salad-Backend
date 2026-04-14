package com.fruitsalad.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * =============================================================================
 * SOUND MANAGER
 * =============================================================================
 * Singleton class that handles all audio playback in the game.
 * Supports background music, sound effects, and special audio clips.
 * 
 * @author  Fruit Salad Ltd.
 * @version 1.0.0
 * =============================================================================
 */
public class SoundManager {

    /* SINGLETON INSTANCE */    
    private static SoundManager instance;
    
    /* AUDIO RESOURCES */    
    private final Map<String, Clip> soundEffects;
    private Clip backgroundMusic;
    private Clip menuMusic;
    private Clip currentMusic;
    
    private float masterVolume = 0.8f;
    private float musicVolume = 0.5f;
    private float sfxVolume = 0.7f;
    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    
    /* SOUND FILE PATHS */
    private static final String SOUND_PATH = "/sounds/";
    public static final String MENU_MUSIC = "menu-background_music.wav";
    public static final String GAME_MUSIC = "background_music.wav";
    public static final String UPGRADE_SOUND = "upgrade-buy_item-effect.wav";
    public static final String CREDIT_SOUND = "-999999_social_credit-effect.wav";
    public static final String EASTER_EGG = "easter egg-willyrex_paradise.wav";
    
    /* CONSTRUCTOR */
    /**
     * Private constructor for singleton pattern.
     */
    private SoundManager() {
        this.soundEffects = new HashMap<>();
        loadAllSounds();
    }
    
    /**
     * Gets the singleton instance of SoundManager.
     * 
     * @return The SoundManager instance
     */
    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /* SOUND LOADING */
    /**
     * Loads all game sounds into memory.
     */
    private void loadAllSounds() {
        System.out.println("[SoundManager] Loading audio files...");
        
        // Load sound effects
        loadSoundEffect("upgrade", UPGRADE_SOUND);
        loadSoundEffect("credit", CREDIT_SOUND);
        loadSoundEffect("easter_egg", EASTER_EGG);
        
        // Load music (separate clips for looping)
        menuMusic = loadClip(MENU_MUSIC);
        backgroundMusic = loadClip(GAME_MUSIC);
        System.out.println("[SoundManager] Audio loading complete.");
    }
    
    /**
     * Loads a sound effect into the cache.
     * 
     * @param name The identifier for the sound
     * @param filename The file name in the sounds folder
     */
    private void loadSoundEffect(String name, String filename) {
        Clip clip = loadClip(filename);
        if (clip != null) {
            soundEffects.put(name, clip);
            System.out.println("[SoundManager] Loaded: " + name);
        }
    }
    
    /**
     * Loads an audio clip from resources.
     * 
     * @param filename The file name to load
     * @return The loaded Clip, or null if loading failed
     */
    private Clip loadClip(String filename) {
        try {
            String path = SOUND_PATH + filename;
            InputStream audioSrc = getClass().getResourceAsStream(path);
            
            if (audioSrc == null) {
                System.err.println("[SoundManager] File not found: " + path);
                return null;
            }
            
            // Buffered stream for mark/reset support
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("[SoundManager] Error loading " + filename + ": " + e.getMessage());
            return null;
        }
    }
    
    /* MUSIC PLAYBACK */
    /**
     * Plays the menu background music.
     */
    public void playMenuMusic() {
        if (!musicEnabled || menuMusic == null) return;
        
        stopCurrentMusic();
        currentMusic = menuMusic;
        playMusicLoop(menuMusic);
    }
    
    /**
     * Plays the game background music.
     */
    public void playGameMusic() {
        if (!musicEnabled || backgroundMusic == null) return;
        
        stopCurrentMusic();
        currentMusic = backgroundMusic;
        playMusicLoop(backgroundMusic);
    }
    
    /**
     * Plays a music clip on loop.
     * 
     * @param clip The clip to play
     */
    private void playMusicLoop(Clip clip) {
        if (clip == null) return;
        
        clip.setFramePosition(0);
        setClipVolume(clip, musicVolume * masterVolume);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    
    /**
     * Stops the currently playing music.
     */
    public void stopCurrentMusic() {
        if (currentMusic != null && currentMusic.isRunning()) { currentMusic.stop(); }
    }
    
    /**
     * Stops all music playback.
     */
    public void stopAllMusic() {
        stopCurrentMusic();
        if (menuMusic != null) menuMusic.stop();
        if (backgroundMusic != null) backgroundMusic.stop();
    }
    
    /* SOUND EFFECTS PLAYBACK */    
    /**
     * Plays the upgrade purchase sound.
     */
    public void playUpgradeSound() { playSoundEffect("upgrade"); }
    
    /**
     * Plays the social credit sound effect.
     */
    public void playCreditSound() { playSoundEffect("credit"); }

    /**
     * Plays the Easter egg audio (Willyrex Paradise).
     */
    public void playEasterEgg() { playSoundEffect("easter_egg"); }
    
    /**
     * Plays a cached sound effect by name.
     * 
     * @param name The identifier of the sound to play
     */
    public void playSoundEffect(String name) {
        if (!soundEnabled) return;
        
        Clip clip = soundEffects.get(name);
        if (clip != null) {
            // Reset to beginning if already playing
            if (clip.isRunning()) { clip.stop(); }
            clip.setFramePosition(0);
            setClipVolume(clip, sfxVolume * masterVolume);
            clip.start();
        }
    }
    
    /* VOLUME CONTROL */    
    /**
     * Sets the volume of a clip.
     * 
     * @param clip The clip to adjust
     * @param volume Volume level (0.0 to 1.0)
     */
    private void setClipVolume(Clip clip, float volume) {
        if (clip == null) return;
        
        try {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            
            // Convert linear volume to decibels
            float dB = (float) (Math.log10(Math.max(0.0001, volume)) * 20.0);
            dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
            
            gainControl.setValue(dB);
        } catch (IllegalArgumentException e) {
            // Volume control not supported
        }
    }
    
    /**
     * Sets the master volume.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0, Math.min(1, volume));
        updateAllVolumes();
    }
    
    /**
     * Sets the music volume.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0, Math.min(1, volume));
        if (currentMusic != null) {
            setClipVolume(currentMusic, musicVolume * masterVolume);
        }
    }
    
    /**
     * Sets the sound effects volume.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setSfxVolume(float volume) { this.sfxVolume = Math.max(0, Math.min(1, volume)); }
    
    /**
     * Updates volumes on all currently playing clips.
     */
    private void updateAllVolumes() {
        if (currentMusic != null) {
            setClipVolume(currentMusic, musicVolume * masterVolume);
        }
    }
    
    /* ENABLE/DISABLE */    
    /**
     * Enables or disables sound effects.
     * 
     * @param enabled True to enable, false to disable
     */
    public void setSoundEnabled(boolean enabled) { this.soundEnabled = enabled; }
    
    /**
     * Enables or disables music.
     * 
     * @param enabled True to enable, false to disable
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            stopAllMusic();
        }
    }
    
    /**
     * Toggles music on/off.
     * 
     * @return The new music enabled state
     */
    public boolean toggleMusic() {
        musicEnabled = !musicEnabled;
        if (!musicEnabled) {
            stopAllMusic();
        } else if (currentMusic != null) {
            playMusicLoop(currentMusic);
        }
        return musicEnabled;
    }
    
    /**
     * Toggles sound effects on/off.
     * 
     * @return The new sound enabled state
     */
    public boolean toggleSound() {
        soundEnabled = !soundEnabled;
        return soundEnabled;
    }
    
    /* GETTERS */
    public boolean isSoundEnabled() { return soundEnabled; }
    public boolean isMusicEnabled() { return musicEnabled; }
    public float getMasterVolume() { return masterVolume; }
    public float getMusicVolume() { return musicVolume; }
    public float getSfxVolume() { return sfxVolume; }
    
    /* CLEANUP */
    /**
     * Releases all audio resources.
     * Call this when the game exits.
     */
    public void dispose() {
        stopAllMusic();
        
        for (Clip clip : soundEffects.values()) {
            if (clip != null) {
                clip.close();
            }
        }
        soundEffects.clear();
        
        if (menuMusic != null) menuMusic.close();
        if (backgroundMusic != null) backgroundMusic.close();
        System.out.println("[SoundManager] Audio resources released.");
    }
}