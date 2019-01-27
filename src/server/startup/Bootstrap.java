package server.startup;

import server.connector.http.HttpConnector;

public class Bootstrap {
    public static void main(String[] args) {
        HttpConnector connector = new HttpConnector();
        connector.start();
    }
}
