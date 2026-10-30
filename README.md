# RecordMC

**Minecraft Fabric** — by AtsukiMC

---

## English

### What is RecordMC?

**RecordMC** is a client-side screen recording mod for Minecraft Java Edition (Fabric).

It allows you to record gameplay videos directly from inside Minecraft with a single keypress without requiring heavy external recording software like OBS. Featuring internal audio recording and asynchronous OpenGL PBO frame capture, it delivers clean, high-performance recording. Once recording stops, the **video file is automatically copied to your OS clipboard**, ready for instant pasting (`Ctrl + V`) into Discord, Slack, social media, or file explorers.

### Supported versions

| Minecraft | JAR | Fabric Loader | Java |
| --- | --- | --- | --- |
| 1.21.11 | `recordmc-1.0.0+1.21.11.jar` | 0.15.11 or newer | 21 or newer |
| 1.21.10 | `recordmc-1.0.0+1.21.10.jar` | 0.15.11 or newer | 21 or newer |
| 1.21.9 | `recordmc-1.0.0+1.21.9.jar` | 0.15.11 or newer | 21 or newer |
| 1.21.8 | `recordmc-1.0.0+1.21.8.jar` | 0.15.11 or newer | 21 or newer |
| 1.21.7 | `recordmc-1.0.0+1.21.7.jar` | 0.15.11 or newer | 21 or newer |
| 1.21.6 | `recordmc-1.0.0+1.21.6.jar` | 0.15.11 or newer | 21 or newer |
| 1.21.5 | `recordmc-1.0.0+1.21.5.jar` | 0.15.11 or newer | 21 or newer |
| 1.21.4 | `recordmc-1.0.0+1.21.4.jar` | 0.15.11 or newer | 21 or newer |
| 1.21.1 | `recordmc-1.0.0+1.21.1.jar` | 0.15.11 or newer | 21 or newer |
| 1.21 | `recordmc-1.0.0+1.21.jar` | 0.15.11 or newer | 21 or newer |
| 1.20.6 | `recordmc-1.0.0+1.20.6.jar` | 0.15.11 or newer | 21 or newer |
| 1.20.1 | `recordmc-1.0.0+1.20.1.jar` | 0.14.22 or newer | 17 or newer |
| 1.20 | `recordmc-1.0.0+1.20.jar` | 0.14.22 or newer | 17 or newer |
| 1.19.4 | `recordmc-1.0.0+1.19.4.jar` | 0.14.22 or newer | 17 or newer |
| 1.18.2 | `recordmc-1.0.0+1.18.2.jar` | 0.14.22 or newer | 17 or newer |
| 1.18.1 | `recordmc-1.0.0+1.18.1.jar` | 0.14.22 or newer | 17 or newer |
| 1.18 | `recordmc-1.0.0+1.18.jar` | 0.14.22 or newer | 17 or newer |
| 1.16.1 | `recordmc-1.0.0+1.16.1.jar` | 0.14.22 or newer | 8 or newer |

### Installation

- Put the JAR and required dependency mods (`fabric-api`, `malilib`) in your client's `mods` folder.
- RecordMC is a **client-side only** mod. Dedicated servers do **not** need it.

### How to use it

- **Start / Stop Recording**: Press `F9` (default) to start or stop recording.
- **Open Config GUI**: Press `H + R` (default) or access via ModMenu to customize settings, keybindings, FPS, quality, and output formats.

### What it does

- **Single-Key In-Game Recording**: Start and stop high-performance screen recording with a single keypress.
- **Full Screen & GUI Capture**: Records everything visible on screen, including HUD, chat, inventory, chests, and open menus.
- **Clean Game Audio**: Records internal Minecraft game audio (BGM, sound effects, ambient sounds) clearly.
- **Multiple Output Formats**: Select your preferred output container (MP4, MOV, MKV, WebM) directly from the config GUI (Default: `mp4`).
- **In-Game REC Indicator**: Displays a blinking red indicator (`● REC`) and elapsed time counter while recording.
- **Automatic Clipboard Copy**: Finished video files are automatically copied to your OS clipboard upon completion for instant pasting (`Ctrl + V`).
- **Automatic Encoder Fallback**: Dynamically detects available encoders (`libx264`, `h264_nvenc`, `h264_vaapi`, `mpeg4`) to ensure recording works out-of-the-box even in sandboxed environments like Flatpak.
- **Multi-Language Support**: Fully localized in 14 native languages with automatic related-language fallback.

### Good to know

- **FFmpeg Requirement**: RecordMC uses FFmpeg under the hood. Make sure `ffmpeg` is installed and accessible in your system PATH, or specify the path to its executable in the config GUI.
- **Performance**: Uses OpenGL Pixel Buffer Objects (PBO) for asynchronous frame capture, minimizing impact on game FPS during recording.
- **Video Storage**: Videos are saved to `<gameDir>/video/` by default. You can change this to any custom folder in the configuration.

### License

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**.

---

## 日本語

### RecordMCとは？

**RecordMC** は、Minecraftのゲーム内からワンキーで手軽に画面録画を行えるクライアントサイドModです。

外部の録画ソフト（OBSなど）を立ち上げることなく、Minecraft内部の純粋なゲーム音とともにプレイ画面全体を高画質かつ軽量にキャプチャ・保存できます。録画完了後は**動画ファイルが自動でOSのクリップボードにコピーされる**ため、DiscordやSNS等に `Ctrl + V` で即座に共有できます。

### 対応バージョン

| Minecraft | JAR | Fabric Loader | Java |
| --- | --- | --- | --- |
| 1.21.11 | `recordmc-1.0.0+1.21.11.jar` | 0.15.11以上 | 21以上 |
| 1.21.10 | `recordmc-1.0.0+1.21.10.jar` | 0.15.11以上 | 21以上 |
| 1.21.9 | `recordmc-1.0.0+1.21.9.jar` | 0.15.11以上 | 21以上 |
| 1.21.8 | `recordmc-1.0.0+1.21.8.jar` | 0.15.11以上 | 21以上 |
| 1.21.7 | `recordmc-1.0.0+1.21.7.jar` | 0.15.11以上 | 21以上 |
| 1.21.6 | `recordmc-1.0.0+1.21.6.jar` | 0.15.11以上 | 21以上 |
| 1.21.5 | `recordmc-1.0.0+1.21.5.jar` | 0.15.11以上 | 21以上 |
| 1.21.4 | `recordmc-1.0.0+1.21.4.jar` | 0.15.11以上 | 21以上 |
| 1.21.1 | `recordmc-1.0.0+1.21.1.jar` | 0.15.11以上 | 21以上 |
| 1.21 | `recordmc-1.0.0+1.21.jar` | 0.15.11以上 | 21以上 |
| 1.20.6 | `recordmc-1.0.0+1.20.6.jar` | 0.15.11以上 | 21以上 |
| 1.20.1 | `recordmc-1.0.0+1.20.1.jar` | 0.14.22以上 | 17以上 |
| 1.20 | `recordmc-1.0.0+1.20.jar` | 0.14.22以上 | 17以上 |
| 1.19.4 | `recordmc-1.0.0+1.19.4.jar` | 0.14.22以上 | 17以上 |
| 1.18.2 | `recordmc-1.0.0+1.18.2.jar` | 0.14.22以上 | 17以上 |
| 1.18.1 | `recordmc-1.0.0+1.18.1.jar` | 0.14.22以上 | 17以上 |
| 1.18 | `recordmc-1.0.0+1.18.jar` | 0.14.22以上 | 17以上 |
| 1.16.1 | `recordmc-1.0.0+1.16.1.jar` | 0.14.22以上 | 8以上 |

### 導入方法

- クライアントの `mods` フォルダに対応するJARおよび前提Mod（`fabric-api`, `malilib`）を入れてください。
- 本Modは**クライアント専用**です。サーバー側への導入は不要です。

### 使い方

- **録画の開始 / 停止**: `F9`（デフォルト）を押すだけで録画を即座に開始・終了できます。
- **設定メニューを開く**: `H + R`（デフォルト）または ModMenu から開き、FPS・画質・エンコーダー・出力形式・ホットキーなどを直感的にカスタマイズできます。

### できること

- **ワンキー録画**: `F9` を押すだけで素早く録画を開始・停止。
- **完全な画面キャプチャ**: HUD、チャット、開いたインベントリやGUIなど、画面に映っているすべてをそのまま記録。
- **ゲーム内音声のみを収録**: マイク音声や外部ソフトの音を拾わず、Minecraft内部の純粋なゲーム音のみをクリアに収録。
- **多彩な出力形式**: MP4、MOV、MKV、WebM（デフォルト: `mp4`）に対応し、ボタン1つで切り替え可能。
- **録画中インジケーター**: 画面右上に点滅する赤いマーク（`● REC`）と経過時間を表示。
- **クリップボード自動コピー**: 録画終了後、生成された動画ファイルが自動でOSのクリップボードにコピーされ、Discord等に `Ctrl + V` で即貼り付け可能。
- **エンコーダー自動フォールバック**: `libx264`, `h264_nvenc`, `h264_vaapi`, `mpeg4` を自動判別し、Flatpak環境などでも確実に動作。
- **多言語対応**: 日本語を含む14言語のネイティブ翻訳に対応。

### ご注意

- **FFmpegの必要性**: 録画処理にFFmpegを使用します。システムにFFmpegがインストールされている（PATHが通っている）か、設定画面でFFmpegの実行ファイルパスを指定してください。
- **動作パフォーマンス**: OpenGL PBO（Pixel Buffer Object）による非同期キャプチャを採用しており、ゲームプレイへの負荷を最小限に抑えています。
- **保存先**: 動画はデフォルトで `<ゲームディレクトリ>/video/` に保存されます（設定で変更可能）。

### ライセンス

本プロジェクトは **GNU Lesser General Public License v3.0 (LGPL-3.0)** の下で公開されています。
