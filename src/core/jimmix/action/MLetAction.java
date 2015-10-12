package jimmix.action;

import javax.management.ObjectName;
import java.io.*;
import java.net.*;

import com.sun.net.httpserver.*;

import jimmix.util.TypeConvertor;
import jimmix.util.Log;
import jimmix.proxy.ProxyType;

public class MLetAction extends Action {

    private String objectName = null;
    private String hostingUrl = null;
    private String jarName = null;
    private String className = null;
    private boolean internalHttpServer = true;

    public MLetAction(String url, ProxyType type, String hostingUrl, String jarName, String className, boolean internalHttpServer) throws Exception {
        super(url, type);
        this.hostingUrl = hostingUrl;
        this.jarName = jarName;
        this.className = className;
        this.objectName = ":name=" + className;
        this.internalHttpServer = internalHttpServer;
    }

    public void invoke() throws Exception {
        Log.log("creating mbean " + this.objectName + " from " + this.className);
        HttpServer server = null;
        if (this.internalHttpServer) {
            Log.log("starting web server");
            int port = new URL(this.hostingUrl).getPort();
            if (port == -1) {
                port = 80;
            }
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new RequestHandler(this.jarName, this.className, this.objectName, this.hostingUrl));
            server.setExecutor(null); // creates a default executor
            server.start();
        }

        try {
            this.proxy.mlet(new ObjectName(objectName), this.hostingUrl);
            Log.log("done");
        } catch (Exception e) {
            Log.error("something went wrong");
        }

        if (server != null) {
            server.stop(0);
        }
    }

    static class RequestHandler implements HttpHandler {

        private String jarName = null;
        private String className = null;
        private String objectName = null;
        private String url = null;

        public RequestHandler(String jarName, String className, String objectName, String url) {
            this.jarName = jarName;
            this.className = className;
            this.objectName = objectName;
            this.url = url;
        }

        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestURI().getPath().contains(".jar")) {
                Log.log("request received for jar file");
                File file = new File(jarName);
                byte[] bytearray = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(bytearray, 0, bytearray.length);
                t.sendResponseHeaders(200, file.length());
                OutputStream os = t.getResponseBody();
                os.write(bytearray, 0, bytearray.length);
                os.close();
            } else {
                Log.log("request received for mlet entry");
                String archive = new File(jarName).getName();
                String response = String.format("<MLET CODE=%s ARCHIVE=%s NAME=%s CODEBASE=%s></MLET>", className, archive, objectName, url);
                Log.log("sending " + response);
                t.sendResponseHeaders(200, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
}
