package routing.ACRP;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;

import java.util.HashMap;
import java.util.Map;

public class Pheromone {
    protected Map<DTNHost, Map<DTNHost, Double>> pheromoneTable; // Menyimpan pheromone berdasarkan hubungan antar node

    public static final String EVAPORATION_RATE_SETTINGS = "evaporationRate";   // Bobot evaporasi Pheromone
    public static final String HEURISTIC_WEIGHT_SETTINGS = "heuristicWeight";   // Bobot heuristik

    protected static double EVAPORATION_RATE = 0.1;  // Nilai default jika tidak menggunakan pengaturan
    protected static double HEURISTIC_WEIGHT = 1.0;   // Nilai default untuk bobot heuristik

    // Konstruktor tanpa parameter Settings
    public Pheromone() {
        pheromoneTable = new HashMap<>();
    }

    // Konstruktor yang memungkinkan pengaturan EVAPORATION_RATE dan HEURISTIC_WEIGHT dari Settings
    public Pheromone(Settings s) {
        this();  // Memanggil konstruktor default
        if (s != null) {
            if (s.contains(EVAPORATION_RATE_SETTINGS)) {
                EVAPORATION_RATE = s.getDouble(EVAPORATION_RATE_SETTINGS);
            }
            if (s.contains(HEURISTIC_WEIGHT_SETTINGS)) {
                HEURISTIC_WEIGHT = s.getDouble(HEURISTIC_WEIGHT_SETTINGS);
            }
        }
    }

    // Menambahkan entri baru untuk pheromone jika tidak ada
    public void createPheromoneTable(Message m) {
        DTNHost destination = m.getTo();
        pheromoneTable.putIfAbsent(destination, new HashMap<>());
        m.addProperty("destination", destination);
        m.addProperty("pathLength", m.getHopCount());
    }

    // Mendapatkan nilai pheromone antara dua node
    public double getPheromone(DTNHost peer, Message m) {
        DTNHost destination = (DTNHost) m.getProperty("destination");
        Map<DTNHost, Double> destinationTable = pheromoneTable.get(destination);
        if (destinationTable == null) return 0;
        return destinationTable.getOrDefault(peer, 0.0);
    }

    // Mengupdate pheromone untuk jalur yang telah dilalui berdasarkan heuristik
    public void updatePheromone(DTNHost thisHost, DTNHost from, Message m) {
        for (Connection con : thisHost.getConnections()) {
            DTNHost neighbor = con.getOtherNode(thisHost);
            double pathLength = (Double) m.getProperty("pathLength");
            
            // Mengambil informasi heuristik yang disimpan (misalnya estimasi waktu atau jarak)
            double heuristicValue = (Double) m.getProperty("heuristicValue"); // Misalnya, "heuristicValue" bisa berupa estimasi jarak ke tujuan

            // Menghitung feromon baru dengan mempertimbangkan heuristik
            double currentPheromone = getPheromone(neighbor, m);
            double newPheromone = (1 - EVAPORATION_RATE) * currentPheromone 
                                  + (pathLength * HEURISTIC_WEIGHT) 
                                  + (heuristicValue * HEURISTIC_WEIGHT);
                                  
            // Update pheromone untuk neighbor yang merupakan next hop
            Map<DTNHost, Double> destinationTable = pheromoneTable.get((DTNHost) m.getProperty("destination"));
            if (destinationTable != null) {
                destinationTable.put(neighbor, newPheromone);
            }
        }
    }
}
