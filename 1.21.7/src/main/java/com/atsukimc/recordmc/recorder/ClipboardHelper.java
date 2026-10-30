package com.atsukimc.recordmc.recorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * 録画完了した動画ファイルをOSのクリップボードにコピーするヘルパークラス
 */
public class ClipboardHelper {
    private static final Logger LOGGER = LogManager.getLogger("RecordMC/Clipboard");

    static {
        // AWTのヘッドレスモードを解除
        try {
            System.setProperty("java.awt.headless", "false");
        } catch (Throwable ignored) {
        }
    }

    /**
     * ファイルをクリップボードにコピーする
     *
     * @param file コピー対象のファイル
     * @return 成功したかどうか
     */
    public static boolean copyFileToClipboard(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        boolean success = false;

        // 1. Java AWT Toolkit によるコピー
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            if (toolkit != null && toolkit.getSystemClipboard() != null) {
                FileTransferable transferable = new FileTransferable(file);
                toolkit.getSystemClipboard().setContents(transferable, null);
                LOGGER.info("Copied file to system clipboard via AWT: {}", file.getAbsolutePath());
                success = true;
            }
        } catch (Throwable t) {
            LOGGER.debug("AWT clipboard copy failed: {}", t.getMessage());
        }

        // 2. Linux (Wayland / X11) コマンドによるフォールバック
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux")) {
            if (!success) {
                success = tryLinuxClipboardCommands(file);
            }
        }

        return success;
    }

    private static boolean tryLinuxClipboardCommands(File file) {
        String uri = file.toURI().toString();
        // wl-copy (Wayland)
        try {
            Process p = new ProcessBuilder("wl-copy", "--type", "text/uri-list", uri).start();
            if (p.waitFor() == 0) {
                LOGGER.info("Copied to Wayland clipboard via wl-copy");
                return true;
            }
        } catch (Throwable ignored) {
        }

        // xclip (X11)
        try {
            Process p = new ProcessBuilder("xclip", "-selection", "clipboard", "-t", "text/uri-list").start();
            p.getOutputStream().write((uri + "\r\n").getBytes());
            p.getOutputStream().flush();
            p.getOutputStream().close();
            if (p.waitFor() == 0) {
                LOGGER.info("Copied to X11 clipboard via xclip");
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * ファイル転送用 Transferable
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
                LOGGER.debug("DataFlavor initialization: {}", e.getMessage());
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
