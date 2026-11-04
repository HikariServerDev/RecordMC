package com.atsukimc.recordmc.compat;

import fi.dy.masa.malilib.util.FileUtils;

import java.io.File;

/**
 * MaLiLib 設定ディレクトリ取得のバージョン差異（<1.21.9: FileUtils.getConfigDirectory(), >=1.21.9: FileUtils.getConfigDirectoryAsPath().toFile()）を吸収するファサード
 */
public final class MaliLibConfigCompat {
    private MaliLibConfigCompat() {}

    public static File getConfigDirectory() {
        //? if >=1.21.9 {
        /*File dir = FileUtils.getConfigDirectoryAsPath().toFile();
        *///?} else {
        File dir = FileUtils.getConfigDirectory();
        //?}
        if (dir == null) {
            dir = new File("config");
        }
        return dir;
    }
}
