package routing;

import core.*;
import routing.ACRP.Duration;
import routing.ACRP.Pheromone;

import java.util.*;

public class AntColonyRoutingProtocol extends ActiveRouter {

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;
    protected Pheromone pheromoneTable;

    public enum antTypes { FORWARD, BACKWARD }

    protected antTypes type;
    protected Message msg;

    private double alpha = 1.0;         // Bobot pheromone
    private double heuristicBeta = 2.0; // Bobot heuristic

    public AntColonyRoutingProtocol(Settings s) {
        super(s);
        this.pheromoneTable = new Pheromone();
        this.startTimestamps = new HashMap<>();
        this.connHistory = new HashMap<>();
    }

    protected AntColonyRoutingProtocol(AntColonyRoutingProtocol proto) {
        super(proto);
        this.pheromoneTable = proto.pheromoneTable;
        this.startTimestamps = new HashMap<>();
        this.connHistory = new HashMap<>();
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message msg = super.messageTransferred(id, from);

        if(msg.getTo() == getHost()){
        pheromoneTable.createPheromoneTable(msg);
        msg.updateProperty("antType", antTypes.BACKWARD);
        }

        if (msg.getProperty("antType") !=null) {
//            System.out.println(msg.getProperty("antType"));
            if (msg.getProperty("antType").equals(antTypes.FORWARD)) {
                //update pheromone

                pheromoneTable.updatePheromone(getHost(), from, msg);
            }
        }

        return msg;
    }

    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);

        if (con.isUp()) {
            DTNHost other = con.getOtherNode(getHost());
            AntColonyRoutingProtocol otherRouter = (AntColonyRoutingProtocol) other.getRouter();

            this.startTimestamps.put(other, SimClock.getTime());
            otherRouter.startTimestamps.put(getHost(), SimClock.getTime());
        } else {
            DTNHost other = con.getOtherNode(getHost());
            double time = startTimestamps.get(other);
            double etime = SimClock.getTime();

            // Find or create the connection history list
            List<Duration> history;
            if(!connHistory.containsKey(other))
            {
                history = new LinkedList<Duration>();
                connHistory.put(other, history);
            }
            else
                history = connHistory.get(other);

            // add this connection to the list
            if(etime - time > 0)
                history.add(new Duration(time, etime));

            startTimestamps.remove(other);
        }

    }

    @Override
    public void update() {
         super.update();
        if (!canStartTransfer() ||isTransferring()) {
            return; // nothing to transfer or is currently transferring
        }

        // try messages that could be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        // Cek semua pesan dalam buffer jika berasal dari current host, maka kirim semua pesan ke tetangga
        for (Message m : getMessageCollection()) {
            if (getHost().equals(m.getFrom())) {
                // Jika pesan berasal dari node ini, coba kirim ke semua koneksi
                tryAllMessagesToAllConnections();
                return;
            }
        }

        tryOtherMessages();
    }

    private Tuple<Message, Connection> tryOtherMessages() {
    Collection<Message> msgCollection = getMessageCollection();
    List<Connection> connections = getConnections();

    for (Message m : msgCollection) {
        List<DTNHost> candidates = new ArrayList<>();

        for (Connection con : connections) {
            DTNHost other = con.getOtherNode(getHost());
            AntColonyRoutingProtocol othRouter = (AntColonyRoutingProtocol) other.getRouter();

            if (othRouter.isTransferring() || othRouter.hasMessage(m.getId())) {
                continue;
            }

            candidates.add(other);
        }

        if (!candidates.isEmpty()) {
            DTNHost nextHop = selectNextHopProbabilistically(m, candidates);

            if (nextHop != null) {
                for (Connection con : connections) {
                    if (con.getOtherNode(getHost()).equals(nextHop)) {
                        return tryMessagesForConnected(
                            List.of(new Tuple<>(m, con))
                        );
                    }
                }
            }
        }
    }

    return null;
}

    private DTNHost selectNextHopProbabilistically(Message msg, List<DTNHost> candidates) {
        Map<DTNHost, Double> probMap = new HashMap<>();
        double denominator = 0.0;

        for (DTNHost neighbor : candidates) {
            double pheromone = pheromoneTable.getPheromone(neighbor, msg);
            double heuristic = getHeuristic(neighbor);
            double value = Math.pow(pheromone, alpha) * Math.pow(heuristic, heuristicBeta);
            probMap.put(neighbor, value);
            denominator += value;
        }

        if (denominator == 0) return null;

        double rand = Math.random();
        double cumulative = 0.0;

        for (Map.Entry<DTNHost, Double> entry : probMap.entrySet()) {
            cumulative += entry.getValue() / denominator;
            if (rand <= cumulative) {
                return entry.getKey();
            }
        }

        return null;
    }

    private double getHeuristic(DTNHost neighbor) {
        // Heuristik sederhana: konstan. Dapat dimodifikasi menjadi berdasarkan delay, frekuensi kontak, dsb.
        return 1.0;
    }

    @Override
    public MessageRouter replicate() {
        return new AntColonyRoutingProtocol(this);
    }
} 
