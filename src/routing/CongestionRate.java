package routing;

import java.util.List;

import core.DTNHost;

public interface CongestionRate {
  public List<Double> getDataInContactNode(DTNHost host);
  public List<Double> getCRNode(DTNHost host);
  public List<Double> getEmaOfCR(DTNHost host);
}
