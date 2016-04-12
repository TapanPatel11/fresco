/*******************************************************************************
 * Copyright (c) 2015 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
package dk.alexandra.fresco.lib.arithmetic;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Assert;

import com.sun.swing.internal.plaf.basic.resources.basic;

import dk.alexandra.fresco.framework.ProtocolFactory;
import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.TestApplication;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThread;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadConfiguration;
import dk.alexandra.fresco.framework.TestThreadRunner.TestThreadFactory;
import dk.alexandra.fresco.framework.sce.SCE;
import dk.alexandra.fresco.framework.sce.SCEFactory;
import dk.alexandra.fresco.framework.value.OInt;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.compare.ComparisonProtocolFactoryImpl;
import dk.alexandra.fresco.lib.compare.MiscOIntGenerators;
import dk.alexandra.fresco.lib.compare.RandomAdditiveMaskFactory;
import dk.alexandra.fresco.lib.compare.RandomAdditiveMaskFactoryImpl;
import dk.alexandra.fresco.lib.field.integer.BasicNumericFactory;
import dk.alexandra.fresco.lib.helper.CopyProtocolImpl;
import dk.alexandra.fresco.lib.helper.builder.NumericIOBuilder;
import dk.alexandra.fresco.lib.helper.builder.NumericProtocolBuilder;
import dk.alexandra.fresco.lib.helper.sequential.SequentialProtocolProducer;
import dk.alexandra.fresco.lib.math.integer.NumericNegateBitFactory;
import dk.alexandra.fresco.lib.math.integer.NumericNegateBitFactoryImpl;
import dk.alexandra.fresco.lib.math.integer.NumericNegateBitProtocol;
import dk.alexandra.fresco.lib.math.integer.PreprocessedNumericBitFactory;
import dk.alexandra.fresco.lib.math.integer.binary.RepeatedRightShiftProtocol;
import dk.alexandra.fresco.lib.math.integer.binary.RightShiftFactory;
import dk.alexandra.fresco.lib.math.integer.binary.RightShiftFactoryImpl;
import dk.alexandra.fresco.lib.math.integer.binary.RightShiftProtocol;
import dk.alexandra.fresco.lib.math.integer.exp.ExpFromOIntFactory;
import dk.alexandra.fresco.lib.math.integer.exp.PreprocessedExpPipeFactory;
import dk.alexandra.fresco.lib.math.integer.inv.LocalInversionFactory;
import dk.alexandra.fresco.lib.math.integer.linalg.InnerProductFactory;
import dk.alexandra.fresco.lib.math.integer.linalg.InnerProductFactoryImpl;
import dk.alexandra.fresco.lib.math.integer.linalg.InnerProductProtocol;


/**
 * Generic test cases for basic finite field operations.
 * 
 * Can be reused by a test case for any protocol suite that implements the basic
 * field protocol factory.
 *
 * TODO: Generic tests should not reside in the runtime package. Rather in
 * mpc.lib or something.
 *
 */
public class BasicArithmeticTests {

	private abstract static class ThreadWithFixture extends TestThread {

		protected SCE sce;

		@Override
		public void setUp() throws IOException {
			sce = SCEFactory.getSCEFromConfiguration(conf.sceConf, conf.protocolSuiteConf);
		}

	}

	public static class TestInput extends TestThreadFactory {
		@Override
		public TestThread next(TestThreadConfiguration conf) {
			return new ThreadWithFixture() {
				@Override
				public void test() throws Exception {
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = 4338818809103728010L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							NumericIOBuilder ioBuilder = new NumericIOBuilder(
									prov);
							SInt input1 = ioBuilder.input(BigInteger.valueOf(10), 1);

							OInt output = ioBuilder.output(input1);
							ProtocolProducer io = ioBuilder.getCircuit();

							ProtocolProducer gp = new SequentialProtocolProducer(
									io);
							this.outputs = new OInt[] { output };
							return gp;
						}
					};

					sce.runApplication(app);

					Assert.assertEquals(BigInteger.valueOf(10),
							app.getOutputs()[0].getValue());
				}
			};
		}
	}
	
	public static class TestOutputToSingleParty extends TestThreadFactory {
		@Override
		public TestThread next(TestThreadConfiguration conf) {
			return new ThreadWithFixture() {
				@Override
				public void test() throws Exception {
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = 4338818809103728010L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							NumericIOBuilder ioBuilder = new NumericIOBuilder(
									prov);
							SInt input1 = ioBuilder.input(
									BigInteger.valueOf(10), 1);

							OInt output = ioBuilder.outputToParty(1, input1);							
							ProtocolProducer io = ioBuilder.getCircuit();

							ProtocolProducer gp = new SequentialProtocolProducer(
									io);
							this.outputs = new OInt[] { output };
							return gp;
						}
					};

					sce.runApplication(app);
					if(conf.netConf.getMyId() == 1) {
						Assert.assertEquals(BigInteger.valueOf(10),
								app.getOutputs()[0].getValue());
					} else {
						Assert.assertNull(app.getOutputs()[0].getValue());
					}
				}
			};
		}
	}
	
	public static class TestAddPublicValue extends TestThreadFactory {
		@Override
		public TestThread next(TestThreadConfiguration conf) {
			return new ThreadWithFixture() {
				@Override
				public void test() throws Exception {
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = 4338818809103728010L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							SequentialProtocolProducer gp = new SequentialProtocolProducer();
							NumericIOBuilder ioBuilder = new NumericIOBuilder(
									prov);
							SInt input1 = ioBuilder.input(
									BigInteger.valueOf(10), 1);

							gp.append(ioBuilder.getCircuit());
							ioBuilder.reset();
							
							BigInteger publicVal = BigInteger.valueOf(4);
							OInt openInput = prov.getOInt(publicVal);
							SInt out = prov.getSInt();
							ProtocolProducer addProtocol = prov.getAddProtocol(input1, openInput, out);
							gp.append(addProtocol);
							
							OInt output = ioBuilder.output(out);							
							ProtocolProducer io = ioBuilder.getCircuit();
							gp.append(io);
							
							this.outputs = new OInt[] { output };
							return gp;
						}
					};

					sce.runApplication(app);					
					Assert.assertEquals(BigInteger.valueOf(14),
							app.getOutputs()[0].getValue());					
				}
			};
		}
	}

	public static class TestCopyProtocol extends TestThreadFactory {

		@Override
		public TestThread next(TestThreadConfiguration conf) {
			return new ThreadWithFixture() {
				@Override
				public void test() throws Exception {
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = -8310958118835789509L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							NumericIOBuilder ioBuilder = new NumericIOBuilder(
									prov);
							
							SInt closed = ioBuilder.input(BigInteger.ONE, 1);
							ProtocolProducer inp = ioBuilder.getCircuit();
							ioBuilder.reset();
							
							SInt into = prov.getSInt();
							ProtocolProducer copy = new CopyProtocolImpl<SInt>(closed, into);
							OInt open = ioBuilder.output(into);
							ProtocolProducer out = ioBuilder.getCircuit();
							this.outputs = new OInt[] {open};
							
							
							SequentialProtocolProducer seq = new SequentialProtocolProducer(inp, copy, out);
							return seq;
						}
					};
					sce.runApplication(app);

					Assert.assertEquals(app.getOutputs()[0].getValue(), BigInteger.ONE);
				}
			};
		}
	};
	
	public static class TestLotsOfInputs extends TestThreadFactory {

		@Override
		public TestThread next(TestThreadConfiguration conf) {
			return new ThreadWithFixture() {
				@Override
				public void test() throws Exception {
					final int[] openInputs = new int[] { 11, 2, 3, 4, 5, 6, 7,
							8, 9, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = -8310958118835789509L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							NumericIOBuilder ioBuilder = new NumericIOBuilder(
									prov);
							SInt knownInput = prov.getSInt(BigInteger.valueOf(200));
							SInt[] inputs = createInputs(ioBuilder, openInputs,	1);
							inputs[0] = knownInput;

							OInt[] outputs = ioBuilder.outputArray(inputs);
							OInt knownOutput = ioBuilder.output(knownInput);
							outputs[0] = knownOutput;
							this.outputs = outputs;
							ProtocolProducer io = ioBuilder.getCircuit();

							ProtocolProducer gp = new SequentialProtocolProducer(
									io);
							return gp;
						}
					};
					sce.runApplication(app);
					
					checkOutputs(openInputs, app.getOutputs());
				}
			};
		}
	};
	
	public static class TestKnownSInt extends TestThreadFactory {

		@Override
		public TestThread next(TestThreadConfiguration conf) {
			return new ThreadWithFixture() {
				@Override
				public void test() throws Exception {
					final int[] openInputs = new int[] { 200, 300, 1, 2 };
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = -8310958118835789509L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							NumericIOBuilder ioBuilder = new NumericIOBuilder(
									prov);
							SInt knownInput1 = prov.getSInt(BigInteger.valueOf(200));
							SInt knownInput2 = prov.getSInt(BigInteger.valueOf(300));
							SInt knownInput3 = prov.getSInt(BigInteger.valueOf(1));
							SInt knownInput4 = prov.getSInt(BigInteger.valueOf(2));
							OInt knownOutput1 = ioBuilder.output(knownInput1);
							OInt knownOutput2 = ioBuilder.output(knownInput2);
							OInt knownOutput3 = ioBuilder.output(knownInput3);
							OInt knownOutput4 = ioBuilder.output(knownInput4);
							this.outputs = new OInt[]{ knownOutput1, knownOutput2, knownOutput3, knownOutput4 };
							ProtocolProducer io = ioBuilder.getCircuit();
							ProtocolProducer gp = new SequentialProtocolProducer(
									io);
							return gp;
						}
					};
					sce.runApplication(app);
					
					checkOutputs(openInputs, app.getOutputs());
				}
			};
		}
	};

	public static class TestSumAndMult extends TestThreadFactory {
		@Override
		public TestThread next(TestThreadConfiguration conf) {
			return new ThreadWithFixture() {
				@Override
				public void test() throws Exception {
					final int[] openInputs = new int[] { 1, 2, 3, 4, 5, 6, 7,
							8, 9, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = -8310958118835789509L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							NumericIOBuilder ioBuilder = new NumericIOBuilder(
									prov);

							SInt[] inputs = createInputs(ioBuilder, openInputs,
									1);

							ProtocolProducer inp = ioBuilder.getCircuit();
							ioBuilder.reset();

							// create wire
							SInt sum = prov.getSInt();

							// create Sequence of protocols which eventually
							// will compute the sum
							SequentialProtocolProducer sumProtocol = new SequentialProtocolProducer();

							sumProtocol.append(prov.getAddProtocol(inputs[0],
									inputs[1], sum));
							if (inputs.length > 2) {
								for (int i = 2; i < inputs.length; i++) {
									// Add sum and next secret shared input and
									// store in sum.
									sumProtocol.append(prov.getAddProtocol(sum,
											inputs[i], sum));
								}
							}

							sumProtocol.append(prov.getMultCircuit(sum, sum,
									sum));

							this.outputs = new OInt[] { ioBuilder.output(sum) };

							ProtocolProducer io = ioBuilder.getCircuit();

							ProtocolProducer gp = new SequentialProtocolProducer(
									inp, sumProtocol, io);
							return gp;
						}
					};
					sce.runApplication(app);
					int sum = 0;
					for (int i : openInputs) {
						sum += i;
					}
					sum = sum * sum;
					Assert.assertEquals(BigInteger.valueOf(sum),
							app.getOutputs()[0].getValue());
				}
			};
		};
	};

	private static void checkOutputs(int[] openInputs, OInt[] outputs) {
		for (int i = 0; i < openInputs.length; i++) {
			Assert.assertEquals(BigInteger.valueOf(openInputs[i]),
					outputs[i].getValue());
		}
	}

	private static SInt[] createInputs(NumericIOBuilder ioBuilder, int[] input,
			int targetID) {
		BigInteger[] bs = new BigInteger[input.length];
		int inx = 0;
		for (int i : input) {
			bs[inx] = BigInteger.valueOf(i);
			inx++;
		}
		return ioBuilder.inputArray(bs, targetID);
	}

	public static class TestSimpleMultAndAdd extends TestThreadFactory {

		@Override
		public TestThread next(TestThreadConfiguration conf) {
			return new ThreadWithFixture() {
				@Override
				public void test() throws Exception {
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = 701623461111107585L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							NumericIOBuilder ioBuilder = new NumericIOBuilder(
									prov);
							SInt input1 = ioBuilder.input(
									BigInteger.valueOf(10), 1);
							SInt input2 = ioBuilder.input(
									BigInteger.valueOf(5), 1);

							ProtocolProducer inputs = ioBuilder.getCircuit();
							ioBuilder.reset();
							NumericProtocolBuilder builder = new NumericProtocolBuilder(
									prov);
							SInt addAndMult = builder.mult(input1,
									builder.add(input1, input2));
							ProtocolProducer circ = builder.getCircuit();

							OInt output = ioBuilder.output(addAndMult);
							this.outputs = new OInt[] { output };
							ProtocolProducer outputs = ioBuilder.getCircuit();

							ProtocolProducer gp = new SequentialProtocolProducer(
									inputs, circ, outputs);
							return gp;
						}
					};
					sce.runApplication(app);

					Assert.assertEquals(BigInteger.valueOf(10 * (10 + 5)),
							app.getOutputs()[0].getValue());
				}
			};
		}
	}
	
	/**
	 * Test a large amount (20000) multiplication protocols in order to
	 * stress-test the protocol suite. 
	 *
	 */
	public static class TestLotsMult extends TestThreadFactory {

		@Override
		public TestThread next(TestThreadConfiguration conf) {
			
			return new ThreadWithFixture() {
				public void test() throws Exception {
					TestApplication app = new TestApplication() {
						private static final int REPS = 20000;
						private static final long serialVersionUID = 701623441111137585L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							NumericIOBuilder ioBuilder = new NumericIOBuilder(prov);
							NumericProtocolBuilder builder = new NumericProtocolBuilder(prov);
							SInt input1 = ioBuilder.input(BigInteger.valueOf(10), 1);
							SInt input2 = ioBuilder.input(BigInteger.valueOf(5), 1);
							SInt[] results = new SInt[2*(REPS/2)];
							builder.beginParScope();
							for (int i = 0; i < REPS/2; i++) {
								results[i] = builder.mult(input1, input2);
							}
							builder.endCurScope();
							builder.beginParScope();
							for (int i = 0; i < REPS/2; i++) {
								results[REPS/2 + i] = builder.mult(input1, input2);
							}
							builder.endCurScope();
							ioBuilder.addGateProducer(builder.getCircuit());
							outputs = ioBuilder.outputArray(results);
							ProtocolProducer gp = ioBuilder.getCircuit();
							return gp;
						}
					};
					sce.runApplication(app);
					OInt[] outputs = app.getOutputs();
					for (OInt o : outputs) {
						Assert.assertEquals(o.getValue(), BigInteger.valueOf(50));
					}
				}
			};
		}
	}
	
	/**
	 * Test a computation of doing a many multiplications and additions 
	 * alternating between the two. This should ensure batches with both 
	 * types of gates.
	 */
	public static class TestAlternatingMultAdd extends TestThreadFactory {

		@Override
		public TestThread next(TestThreadConfiguration conf) {
			
			return new ThreadWithFixture() {
				public void test() throws Exception {
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = 701623441111137585L;

						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							BasicNumericFactory prov = (BasicNumericFactory) provider;
							NumericIOBuilder ioBuilder = new NumericIOBuilder(prov);
							NumericProtocolBuilder builder = new NumericProtocolBuilder(prov);
							ioBuilder.beginSeqScope();
							ioBuilder.beginParScope();
							SInt input1 = ioBuilder.input(BigInteger.valueOf(10), 1);
							SInt input2 = ioBuilder.input(BigInteger.valueOf(5), 1);
							ioBuilder.endCurScope();
							builder.beginParScope();
							for (int i = 0; i < 1000; i++) {
								if (i % 2 == 0) {
									builder.mult(input1, input2);
								} else {
									builder.add(input1, input2);
								}
							}
							builder.endCurScope();
							
							ioBuilder.addGateProducer(builder.getCircuit());
							ioBuilder.endCurScope();
							ProtocolProducer gp = ioBuilder.getCircuit();
							return gp;
						}
					};
					sce.runApplication(app);					
				}
			};
		}
	}
	
	/**
	 * Test binary right shift of a shared secret.
	 */
	public static class TestRightShift extends TestThreadFactory {

		@Override
		public TestThread next(TestThreadConfiguration conf) {
			
			return new ThreadWithFixture() {
				private final BigInteger input = BigInteger.valueOf(12332157);

				public void test() throws Exception {
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = 701623441111137585L;
						
						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							
							BasicNumericFactory basicNumericFactory = (BasicNumericFactory) provider;
							PreprocessedNumericBitFactory preprocessedNumericBitFactory = (PreprocessedNumericBitFactory) provider;
							RandomAdditiveMaskFactory randomAdditiveMaskFactory = new RandomAdditiveMaskFactoryImpl(basicNumericFactory, preprocessedNumericBitFactory);
							MiscOIntGenerators miscOIntGenerators = new MiscOIntGenerators(basicNumericFactory);
							InnerProductFactory innerProductFactory = new InnerProductFactoryImpl(basicNumericFactory);
							RightShiftFactory rightShiftFactory = new RightShiftFactoryImpl(80, 
									basicNumericFactory, randomAdditiveMaskFactory, miscOIntGenerators, innerProductFactory);

							SInt result = basicNumericFactory.getSInt();
							SInt remainder = basicNumericFactory.getSInt();

							NumericIOBuilder ioBuilder = new NumericIOBuilder(basicNumericFactory);
							SequentialProtocolProducer sequentialProtocolProducer = new SequentialProtocolProducer();
							
							SInt input1 = ioBuilder.input(input, 1);
							sequentialProtocolProducer.append(ioBuilder.getCircuit());
							
							RightShiftProtocol rightShiftProtocol = rightShiftFactory.getRightShiftProtocol(input1, result, remainder);
							sequentialProtocolProducer.append(rightShiftProtocol);
							
							OInt output1 = ioBuilder.output(result);
							OInt output2 = ioBuilder.output(remainder);
							
							sequentialProtocolProducer.append(ioBuilder.getCircuit());
							
							ProtocolProducer gp = sequentialProtocolProducer;
							
							outputs = new OInt[] {output1, output2};
							
							return gp;
						}
					};
					sce.runApplication(app);
					BigInteger result = app.getOutputs()[0].getValue();
					BigInteger remainder = app.getOutputs()[1].getValue();
					System.out.println(input + " >> 1 = " + result);
					System.out.println(input + " (mod 2) = " + remainder);
					
					Assert.assertEquals(result, input.shiftRight(1));
					Assert.assertEquals(remainder, input.mod(BigInteger.valueOf(2)));
				}
			};
		}
	}
	

	public static class TestRepeatedRightShift extends TestThreadFactory {

		@Override
		public TestThread next(TestThreadConfiguration conf) {
			
			return new ThreadWithFixture() {
				private final BigInteger input = BigInteger.valueOf(12332153);
				private final int n = 7;
				
				public void test() throws Exception {
					TestApplication app = new TestApplication() {

						private static final long serialVersionUID = 701623441111137585L;
						
						@Override
						public ProtocolProducer prepareApplication(
								ProtocolFactory provider) {
							
							BasicNumericFactory basicNumericFactory = (BasicNumericFactory) provider;
							PreprocessedNumericBitFactory preprocessedNumericBitFactory = (PreprocessedNumericBitFactory) provider;
							RandomAdditiveMaskFactory randomAdditiveMaskFactory = new RandomAdditiveMaskFactoryImpl(basicNumericFactory, preprocessedNumericBitFactory);
							MiscOIntGenerators miscOIntGenerators = new MiscOIntGenerators(basicNumericFactory);
							InnerProductFactory innerProductFactory = new InnerProductFactoryImpl(basicNumericFactory);
							RightShiftFactory rightShiftFactory = new RightShiftFactoryImpl(80, 
									basicNumericFactory, randomAdditiveMaskFactory, miscOIntGenerators, innerProductFactory);

							SInt result = basicNumericFactory.getSInt();
							
							SInt[] remainders = new SInt[n];
							for (int i = 0; i < n; i++) {
								remainders[i] = basicNumericFactory.getSInt();
							}
							
							NumericIOBuilder ioBuilder = new NumericIOBuilder(basicNumericFactory);
							SequentialProtocolProducer sequentialProtocolProducer = new SequentialProtocolProducer();
							
							SInt input1 = ioBuilder.input(input, 1);
							sequentialProtocolProducer.append(ioBuilder.getCircuit());
							
							RepeatedRightShiftProtocol rightShiftProtocol = rightShiftFactory.getRepeatedRightShiftProtocol(input1, n, result, remainders);
							sequentialProtocolProducer.append(rightShiftProtocol);
							
							OInt shiftOutput = ioBuilder.output(result);
							OInt[] remainderOutput = ioBuilder.outputArray(remainders);
							sequentialProtocolProducer.append(ioBuilder.getCircuit());
							
							ProtocolProducer gp = sequentialProtocolProducer;
							
							outputs = new OInt[n+1];
							outputs[0] = shiftOutput;
							for (int i = 0; i < n; i++) { 
								outputs[i+1] = remainderOutput[i];
							}
							return gp;
						}
					};
					sce.runApplication(app);
					
					BigInteger output = app.getOutputs()[0].getValue();
					System.out.println(input + " >> " + n + " = " + output);
					Assert.assertEquals(input.shiftRight(n), output);

					BigInteger[] remainders = new BigInteger[n];
					for (int i = 0; i < n; i++) {
						remainders[i] = app.getOutputs()[i+1].getValue();
						Assert.assertEquals(input.testBit(i) ? BigInteger.ONE : BigInteger.ZERO, remainders[i]);
					}
					System.out.println(Arrays.toString(remainders));
				}
			};
		}
	}

	
}
