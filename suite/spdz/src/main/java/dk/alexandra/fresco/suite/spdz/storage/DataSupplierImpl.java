package dk.alexandra.fresco.suite.spdz.storage;

import dk.alexandra.fresco.framework.MPCException;
import dk.alexandra.fresco.framework.sce.resources.storage.StreamedStorage;
import dk.alexandra.fresco.framework.sce.resources.storage.exceptions.NoMoreElementsException;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzInputMask;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzSInt;
import dk.alexandra.fresco.suite.spdz.datatypes.SpdzTriple;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data supplier which supplies the SPDZ protocol suite with preprocessed data.
 * It fetches data from the native storage object within FRESCO and assumes that
 * something else put it there already. See e.g. @NewDataRetriever for a way to
 * do so.
 * 
 * @author Kasper Damgaard
 *
 */
public class DataSupplierImpl implements DataSupplier {

  public static final String STORAGE_FOLDER = "spdz/";
  public static final String SSK_KEY = "SSK";
  public static final String MODULUS_KEY = "MOD_P";
  public static final String TRIPLE_KEY_PREFIX = "TRIPLE_";
  public static final String EXP_PIPE_KEY_PREFIX = "EXP_PIPE_";
  public static final String SQUARE_KEY_PREFIX = "SQUARE_";
  public static final String BIT_KEY_PREFIX = "BIT_";
  public static final String INPUT_KEY_PREFIX = "INPUT_";
  public static final String STORAGE_NAME_PREFIX = STORAGE_FOLDER+"SPDZ_";

  public static final String GLOBAL_STORAGE = "GLOBAL";
  public static final String INPUT_STORAGE = "INPUT_";
  public static final String EXP_PIPE_STORAGE = "EXP";
  public static final String TRIPLE_STORAGE = "TRIPLE";
  public static final String BIT_STORAGE = "BIT";
  
  private final static Logger logger = LoggerFactory.getLogger(DataSupplierImpl.class);

  private StreamedStorage storage;
  private String storageName;

  private int tripleCounter = 0;
  private int expPipeCounter = 0;
  private int[] inputMaskCounters;
  private int bitCounter = 0;

  private BigInteger ssk;
  private BigInteger mod;

  /**
   * Creates a new supplier which takes preprocessed data from the native
   * storage object of FRESCO.
   * 
   * @param storage
   *            The FRESCO native storage object
   * @param storageName
   *            The name of the 'database' we should use (e.g. the full filename).
   * @param noOfParties
   *            The number of parties in the computation.
   */
  public DataSupplierImpl(StreamedStorage storage, String storageName,
      int noOfParties) {
    this.storage = storage;
    this.storageName = storageName;
    this.inputMaskCounters = new int[noOfParties];
  }

  @Override
  public SpdzTriple getNextTriple() {
    SpdzTriple trip;
    try {
      trip = this.storage.getNext(storageName+
          DataSupplierImpl.TRIPLE_STORAGE);
    } catch (NoMoreElementsException e) {
      logger
          .error("Triple no. " + tripleCounter + " was not present in the storage: " + storageName + DataSupplierImpl.TRIPLE_STORAGE);
      throw new MPCException(
          "Triple no. " + tripleCounter + " was not present in the storage: " + storageName + DataSupplierImpl.TRIPLE_STORAGE, e);
    }		
    tripleCounter ++;
    return trip;
  }

  @Override
  public SpdzSInt[] getNextExpPipe() {
    SpdzSInt[] expPipe;
    try {
      expPipe = this.storage.getNext(storageName+DataSupplierImpl.EXP_PIPE_STORAGE);
    } catch (NoMoreElementsException e) {
      logger
          .error("expPipe no. " + expPipeCounter + " was not present in the storage: " + storageName + DataSupplierImpl.EXP_PIPE_STORAGE);
      throw new MPCException(
          "expPipe no. " + expPipeCounter + " was not present in the storage: " + storageName + DataSupplierImpl.EXP_PIPE_STORAGE, e);
    }	
    expPipeCounter ++;
    return expPipe;
  }

  @Override
  public SpdzInputMask getNextInputMask(int towardPlayerID) {
    SpdzInputMask mask;
    try {
      mask = this.storage.getNext(storageName +
          DataSupplierImpl.INPUT_STORAGE + towardPlayerID);
    } catch (NoMoreElementsException e) {
      logger.error("Mask no. " + inputMaskCounters[towardPlayerID - 1] + " towards player "
          + towardPlayerID + " was not present in the storage "
          + (storageName + DataSupplierImpl.INPUT_STORAGE + towardPlayerID));
      throw new MPCException("Mask no. " + inputMaskCounters[towardPlayerID - 1]
          + " towards player " + towardPlayerID + " was not present in the storage "
          + (storageName + DataSupplierImpl.INPUT_STORAGE + towardPlayerID), e);
    }
    inputMaskCounters[towardPlayerID-1]++;		
    return mask;
  }

  @Override
  public SpdzSInt getNextBit() {
    SpdzSInt bit;
    try {
      bit = this.storage.getNext(storageName + 
          DataSupplierImpl.BIT_STORAGE);
    } catch (NoMoreElementsException e) {
      logger.warn("Bit no. " + bitCounter + " was not present in the storage: " + storageName + DataSupplierImpl.BIT_STORAGE);
      throw new MPCException("Bit no. " + bitCounter + " was not present in the storage: " + storageName + DataSupplierImpl.BIT_STORAGE, e);
    }
    bitCounter++;
    return bit;
  }

  @Override
  public BigInteger getModulus() {
    if(this.mod != null) {
      return this.mod;
    }
    try {
      this.mod = this.storage.getNext(storageName +
          DataSupplierImpl.MODULUS_KEY);
    } catch (NoMoreElementsException e) {
      throw new MPCException("Modulus was not present in the storage "+ storageName + DataSupplierImpl.MODULUS_KEY);
    }		
    return this.mod;
  }

  @Override
  public BigInteger getSSK() {
    if(this.ssk != null) {
      return this.ssk;
    }
    try {
      this.ssk = this.storage.getNext(storageName+
          DataSupplierImpl.SSK_KEY);
    } catch (NoMoreElementsException e) {
      throw new MPCException("SSK was not present in the storage "+ storageName + DataSupplierImpl.SSK_KEY);
    }
    return this.ssk;
  }

  @Override
  public SpdzSInt getNextRandomFieldElement() {
    // TODO: We should probably have a random element storage stream 
    return new SpdzSInt(this.getNextTriple().getA());
  }
}
