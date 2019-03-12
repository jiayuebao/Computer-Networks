public class GraphNode {
    int distance;
    String uuid;
    public GraphNode(String uuid, int distance) {
        this.uuid = uuid;
        this.distance = distance;
    }

    public String getUuid() {
        return this.uuid;
    }

    public int getDistance() {
        return this.distance;
    }
}
