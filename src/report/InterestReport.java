package report;

import java.util.*;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimScenario;
import core.UpdateListener;
import lombok.val;

public class InterestReport extends Report implements MessageListener, UpdateListener {
  private Map<String, Integer> msgDelive;
	private Map<String, Double> creationTimes;

  /**
   * key => index dari list interest [0,1,2,3,4]
   * value => total node terhadap interest
   */
  private Map<Integer, Integer> nodeTopics;
  private Map<Integer, Integer> tes;
  private Map<Integer, Integer> msgTopicsDelivered;
  private Map<Integer, Integer> msgTopicsCreated;
  private Map<Integer, List<Double>> latencyPerInterest;
  private Set<Message> setOfMsg;
  private Map<DTNHost, List<Boolean>> totalNodeWithTopic;

  /**
   * Constructor
   */
  public InterestReport() {
    init();
  }

  @Override
  protected void init() {
    super.init();

    this.creationTimes = new HashMap<>();
    this.msgDelive = new HashMap<>();
    this.setOfMsg = new HashSet<>();
    this.totalNodeWithTopic = new HashMap<>();
    this.nodeTopics = new HashMap<>();
    this.msgTopicsDelivered = new HashMap<>();
    this.msgTopicsCreated = new HashMap<>();
    this.tes = new HashMap<>();
    this.latencyPerInterest = new HashMap<>();
  }

  public void updated(List<DTNHost> hosts) {
    for (DTNHost h : hosts) {
      this.totalNodeWithTopic.put(h, h.getSocialProfileOI());
    }
  }

  public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    if (isWarmupID(m.getId())) {
      return;
    }

    // if (dropped) {
    // this.nrofDropped++;
    // } else {
    // this.nrofRemoved++;
    // }

    // this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
  }

  public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    if (isWarmupID(m.getId())) {
      return;
    }

    // this.nrofAborted++;
  }

  public void messageTransferred(Message m, DTNHost from, DTNHost to,
      boolean finalTarget) {
    if (isWarmupID(m.getId())) {
      return;
    }

    // int index = 0;

    // for (Boolean topic : to.getSocialProfileOI()) {
    //   if (topic.booleanValue()) {
    //     int value = this.tes.get(index) != null ? this.tes.get(index) + 1 : 0;
    //     this.tes.put(index, value);
    //   }
    //   index++;
    // }

    // int topicIndex =
    if (finalTarget) {
      countMsgDelivered(to);

      // latency
      int i = 0;
      for(Boolean topic : to.getSocialProfileOI()) {
        if(topic.booleanValue()) {
          List<Double> value = this.latencyPerInterest.get(i) != null 
            ? this.latencyPerInterest.get(i)
            : new ArrayList<>();
          value.add(getSimTime() - this.creationTimes.get(m.getId()));
          this.latencyPerInterest.put(i, value);
        }
        i++;
      }
    }

    // this.nrofRelayed++;
    // if (finalTarget) {
    // this.latencies.add(getSimTime() - this.creationTimes.get(m.getId()));
    // this.nrofDelivered++;
    // this.hopCounts.add(m.getHops().size() - 1);

    // if (m.isResponse()) {
    // this.rtt.add(getSimTime() - m.getRequest().getCreationTime());
    // this.nrofResponseDelivered++;
    // }
    // }
  }

  public void newMessage(Message m) {
    if (isWarmup()) {
      addWarmupID(m.getId());
      return;
    }

    int i = 0;
    for (Boolean topic : (List<Boolean>) m.getProperty("topic")) {
      if (topic.booleanValue()) {
        int value = this.msgTopicsCreated.get(i) != null ? this.msgTopicsCreated.get(i) + 1 : 0;
        this.msgTopicsCreated.put(i, value);
      }
      i++;
    }

    this.creationTimes.put(m.getId(), getSimTime());
    // this.nrofCreated++;
    // if (m.getResponseSize() > 0) {
    //   this.nrofResponseReqCreated++;
    // }
  }

  public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    if (isWarmupID(m.getId())) {
      return;
    }

    // this.nrofStarted++;
  }

  @Override
  public void done() {
    write("Message stats for scenario " + getScenarioName() +
        "\nsim_time: " + format(getSimTime()));

    List<DTNHost> nodes = SimScenario.getInstance().getHosts();
    write("Total node : " + nodes.size());

    String statsText = "";

    // Iterator<Message> it = setOfMsg.iterator();
    // while (it.hasNext()) {
    // statsText += it.next().getProperty("topic") + "\n";
    // }

    // for(Map.Entry<DTNHost, List<Boolean>> entry : totalNodeWithTopic.entrySet())
    // {
    // statsText += entry.getKey() + " : " + entry.getValue() + "\n";
    // }

    for (DTNHost node : nodes) {
      countNodeTopic(node);
    }

    statsText += "============\n";
    statsText += "Total Node per Interest : \n";
    for(Map.Entry<Integer, Integer> entry : this.nodeTopics.entrySet()) {
      statsText += entry.getKey() + " : " + entry.getValue() + "\n";
    }

    statsText += "============\n";
    statsText += "Total Message Delivered per Interest : \n";
    for(Map.Entry<Integer, Integer> entry : this.msgTopicsDelivered.entrySet()) {
      statsText += entry.getKey() + " : " + entry.getValue() + "\n";
    }

    statsText += "============\n";
    statsText += "Success Rate per Interest : \n";
    int index = 0;
    for(Map.Entry<Integer, Integer> entry : this.msgTopicsDelivered.entrySet()) {
      statsText += entry.getKey() + " : " + (double)entry.getValue()/nodeTopics.get(index)/100.0 + "\n";
    }

    // statsText += this.nodeTopics + "\n";
    // statsText += this.tes + "\n";
    // statsText += this.msgTopicsDelivered + "\n";
    // statsText += this.msgTopicsCreated + "\n";
    // statsText += this.latencyPerInterest.size() + "\n";

    // for(Map.Entry<Integer, List<Double>> entry : this.latencyPerInterest.entrySet()) {
    //   statsText += entry.getKey() + " : " + getAverage(entry.getValue()) + "\n";
    // }
    // write("\n");
    // write(this.latencyPerInterest + "\n");
    write(statsText);
    super.done();
  }

  private void countNodeTopic(DTNHost host) {
    int i = 0;

    for (Boolean topic : host.getSocialProfileOI()) {
      if (topic.booleanValue()) {
        int value = this.nodeTopics.get(i) != null ? this.nodeTopics.get(i) + 1 : 0;
        this.nodeTopics.put(i, value);
      }
      i++;
    }
  }

  private void countMsgDelivered(DTNHost host) {
    int i = 0;
    for (Boolean topic : host.getSocialProfileOI()) {
      if (topic.booleanValue()) {
        int value = this.msgTopicsDelivered.get(i) != null ? this.msgTopicsDelivered.get(i) + 1 : 0;
        this.msgTopicsDelivered.put(i, value);
      }
      i++;
    }
  }
}
