package sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;
import java.net.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

public class Main implements HttpHandler {
    static String PATH;
    public static void main(String args[]) throws IOException {
        System.setProperty("file.encoding","Windows-1251");

        String path_to_config = getCurrentPath() + "config.json";
        String json_content = getContentJSONFile(path_to_config);

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        CConfig config = gson.fromJson(json_content, CConfig.class);

        String path_listing = config.getPathListing();
        Integer port = config.getPortServer();

        System.out.println(path_listing);
        PATH = path_listing;

        gson.toJson(config);
        System.out.println("Starting web server...");
        String local_ip = getLocalIPComputer();
        System.out.println("IP Server: " + local_ip + ":" + port);


        HttpServer server = HttpServer.create();

        server.bind(new InetSocketAddress(local_ip, port), 0);
        create(server, "/", path_listing, "index.html");
        server.start();
    }
    public void setPathListingServer(String path)
    {
        PATH = path;
    }
    private static final Map<String,String> MIME_MAP = new HashMap<>();
    static {
        MIME_MAP.put("appcache", "text/cache-manifest");
        MIME_MAP.put("css", "text/css");
        MIME_MAP.put("gif", "image/gif");
        MIME_MAP.put("html", "text/html");
        MIME_MAP.put("js", "application/javascript");
        MIME_MAP.put("json", "application/json");
        MIME_MAP.put("jpg", "image/jpeg");
        MIME_MAP.put("jpeg", "image/jpeg");
        MIME_MAP.put("mp4", "video/mp4");
        MIME_MAP.put("pdf", "application/pdf");
        MIME_MAP.put("png", "image/png");
        MIME_MAP.put("svg", "image/svg+xml");
        MIME_MAP.put("xlsm", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_MAP.put("xml", "application/xml");
        MIME_MAP.put("zip", "application/zip");
        MIME_MAP.put("md", "text/plain");
        MIME_MAP.put("txt", "text/plain");
        MIME_MAP.put("php", "text/plain");
    };

    private String filesystemRoot;
    private String urlPrefix;
    private String directoryIndex;

    /**
     * @param urlPrefix The prefix of all URLs.
     *                   This is the first argument to createContext. Must start and end in a slash.
     * @param filesystemRoot The root directory in the filesystem.
     *                       Only files under this directory will be served to the client.
     *                       For instance "./staticfiles".
     * @param directoryIndex File to show when a directory is requested, e.g. "index.html".
     */
    public Main(String urlPrefix, String filesystemRoot, String directoryIndex) {
        if (!urlPrefix.startsWith("/")) {
            throw new RuntimeException("pathPrefix does not start with a slash");
        }
        if (!urlPrefix.endsWith("/")) {
            throw new RuntimeException("pathPrefix does not end with a slash");
        }
        this.urlPrefix = urlPrefix;

        assert filesystemRoot.endsWith("/");
        try {
            this.filesystemRoot = new File(filesystemRoot).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.directoryIndex = directoryIndex;
    }

    /**
     * Create and register a new static file handler.
     * @param hs The HTTP server where the file handler will be registered.
     * @param path The path in the URL prefixed to all requests, such as "/static/"
     * @param filesystemRoot The filesystem location.
     *                       For instance "/var/www/mystaticfiles/".
     *                       A request to "/static/x/y.html" will be served from the filesystem file "/var/www/mystaticfiles/x/y.html"
     * @param directoryIndex File to show when a directory is requested, e.g. "index.html".
     */
    public static void create(HttpServer hs, String path, String filesystemRoot, String directoryIndex) {
        Main sfh = new Main(path, filesystemRoot, directoryIndex);
        hs.createContext(path, sfh);
    }
    public static String getContentJSONFile(String path_to_config) throws IOException {
        String content;
        if (!new File(path_to_config).exists())
        {
            try (JsonWriter writer = new JsonWriter(new FileWriter("config.json"))) {
                writer.beginObject();
                writer.setIndent("     ");
                writer.name("path_listing").value("D:/temp/ИП-41");
                writer.name("port").value(1336);
                writer.endObject();
                writer.close();
            }
        }
        try(BufferedReader br = new BufferedReader(new FileReader(path_to_config))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            content = sb.toString();
        }
        return content;
    }
    public void handle(HttpExchange he) throws IOException {
        Path path = Paths.get(PATH);
        final List<Path> files = listFiles(path);


        StringBuilder path_to_files = new StringBuilder();

        path_to_files.append("<!DOCTYPE html>\r\n" +
                "<html>\r\n" +
                "<head>\r\n" + "<meta charset=\"windows-1251\">" + "</head>\r\n" +
                "<body>\r\n");
        for (int i = 0; i < files.size(); i++) {
            path_to_files.append("<a href=\"" + path.relativize(files.get(i)).toString()
                    +"\">"
                    + path.relativize(files.get(i)).toString()
                    + "</a><br>"
                    + System.lineSeparator());


        }
        String ip_addr_client = he.getRemoteAddress().getAddress().toString();
        path_to_files.append("\r\n" +
                "<center><b>Ваш IP: " + ip_addr_client + "</b></center>" +
                "</body>\r\n" +
                "</html>");
        Files.write(path.toAbsolutePath().resolve("index.html"), path_to_files.toString().getBytes());

        String method = he.getRequestMethod();
        if (! ("HEAD".equals(method) || "GET".equals(method))) {
            sendError(he, 501, "Unsupported HTTP method");
            return;
        }

        String wholeUrlPath = he.getRequestURI().getPath();
        if (wholeUrlPath.endsWith("/")) {
            wholeUrlPath += directoryIndex;
        }
        if (! wholeUrlPath.startsWith(urlPrefix)) {
            throw new RuntimeException("Path is not in prefix - incorrect routing?");
        }
        String urlPath = wholeUrlPath.substring(urlPrefix.length());

        File f = new File(filesystemRoot, urlPath);
        File canonicalFile;
        try {
            canonicalFile = f.getCanonicalFile();
        } catch (IOException e) {
            // This may be more benign (i.e. not an attack, just a 403),
            // but we don't want the attacker to be able to discern the difference.
            reportPathTraversal(he);
            return;
        }

        String canonicalPath = canonicalFile.getPath();
        if (! canonicalPath.startsWith(filesystemRoot)) {
            reportPathTraversal(he);
            return;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(canonicalFile);
        } catch (FileNotFoundException e) {
            // The file may also be forbidden to us instead of missing, but we're leaking less information this way
            sendError(he, 404, "File not found");
            return;
        }

        String mimeType = lookupMime(urlPath);
        he.getResponseHeaders().set("Content-Type", mimeType);
        if ("GET".equals(method)) {
            he.sendResponseHeaders(200, canonicalFile.length());
            OutputStream os = he.getResponseBody();
            copyStream(fis, os);
            os.close();
        } else {
            assert("HEAD".equals(method));
            he.sendResponseHeaders(200, -1);
        }
        fis.close();
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) >= 0) {
            os.write(buf, 0, n);
        }
    }
    private void sendError(HttpExchange he, int rCode, String description) throws IOException {
        String message = "HTTP error " + rCode + ": " + description;
        byte[] messageBytes = message.getBytes("windows-1251");

        he.getResponseHeaders().set("Content-Type", "text/plain; charset=windows-1251");
        he.sendResponseHeaders(rCode, messageBytes.length);
        OutputStream os = he.getResponseBody();
        os.write(messageBytes);
        os.close();
    }

    // This is one function to avoid giving away where we failed
    private void reportPathTraversal(HttpExchange he) throws IOException {
        sendError(he, 400, "Path traversal attempt detected");
    }

    private static String getExt(String path) {
        int slashIndex = path.lastIndexOf('/');
        String basename = (slashIndex < 0) ? path : path.substring(slashIndex + 1);

        int dotIndex = basename.lastIndexOf('.');
        if (dotIndex >= 0) {
            return basename.substring(dotIndex + 1);
        } else {
            return "";
        }
    }

    private static String lookupMime(String path) {
        String ext = getExt(path).toLowerCase();
        return MIME_MAP.getOrDefault(ext, "application/octet-stream");
    }

    public static List<Path> listFiles(Path path) throws IOException {
        Deque<Path> stack = new ArrayDeque<Path>();
        final List<Path> files = new LinkedList<>();

        stack.push(path);

        while (!stack.isEmpty()) {
            DirectoryStream<Path> stream = Files.newDirectoryStream(stack.pop());
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    stack.push(entry);
                }
                else {
                    files.add(entry);
                }
            }
            stream.close();
        }

        return files;
    }
    public static String getLocalIPComputer() throws IOException
    {
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        }
    }
    public static String getCurrentPath()
    {
        File currentJavaJarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String currentJavaJarFilePath = currentJavaJarFile.getAbsolutePath();
        String currentRootDirectoryPath = currentJavaJarFilePath.replace(currentJavaJarFile.getName(), "");
        return currentRootDirectoryPath;
    }

}


