# RecordMC

**Minecraft Fabric** — by AtsukiMC

---

## English

### What is RecordMC?

**RecordMC** is a lightweight, client-side screen recording mod for Minecraft Java Edition (Fabric).

It allows you to record crisp, high-framerate gameplay directly from within Minecraft with a single hotkey press, without running heavy external broadcasting tools like OBS. Built with OpenGL Pixel Buffer Objects (PBO) for asynchronous frame capture, it minimizes impact on in-game performance. Internal Minecraft sound is captured cleanly without picking up external desktop audio or microphone noise. Upon stopping, the **completed video file is automatically copied to your OS clipboard**, ready for instant pasting (`Ctrl + V`) into Discord, Slack, web browsers, or file managers.

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

### Requirements & Dependencies

- **Fabric API**: Required for all supported versions.
- **MaLiLib**: Required for in-game configuration GUI and hotkey handling.
- **FFmpeg**: Must be available on your system.
  - **Linux (Ubuntu / Debian):** `sudo apt update && sudo apt install -y ffmpeg`
  - **Linux (Arch):** `sudo pacman -S ffmpeg`
  - **macOS:** `brew install ffmpeg`
  - **Windows:** Download `ffmpeg.exe` from the official website and add it to your system `PATH`, or set its full path in RecordMC's config GUI.

### Installation

- Download the matching version JAR and place it inside your Minecraft client's `mods` directory alongside **Fabric API** and **MaLiLib**.
- RecordMC is a **client-side only** mod. Dedicated servers do **not** need it.

### How to use it

- **Start / Stop Recording**: Press `F9` (default). A notification and sound will confirm the action.
- **Open Config Menu**: Press `H + R` (default) or use ModMenu to open the settings GUI. You can adjust framerate (1–240 FPS), quality factor (CRF), encoding preset, output container, and hotkeys.

### What it does

- **Single-Key Recording**: Toggle recording instantly without leaving fullscreen or opening external windows.
- **Full Screen & GUI Capture**: Records HUD, chat, inventory, chests, and open interfaces exactly as you see them.
- **Clean Game Audio**: Records pure Minecraft sound effects, background music, and ambient noise.
- **Multiple Output Formats**: Choose between MP4, MOV, MKV, and WebM (Default: `mp4`).
- **In-Game REC Indicator**: Shows a blinking red mark (`● REC`) and live elapsed timer in the top-right corner during recording.
- **Automatic Clipboard Copy**: Once recording finishes, the video file is automatically placed into your system clipboard for quick pasting.
- **Automatic Encoder Fallback**: Dynamically probes and selects the best available video encoder (`libx264` -> `h264_nvenc` -> `h264_vaapi` -> `mpeg4`), ensuring reliable recording even in restricted Flatpak or sandbox environments.
- **Multi-Language Support**: Fully localized in 14 native languages with intelligent related-group fallback.

### Good to know

- **FFmpeg Detection**: RecordMC automatically detects FFmpeg from `PATH` or standard installation directories. If your binary is installed elsewhere, enter the path under `Generic` -> `customFfmpegPath`.
- **Output Directory**: Recorded files are saved to `<gameDir>/video/` by default. You can customize the save directory in settings.
- **Encoder Behavior**: If using `mp4`, `mov`, or `mkv`, video streams are multiplexed without re-encoding (`copy`), finishing the final file in milliseconds.

### License

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**.

---

## 日本語

### RecordMCとは？

**RecordMC** は、Minecraftのゲーム内からワンキーで手軽に画面録画を行える軽量なクライアントサイドModです。

外部の録画ソフトウェア（OBS等）を起動することなく、ゲームプレイ画面全体を高画質かつ低負荷でキャプチャ・保存できます。非同期OpenGL PBO（Pixel Buffer Object）キャプチャを採用しているため、録画中のゲームフレームレートの低下を最小限に抑えます。マイクや外部アプリの音を拾わず、Minecraft内部の純粋なゲーム音のみをクリアに収録します。録画完了後は**生成された動画ファイルが自動でOSのクリップボードにコピーされる**ため、DiscordやSNS、Webブラウザへ `Ctrl + V` で即座に共有できます。

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

### 必要環境・前提Mod

- **Fabric API**: 各バージョン対応版が必須です。
- **MaLiLib**: 設定GUIおよびホットキー管理のため必須です。
- **FFmpeg**: システムにインストールされている必要があります。
  - **Linux (Ubuntu / Debian):** `sudo apt update && sudo apt install -y ffmpeg`
  - **Linux (Arch):** `sudo pacman -S ffmpeg`
  - **macOS:** `brew install ffmpeg`
  - **Windows:** 公式サイト等から `ffmpeg.exe` をダウンロードし、システム環境変数 `PATH` に追加するか、本Modの設定画面で実行ファイルのフルパスを指定してください。

### 導入方法

- お使いのMinecraftバージョンに対応するJARをダウンロードし、前提Mod（**Fabric API**, **MaLiLib**）とともにクライアントの `mods` フォルダに配置してください。
- 本Modは**クライアント専用**です。専用サーバーへの導入は不要です。

### 使い方

- **録画の開始 / 停止**: `F9` キー（デフォルト）を押します。チャット通知と効果音で状態が切り替わります。
- **設定メニューを開く**: `H + R` キー（デフォルト）または ModMenu から開きます。フレームレート（1〜240 FPS）、画質（CRF値）、エンコードプリセット、出力コンテナ形式、キーバインド等をカスタマイズできます。

### できること

- **ワンキー録画**: フルスクリーンプレイ中でもゲームを中断することなく、ワンキーで即座に録画を開始・停止。
- **完全な画面キャプチャ**: HUD、チャット、開いているインベントリやコンテナ画面など、視界に入るすべてを忠実に記録。
- **ゲーム内音声のみを収録**: 外部ソフトの通知音や通話音を拾わず、BGM・効果音・環境音のみをクリアに収録。
- **多彩な出力形式**: MP4、MOV、MKV、WebM（デフォルト: `mp4`）に対応し、ボタン操作で切り替え可能。
- **録画中インジケーター**: 録画中は画面右上に点滅する赤いマーク（`● REC`）と経過時間を表示。
- **クリップボード自動コピー**: 録画完了後、生成ファイルが自動でOSクリップボードに登録され、`Ctrl + V` で即座に貼り付け可能。
- **エンコーダー自動フォールバック**: 起動時にエンコーダーのプローブを行い、利用可能な形式（`libx264` -> `h264_nvenc` -> `h264_vaapi` -> `mpeg4`）を自動選択。特許制限によりlibx264が同梱されないFlatpak環境等でも設定不要で動作。
- **多言語対応**: 日本語を含む14言語のネイティブ翻訳に対応。

### ご注意・仕様

- **FFmpegの検出**: 通常は `PATH` または標準配置ディレクトリから自動検出されます。独自の場所にインストールしている場合は、設定画面（`Generic` -> `customFfmpegPath`）でパスを指定してください。
- **保存先ディレクトリ**: 録画ファイルはデフォルトで `<ゲームディレクトリ>/video/` に保存されます。絶対パスまたは相対パスで自由に変更可能です。
- **結合処理**: MP4 / MOV / MKV 形式では、録画終了時に映像ストリームの再エンコードを行わず高速結合（Stream Copy）するため、数ミリ秒で保存が完了します。

### ライセンス

本プロジェクトは **GNU Lesser General Public License v3.0 (LGPL-3.0)** の下で公開されています。
