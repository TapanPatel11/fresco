package dk.alexandra.fresco.tools.mascot.mult;

import java.security.SecureRandom;

import dk.alexandra.fresco.framework.network.Network;
import dk.alexandra.fresco.framework.util.StrictBitVector;
import dk.alexandra.fresco.tools.mascot.MascotResourcePool;
import dk.alexandra.fresco.tools.mascot.TwoPartyProtocol;
import dk.alexandra.fresco.tools.ot.base.RotBatch;
import dk.alexandra.fresco.tools.ot.otextension.BristolRotBatch;

public class MultiplyShared extends TwoPartyProtocol {

  protected RotBatch<StrictBitVector> rot;
  protected int numLeftFactors;

  public MultiplyShared(MascotResourcePool resourcePool, Network network, Integer otherId, int numLeftFactors) {
    super(resourcePool, network, otherId);
    this.numLeftFactors = numLeftFactors;
    // TODO is mod bit length the correct parameter here?
    this.rot = new BristolRotBatch(getMyId(), otherId, getModBitLength(), resourcePool.getLambdaSecurityParam(),
        new SecureRandom(), network);
  }

}
