/**
 * 
 */
package com.heliosapm.attachme.jar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.heliosapm.attachme.agent.AgentInstrumentation;
import com.heliosapm.attachme.agent.AgentInstrumentationMBean;

/**
 * <p>Title: JarBuilder</p>
 * <p>Description: Fluent style dynamic jar archive builder</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>com.heliosapm.attachme.jar.JarBuilder</code></b>
 */

public class JarBuilder {
	/** The jar's manifest entries */
	private final Map<String, String> manifestEntries = new LinkedHashMap<String, String>();
	/** Additional resources to add to the jar, keyed by the jar archive path */
	private final Map<String, byte[]> resources = new LinkedHashMap<String, byte[]>();
	/** Classes to add to the jar */
	private final Set<Class<?>> jarClasses = new LinkedHashSet<Class<?>>();
	
	
	/** The platform mbean server */
	public static final MBeanServer PLATFORM_MBEANSERVER = ManagementFactory.getPlatformMBeanServer();
	
	/** A set of all primitive classes */
	@SuppressWarnings("unchecked")
	public static final Set<Class<?>> PRIMITIVES = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(
			byte.class, boolean.class, char.class, short.class, int.class, float.class, long.class, double.class  
	)));
	
	
	/**
	 * Creates and returns a new JarBuilder instance
	 * @return a new JarBuilder instance
	 */
	public static JarBuilder newInstance() {		
		return new JarBuilder();
	}
	
	/**
	 * Creates a new JarBuilder
	 */
	private JarBuilder() {
		
	}
	
	/**
	 * Writes the built jar to the passed output stream
	 * @param os The output stream to write the jar to
	 * @return the number of bytes written
	 */
	public int writeJar(final OutputStream os) {
		int totalBytes = 0;
		JarOutputStream jos = null;
		try {
			StringBuilder manifest = new StringBuilder("Manifest-Version: 1.0\n");
			for(Map.Entry<String, String> entry: manifestEntries.entrySet()) {
				manifest.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(manifest.toString().getBytes());
			Manifest mf = new Manifest(bais);
			jos = new JarOutputStream(os, mf);
			TreeMap<String, byte[]> outItems = new TreeMap<String, byte[]>(resources);
			for(Class<?> clazz : jarClasses) {
				outItems.put(clazz.getName().replace('.', '/') + ".class", getClassBytes(clazz));
			}
			
			addClassesToJar(jos, AgentInstrumentation.class, AgentInstrumentationMBean.class);
			jos.flush();
			jos.close();
			jos = null;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to write out agent jar", ex);
		} finally {
			if(jos!=null) try { jos.close(); } catch (Exception e) {/* No Op */}
		}		
		return totalBytes;
	}
	
	
	/**
	 * Appends the passed map entries to the jar's manifest declarations
	 * @param entries A map of manifest entries
	 * @return this JarBuilder
	 */
	public JarBuilder addManifestEntries(final Map<String, ? extends Object> entries) {
		if(entries!=null && !entries.isEmpty()) {
			for(Map.Entry<String, ? extends Object> entry: entries.entrySet()) {
				addManifestEntry(entry.getKey(), entry.getValue());
			}
		}
		return this;
	}
	
	/**
	 * Adds the passd key/value pair to the jar's manifest declarations
	 * @param key The manifest entry key
	 * @param value The manifest entry value
	 * @return this JarBuilder
	 */
	public <T extends Object> JarBuilder addManifestEntry(final String key, final T value) {
		if(key!=null && !key.trim().isEmpty() && value!=null) {
			String v = value.toString().trim();
			if(!v.isEmpty()) {
				manifestEntries.put(key.trim(), v);
			}
		}
		return this;
	}
	
	/**
	 * Adds the passed classes to the built jar
	 * @param classes The classes to add
	 * @return this JarBuilder
	 */
	public JarBuilder addClasses(final Class<?>...classes) {
		if(classes!=null) {
			for(Class<?> clazz: classes) {
				if(clazz!=null) {
					jarClasses.add(clazz);
				}
			}
		}
		return this;
	}
	
	/**
	 * Resolves the passed class names and adds the resolved classes to the built jar. Silently ignores:<ol>
	 * 	<li>Primitive types</li>
	 *  <li>Core classes (i.e. with package names starting with <b><code>java.</code></b></li>
	 * </ol>
	 * @param cl The optional class loader to use. If supplied, the classloader will be used to resolve the class names. Ignored if null.
	 * @param classNames The classnames to resolve
	 * @return this JarBuilder
	 * @throws ClassNotFoundException thrown if a classname cannot be resolved. If this occurs, no classes will be added.
	 */
	public JarBuilder addClasses(final ClassLoader cl, final String...classNames) throws ClassNotFoundException {
		if(classNames!=null) {
			final Set<Class<?>> classesToAdd = new LinkedHashSet<Class<?>>();
			for(String className: classNames) {
				if(className!=null && !className.trim().isEmpty()) {
					Class<?> clazz = null;
					if(cl==null) {
						clazz = Class.forName(className.trim());
					} else {
						clazz = Class.forName(className.trim(), false, cl);						
					}
					if(PRIMITIVES.contains(clazz) || clazz.getPackage().getName().startsWith("java.")) continue;
					classesToAdd.add(clazz);
				}
			}
			jarClasses.addAll(classesToAdd);
		}
		return this;
	}
	
	/**
	 * Resolves the passed class names and adds the resolved classes to the built jar. Silently ignores:<ol>
	 * 	<li>Primitive types</li>
	 *  <li>Core classes (i.e. with package names starting with <b><code>java.</code></b></li>
	 * </ol>
	 * @param mbeanServer The optional MBeanServer to resolve the classloader from. If null, will use the platform mbean server
	 * @param objectName The ObjectName to resolve a classloader to use. The resulting classloader will be used to resolve the class names.
	 * @param classNames The classnames to resolve
	 * @return this JarBuilder
	 * @throws ClassNotFoundException thrown if a classname cannot be resolved. If this occurs, no classes will be added.
	 */
	public JarBuilder addClasses(final MBeanServer mbeanServer, final ObjectName objectName, final String...classNames) throws ClassNotFoundException {
		addClasses(getClassLoader(mbeanServer, objectName), classNames);
		return this;
	}
	
	/**
	 * Resolves the passed class names and adds the resolved classes to the built jar, resolving the passed ObjectName into a ClassLoader from the platform MBeanServer.
	 * Silently ignores:<ol>
	 * 	<li>Primitive types</li>
	 *  <li>Core classes (i.e. with package names starting with <b><code>java.</code></b></li>
	 * </ol>
	 * @param objectName The ObjectName to resolve a classloader to use. The resulting classloader will be used to resolve the class names.
	 * @param classNames The classnames to resolve
	 * @return this JarBuilder
	 * @throws ClassNotFoundException thrown if a classname cannot be resolved. If this occurs, no classes will be added.
	 */
	public JarBuilder addClasses(final ObjectName objectName, final String...classNames) throws ClassNotFoundException {
		addClasses(getClassLoader(PLATFORM_MBEANSERVER, objectName), classNames);
		return this;
	}
	
	
	/**
	 * Resolves the passed class names and adds the resolved classes to the built jar. Silently ignores:<ol>
	 * 	<li>Primitive types</li>
	 *  <li>Core classes (i.e. with package names starting with <b><code>java.</code></b></li>
	 * </ol>
	 * @param classNames The classnames to resolve
	 * @return this JarBuilder
	 * @throws ClassNotFoundException thrown if a classname cannot be resolved. If this occurs, no classes will be added.
	 */
	public JarBuilder addClasses(final String...classNames) throws ClassNotFoundException {
		addClasses((ClassLoader)null, classNames);
		return this;
	}


	/**
	 * Writes the passed classes to the passed JarOutputStream
	 * @param jos the JarOutputStream
	 * @param clazzes The classes to write
	 * @throws IOException on an IOException
	 */
	protected static void addClassesToJar(JarOutputStream jos, Class<?>...clazzes) throws IOException {
		for(Class<?> clazz: clazzes) {
			jos.putNextEntry(new ZipEntry(clazz.getName().replace('.', '/') + ".class"));
			jos.write(getClassBytes(clazz));
			jos.flush();
			jos.closeEntry();
		}
	}
	
	/**
	 * Returns the bytecode bytes for the passed class
	 * @param clazz The class to get the bytecode for
	 * @return a byte array of bytecode for the passed class
	 */
	public static byte[] getClassBytes(Class<?> clazz) {
		InputStream is = null;
		try {
			is = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class");
			ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
			byte[] buffer = new byte[8092];
			int bytesRead = -1;
			while((bytesRead = is.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
			}
			baos.flush();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read class bytes for [" + clazz.getName() + "]", e);
		} finally {
			if(is!=null) { try { is.close(); } catch (Exception e) {/* No Op */} }
		}
	}

	
	
	/**
	 * Returns the classloader associated with the passed object name. If the represented MBean is a classloader, then that classloader is returned.
	 * Otherwise, the classloader for the represented MBean is returned.
	 * @param mbeanServer The optional MBeanServer to get the classloader from. If null, will use the platform MBeanServer
	 * @param objectName The ObjectName to get the associated classloader for
	 * @return the classloader
	 */
	public static ClassLoader getClassLoader(final MBeanServer mbeanServer, final ObjectName objectName) {
		final MBeanServer mbs = mbeanServer==null ? PLATFORM_MBEANSERVER : mbeanServer; 
		if(objectName==null) throw new IllegalArgumentException("The passed ObjectName was null");
		if(mbs.isRegistered(objectName)) throw new IllegalArgumentException("The passed ObjectName is not registered in the passed MBeanServer");
		try {
			if(mbs.isInstanceOf(objectName, "java.lang.ClassLoader")) {
				return mbs.getClassLoader(objectName);
			}
			return mbs.getClassLoaderFor(objectName);			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get ClassLoader from ObjectName [" + objectName + "]", ex);
		}
	}
	
}
