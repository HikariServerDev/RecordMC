package com.atsukimc.recordmc.recorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 録画完了した動画ファイルをOSのシステムクリップボードへ自動コピーするヘルパークラス。
 *
 * <p>設計上の特徴:
 * <ul>
 *   <li>Java AWT Clipboard (javaFileListFlavor, text/uri-list, gnome-copied-files) を第一選択として使用。</li>
 *   <li>Linux環境においてAWTでクリップボード共有が正常に機能しない場合（Wayland環境等）、
 *       外部コマンド（wl-copy または xclip）への安全なフォールバックを実行。</li>
 *   <li>失敗時も例外を握りつぶさず、原因調査のためのデバッグログを記録。</li>
 * </ul>
 */
public class ClipboardHelper {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/Clipboard");

    static {
        // Minecraftがクライアント環境でheadlessと誤判定されるのを防ぐため明示的に解除
        try {
            System.setProperty("java.awt.headless", "false");
        } catch (Throwable t) {
            LOGGER.debug("Could not modify java.awt.headless property: {}", t.getMessage());
        }
    }

    /**
     * 指定された動画ファイルをシステムクリップボードにコピーする。
     *
     * @param file コピー対象の動画ファイル
     * @return コピーに成功した場合はtrue、失敗した場合はfalse
     */
    public static boolean copyFileToClipboard(File file) {
        if (file == null || !file.exists()) {
            LOGGER.warn("Cannot copy to clipboard: file does not exist: {}", file != null ? file.getAbsolutePath() : "null");
            return false;
        }

        boolean success = false;

        // 1. Java AWT Toolkit によるクリップボード設定
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            if (toolkit != null && toolkit.getSystemClipboard() != null) {
                FileTransferable transferable = new FileTransferable(file);
                toolkit.getSystemClipboard().setContents(transferable, null);
                LOGGER.info("Copied file to system clipboard via AWT: {}", file.getAbsolutePath());
                success = true;
            }
        } catch (Throwable t) {
            LOGGER.debug("AWT clipboard copy failed, attempting OS command fallback: {}", t.getMessage());
        }

        // 2. Linux環境 (Wayland / X11) でのネイティブコマンドフォールバック
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux") && !success) {
            success = tryLinuxClipboardCommands(file);
        }

        if (!success) {
            LOGGER.warn("Failed to copy video to system clipboard through all available mechanisms");
        }

        return success;
    }

    /**
     * Linux環境において wl-copy (Wayland) または xclip (X11) を使ってファイルURIをクリップボードに設定する。
     */
    private static boolean tryLinuxClipboardCommands(File file) {
        String uri = file.toURI().toString();

        // 1. Wayland環境: wl-copy
        try {
            Process p = new ProcessBuilder("wl-copy", "--type", "text/uri-list", uri).start();
            boolean finished = p.waitFor(3, TimeUnit.SECONDS);
            if (finished && p.exitValue() == 0) {
                LOGGER.info("Copied file URI to Wayland clipboard via wl-copy");
                return true;
            }
            if (p.isAlive()) {
                p.destroyForcibly();
            }
        } catch (Throwable t) {
            LOGGER.debug("wl-copy execution failed: {}", t.getMessage());
        }

        // 2. X11環境: xclip
        try {
            Process p = new ProcessBuilder("xclip", "-selection", "clipboard", "-t", "text/uri-list").start();
            try (OutputStream os = p.getOutputStream()) {
                os.write((uri + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            boolean finished = p.waitFor(3, TimeUnit.SECONDS);
            if (finished && p.exitValue() == 0) {
                LOGGER.info("Copied file URI to X11 clipboard via xclip");
                return true;
            }
            if (p.isAlive()) {
                p.destroyForcibly();
            }
        } catch (Throwable t) {
            LOGGER.debug("xclip execution failed: {}", t.getMessage());
        }

        return false;
    }

    /**
     * 各種OS・デスクトップ環境（Windows, macOS, GNOME, KDE等）のファイル貼り付け仕様に対応したTransferable。
     */
    public static class FileTransferable implements Transferable {
        private final List<File> files;
        private final String uriList;
        private final String gnomeFormat;
        private final String path;

        private static DataFlavor URI_LIST_FLAVOR;
        private static DataFlavor GNOME_COPIED_FLAVOR;

        static {
            try {
                URI_LIST_FLAVOR = new DataFlavor("text/uri-list;class=java.lang.String");
                GNOME_COPIED_FLAVOR = new DataFlavor("x-special/gnome-copied-files;class=java.lang.String");
            } catch (ClassNotFoundException e) {
                LOGGER.debug("Could not initialize extended DataFlavors: {}", e.getMessage());
            }
        }

        public FileTransferable(File file) {
            this.files = Collections.singletonList(file);
            this.path = file.getAbsolutePath();
            this.uriList = file.toURI().toString() + "\r\n";
            this.gnomeFormat = "copy\n" + file.toURI().toString();
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            if (URI_LIST_FLAVOR != null && GNOME_COPIED_FLAVOR != null) {
                return new DataFlavor[]{
                        DataFlavor.javaFileListFlavor,
                        URI_LIST_FLAVOR,
                        GNOME_COPIED_FLAVOR,
                        DataFlavor.stringFlavor
                };
            } else {
                return new DataFlavor[]{
                        DataFlavor.javaFileListFlavor,
                        DataFlavor.stringFlavor
                };
            }
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (flavor == null) {
                return false;
            }
            if (DataFlavor.javaFileListFlavor.equals(flavor) || DataFlavor.stringFlavor.equals(flavor)) {
                return true;
            }
            if (URI_LIST_FLAVOR != null && URI_LIST_FLAVOR.equals(flavor)) {
                return true;
            }
            if (GNOME_COPIED_FLAVOR != null && GNOME_COPIED_FLAVOR.equals(flavor)) {
                return true;
            }
            return false;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                return files;
            } else if (DataFlavor.stringFlavor.equals(flavor)) {
                return path;
            } else if (URI_LIST_FLAVOR != null && URI_LIST_FLAVOR.equals(flavor)) {
                return uriList;
            } else if (GNOME_COPIED_FLAVOR != null && GNOME_COPIED_FLAVOR.equals(flavor)) {
                return gnomeFormat;
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
