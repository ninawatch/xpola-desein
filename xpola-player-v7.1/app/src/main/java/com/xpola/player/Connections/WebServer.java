package com.xpola.player.Connections;

import android.content.Context;

import com.google.gson.Gson;
import com.xpola.player.Utils.Prefs;
import com.xpola.player.Utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {

    private Context context;
    private Prefs prefs;

    public WebServer(Context context) throws IOException {
        super(8080);
        this.context = context;
        this.prefs = new Prefs(context);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri.equals("/")) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getHtml("index.html"));
        } else if (uri.equals("/add") && session.getMethod() == Method.POST) {
            try {
                session.parseBody(new HashMap<>());
                Map<String, String> params = session.getParms();
                List<Map<String, String>> list = Utils.getListString(prefs.getString("list", "[]"));
                if (list == null) {
                    list = new ArrayList<>();
                }

                Map<String, String> item = new HashMap<>();
                item.put("name", params.get("name"));
                String color = params.get("color");
                if (color == null || color.isEmpty()) {
                    color = Utils.ColorString();
                }
                item.put("color", color);
                item.put("url", params.get("url"));
                item.put("user_agent", params.get("user_agent"));
                item.put("referer", params.get("referer"));
                item.put("license", params.get("license"));
                item.put("scheme", params.get("schema"));
                item.put("player", params.get("player"));
                // تم التعديل: الآن القيمة ستكون true فقط إذا كانت m3u
                item.put("isM3u", String.valueOf("m3u".equals(params.get("player_type"))));

                list.add(item);
                prefs.setString("list", new Gson().toJson(list));

                // عند النجاح، التوجيه إلى add.html
                return newFixedLengthResponse(Response.Status.OK, "text/html", getHtml("add.html"));

            } catch (IOException | ResponseException e) {
                e.printStackTrace();
                // عند حدوث خطأ، التوجيه إلى erreur.html
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", getHtml("erreur.html"));
            }
        } else if (uri.startsWith("/delete")) {
            try {
                Map<String, String> params = session.getParms();
                int position = Integer.parseInt(params.get("position"));
                List<Map<String, String>> list = Utils.getListString(prefs.getString("list", "[]"));
                if (list != null && position < list.size()) {
                    list.remove(position);
                    prefs.setString("list", new Gson().toJson(list));
                    // عند النجاح، التوجيه إلى delete.html
                    return newFixedLengthResponse(Response.Status.OK, "text/html", getHtml("delete.html"));
                } else {
                    // إذا كان الموضع غير صالح، التوجيه إلى erreur.html
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/html", getHtml("erreur.html"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                // عند حدوث خطأ، التوجيه إلى erreur.html
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", getHtml("erreur.html"));
            }
        } else if (uri.equals("/add.html")) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getHtml("add.html"));
        } else if (uri.equals("/delete.html")) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getHtml("delete.html"));
        } else if (uri.equals("/erreur.html")) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getHtml("erreur.html"));
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", getHtml("erreur.html"));
    }

    private String getHtml(String filename) {
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String html = new String(buffer);

            // فقط لملف index.html نقوم باستبدال البيانات
            if (filename.equals("index.html")) {
                List<Map<String, String>> list = Utils.getListString(prefs.getString("list", "[]"));
                StringBuilder sb = new StringBuilder();
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        Map<String, String> item = list.get(i);
                        sb.append("<tr>");
                        sb.append("<td>").append(item.get("name")).append("</td>");
                        sb.append("<td>").append(item.get("url")).append("</td>");
                        sb.append("<td><a href='/delete?position=").append(i).append("'>Delete</a></td>");
                        sb.append("</tr>");
                    }
                }
                html = html.replace("<!--DATA-->", sb.toString());
            }

            return html;
        } catch (IOException e) {
            e.printStackTrace();
            return "<html><body><h1>Error loading " + filename + "</h1></body></html>";
        }
    }
}