/**
 * 
 */
package attachme;

import java.lang.management.ManagementFactory;
import java.util.Random;

import javax.management.ObjectName;
import javax.management.Query;
import javax.management.StringValueExp;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.heliosapm.attachme.agent.AgentInstrumentation;
import com.heliosapm.attachme.agent.AgentInstrumentationMBean;

/**
 * <p>Title: BaseTest</p>
 * <p>Description: </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>attachme.BaseTest</code></b>
 */
@Ignore
public class BaseTest {

	/** The currently executing test name */
	@Rule public final TestName name = new TestName();

	/**
	 * Default set up before each test class
	 * @throws java.lang.Exception thrown on any error
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		/* No Op */
	}

	/**
	 * Default tear down after each test class
	 * @throws java.lang.Exception thrown on any error
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		for(final ObjectName on: ManagementFactory.getPlatformMBeanServer().queryNames(AgentInstrumentation.AGENT_INSTR_ON, Query.isInstanceOf(new StringValueExp(AgentInstrumentationMBean.class.getName())))) {
			ManagementFactory.getPlatformMBeanServer().unregisterMBean(on);
			log("Unregistered [%s]", on);
		}
		// com.heliosapm.attachme.agent.AgentInstrumentationMBean
	}

	/**
	 * Default set up before each test method
	 * @throws java.lang.Exception thrown on any error
	 */
	@Before
	public void setUp() throws Exception {
		/* No Op */
	}

	/**
	 * Default tear down after each test method
	 * @throws java.lang.Exception thrown on any error
	 */
	@After
	public void tearDown() throws Exception {
		/* No Op */
	}
	
	/** A random value generator */
	protected static final Random RANDOM = new Random(System.currentTimeMillis());
	
	/**
	 * Returns a random positive long
	 * @return a random positive long
	 */
	protected static long nextPosLong() {
		return Math.abs(RANDOM.nextLong());
	}
	
	/**
	 * Returns a random positive int
	 * @return a random positive int
	 */
	protected static int nextPosInt() {
		return Math.abs(RANDOM.nextInt());
	}
	
	/**
	 * Returns a random positive int within the bound
	 * @param bound the bound on the random number to be returned. Must be positive. 
	 * @return a random positive int
	 */
	protected static int nextPosInt(int bound) {
		return Math.abs(RANDOM.nextInt(bound));
	}
	
	
	
	/**
	 * Prints the test name about to be executed
	 */
	@Before
	public void printTestName() {
		log("\n\t==================================\n\tRunning Test [" + name.getMethodName() + "]\n\t==================================\n");
	}
	
	/**
	 * Out printer
	 * @param fmt the message format
	 * @param args the message values
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
	/**
	 * Err printer
	 * @param fmt the message format
	 * @param args the message values
	 */
	public static void loge(String fmt, Object...args) {
		System.err.print(String.format(fmt, args));
		if(args!=null && args.length>0 && args[0] instanceof Throwable) {
			System.err.println("  Stack trace follows:");
			((Throwable)args[0]).printStackTrace(System.err);
		} else {
			System.err.println("");
		}
	}

}
