package tangle;

public class NodeAddress {
    private final String protocol, host, port;

    public NodeAddress(String address) {
        this.protocol = address.split(":")[0];
        this.host = address.split(":")[1].substring(2);
        this.port = address.split(":")[2];
    }

    public NodeAddress(String protocol, String host, String port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    public String buildAddress() {
        return protocol + "://" + host + ":" + port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }
}
