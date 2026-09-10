package com.xpola.player.Modules;

import com.xpola.player.Utils.Utils;

public class FileInfo {
    private final String name, extension;

    public FileInfo(String name, String extension) {
        this.name = name;
        this.extension = extension;
    }


    public String getExtension() {
        return Utils.objectToString(extension);
    }

    public String getName() {
        return Utils.objectToString(name);
    }


    public boolean isM3uFile() {
        return getExtension().equalsIgnoreCase("m3u") || getExtension().equalsIgnoreCase("xpl");
    }

}
