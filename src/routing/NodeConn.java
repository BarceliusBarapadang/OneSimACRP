package routing;

import java.util.*;
import core.*;

public interface NodeConn {
  public Set<DTNHost> getNodeConnected(DTNHost host);
}
