import java.util.Map;

public class Advertisement {
        long sequence;
        Map<String, Integer> metrics;

        public Advertisement(long sequence, Map<String, Integer> metrics) {
            this.sequence = sequence;
            this.metrics = metrics;
        }

        public long getSequence() {
            return this.sequence;
        }

        public void setSequence(long sequence) {
            this.sequence = sequence;
        }

        public Map<String, Integer> getMetrics() {
            return this.metrics;
        }
}
