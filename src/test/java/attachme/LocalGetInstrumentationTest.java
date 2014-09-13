/**
 * 
 */
package attachme;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import javax.management.MBeanServer;

import org.junit.Test;

import com.heliosapm.attachme.agent.AgentInstrumentation;
import com.heliosapm.attachme.agent.AgentInstrumentationMBean;
import com.heliosapm.attachme.agent.LocalAgentInstaller;

/**
 * <p>Title: LocalGetInstrumentationTest</p>
 * <p>Description: Tests local installation of the attach agent and loading the instumentation instance</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>attachme.LocalGetInstrumentationTest</code></b>
 */

public class LocalGetInstrumentationTest extends BaseTest {
	/** The local platform MBeanServer */
	public final MBeanServer PLATFORM_MBS = ManagementFactory.getPlatformMBeanServer();
	
	/**
	 * Tests the installation of the local agent
	 * @throws Exception on any error
	 */
	@Test
	public void testLocalInstall() throws Exception {
		assertFalse(PLATFORM_MBS.isRegistered(AgentInstrumentation.AGENT_INSTR_ON));
		Instrumentation instr = LocalAgentInstaller.getInstrumentation();
		assertNotNull(instr);
		assertTrue(PLATFORM_MBS.isRegistered(AgentInstrumentation.AGENT_INSTR_ON));
		assertTrue(PLATFORM_MBS.isInstanceOf(AgentInstrumentation.AGENT_INSTR_ON, AgentInstrumentationMBean.class.getName()));
		Properties p = (Properties)PLATFORM_MBS.getAttribute(AgentInstrumentation.AGENT_INSTR_ON, "AgentProperties");
		assertNotNull(p);
		assertTrue(!p.isEmpty());
		log("Agent Props:");
		for(String key: p.stringPropertyNames()) {
			log("\t%s : %s", key, p.getProperty(key, "<null>"));
		}
	}

}
