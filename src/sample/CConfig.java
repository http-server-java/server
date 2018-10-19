package sample;

public class CConfig {
    private String path_listing;
    private int port;

    String getPathListing()
    {
        return path_listing;
    }
    int getPortServer()
    {
        return port;
    }
    public CConfig(String path, int port_from_json){
        this.path_listing = path;
        this.port = port_from_json;
    }
}