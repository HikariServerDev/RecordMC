# RecordMC

[English](README.md) | [日本語](README-ja.md)

An in-game screen recording mod for Minecraft Java 1.18.2 (Fabric).

---

## Overview
**RecordMC** allows you to record gameplay videos directly from inside Minecraft with a single keypress.  
Featuring a MaLiLib-based configuration GUI, you can easily customize settings and keybindings via `H + R` or ModMenu.

---

## Features

- **Single-Key Recording**: Press `F9` (customizable) to toggle recording on and off instantly.
- **Polished Config GUI**: Open the configuration menu via `H + R` or ModMenu to visually adjust FPS, quality, encoding preset, output format, hotkeys, and more.
- **Full Screen Capture**: Records everything visible on screen, including HUD, chat, inventory, chests, and open GUI menus.
- **Game Audio Recording**: Records purely internal Minecraft game audio.
- **Multiple Output Formats**: Switch output container formats with a single button click. Supports MP4, MOV, MKV, and WebM (Default: `mp4`).
- **In-Game REC Indicator**: Displays a blinking red indicator (● REC) and elapsed time in the top-right corner while recording.
- **Auto-Save & Clipboard Copy**: Saves recordings to `<gameDir>/video/yyyy.MM.dd.HH-mm-ss.<ext>`. The recorded video file is automatically copied to your system clipboard upon completion, allowing you to instantly paste (`Ctrl + V`) and upload it to Discord, social media, or file managers.
- **Multi-Language Support**:
  - Fully localized in 14 native languages: English, Japanese, Simplified Chinese, Traditional Chinese, Korean, Russian, German, French, Spanish (European/Latin American), Brazilian Portuguese, Italian, Polish, and Ukrainian.
  - Automatically detects and falls back to the most closely related language group when an unregistered language is selected.

---

## Requirements

| Component | Requirement |
|---|---|
| **Minecraft** | `1.18.2` |
| **Fabric Loader** | `>= 0.14.0` |
| **Fabric API** | Target version for 1.18.2 |
| **MaLiLib** | `>= 0.12.0` (Required) |
| **ModMenu** | Optional |
| **FFmpeg** | Must be installed on your system (in PATH or specified in config) |

### Installing FFmpeg
Linux (Ubuntu/Debian):
```bash
sudo apt update && sudo apt install -y ffmpeg
```
Arch Linux:
```bash
sudo pacman -S ffmpeg
```
Windows:
Download `ffmpeg.exe` from the official site and add it to your PATH, or set the full path to `ffmpeg.exe` in the mod's config GUI.

---

## Keybindings

- **Open Config GUI**: `H + R` (Default)
- **Start / Stop Recording**: `F9` (Default)

> [!NOTE]
> Hotkeys can be customized at any time inside the config menu (`H + R` -> "Hotkeys" tab) or via ModMenu.

---

## Configuration Options

Settings can be adjusted via the in-game GUI (`H + R`) or edited in `config/recordmc.json`:

### Generic Tab
| Property | Type | Default | Description |
|---|---|---|---|
| `fps` | integer | `60` | Target recording frame rate (1–240 FPS) |
| `crf` | integer | `20` | H.264 quality factor (0–51, lower means higher quality & larger file size, recommended: 18–23) |
| `preset` | choice (button) | `ultrafast` | FFmpeg encoding preset. Click to cycle through options (`ultrafast`, `superfast`, `veryfast`, `faster`, `fast`, `medium`) |
| `videoFormat` | choice (button) | `mp4` | Output container format. Click to cycle between `mp4`, `mov`, `mkv`, and `webm` |
| `recordAudio` | boolean (button) | `true` | Whether to record internal Minecraft game audio (BGM/SE/ambient) |
| `showIndicator` | boolean (button) | `true` | Whether to show the red REC indicator and elapsed time in the top-right corner |
| `copyToClipboard` | boolean (button) | `true` | Whether to automatically copy the finished video file to clipboard for instant pasting |
| `customFfmpegPath` | string (path) | `""` (empty) | Path to FFmpeg executable. Empty for auto-detection. Supports absolute paths, relative paths, or system command names (displays active path on hover) |
| `customVideoDir` | string (path) | `""` (empty) | Destination folder for recordings. Empty for `./video`. Supports absolute paths or paths relative to game directory (displays active path on hover) |

### Hotkeys Tab
| Property | Default | Description |
|---|---|---|
| `openGui` | `H,R` | Open RecordMC config GUI |
| `toggleRecording` | `F9` | Start / Stop recording |

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).

Copyright (c) 2026 AtsukiMC
