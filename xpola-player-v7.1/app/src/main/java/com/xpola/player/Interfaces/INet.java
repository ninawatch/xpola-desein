package com.xpola.player.Interfaces;


public interface INet {
    void onSuccess(String data, String url);

    void onFailed(String error, String url);
}
