package jimmix.proxy;

import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.Attribute;

import org.jboss.console.remote.RemoteMBeanAttributeInvocation;
import org.jboss.console.remote.RemoteMBeanInvocation;
import org.jboss.invocation.http.interfaces.HttpInvokerProxy;
import org.jboss.invocation.Invoker;
import org.jboss.proxy.GenericProxyFactory;

import jimmix.util.*;

public class JMXInvokerServletProxy implements Proxy {

    private MBeanServerConnection mbeanServer;

    public JMXInvokerServletProxy(String url) throws Exception {
        Object cacheID = null;
        ObjectName targetName = new ObjectName("jboss.jmx:type=adaptor,name=Invoker");
        Invoker invoker = new HttpInvokerProxy(url);
        String jndiName = null;
        String proxyBindingName = null;

        // Building the interceptors list
        // These interceptors will be executed from the client side
        ArrayList interceptorClasses = new ArrayList();
        interceptorClasses.add(Class.forName("org.jboss.proxy.ClientMethodInterceptor"));
        interceptorClasses.add(Class.forName("org.jboss.proxy.SecurityInterceptor"));
        interceptorClasses.add(Class.forName("org.jboss.jmx.connector.invoker.client.InvokerAdaptorClientInterceptor"));
        interceptorClasses.add(Class.forName("org.jboss.invocation.InvokerInterceptor"));

        // Getting the current classloader
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Building the exported interfaces list
        // The final proxy will implement these interfaces
        Class[] interfaces = new Class[]{
                Class.forName("org.jboss.jmx.adaptor.rmi.RMIAdaptorExt")
        };

        GenericProxyFactory proxyFactory = new GenericProxyFactory();
        MBeanServerConnection server = (MBeanServerConnection) proxyFactory.createProxy(
                cacheID,
                targetName,
                invoker,
                jndiName,
                proxyBindingName,
                interceptorClasses,
                classLoader,
                interfaces
        );

        this.mbeanServer = server;
        ;
    }

    public String invoke(ObjectName name, String actionName, Object[] params, String[] signature) throws Exception {
        String resultToString = "";
        Object result = mbeanServer.invoke(name, actionName, params, signature);
        if (result != null) {
            resultToString = TypeConvertor.convertObjectToString(result, TypeConvertor.getObjectClass(result));
        }
        return resultToString;
    }

    public String get(ObjectName name, String attribute) throws Exception {
        String resultToString = "";
        Object result = mbeanServer.getAttribute(name, attribute);
        if (result != null) {
            resultToString = TypeConvertor.convertObjectToString(result, TypeConvertor.getObjectClass(result));
        }
        return resultToString;
    }

    public void set(ObjectName name, String attribute, String value) throws Exception {
        mbeanServer.setAttribute(name, new Attribute(attribute, value));
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws Exception {
        return mbeanServer.createMBean(className, name, loaderName, params, signature);
    }

    public void unregisterMBean(ObjectName name) throws Exception {
        mbeanServer.unregisterMBean(name);
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws Exception {
        return mbeanServer.queryNames(name, query);
    }

    public void mlet(ObjectName objectName, String serverUrl) throws Exception {
        ObjectInstance bean;
        try {
            bean = mbeanServer.getObjectInstance(objectName);
            Log.log("mbean " + objectName + " already exists");
        } catch (Exception e) {
            // bean does not exist, try and create bean...
            Log.log("retrieving MLet instance");
            ObjectInstance mlet;
            try {
                mlet = mbeanServer.createMBean("javax.management.loading.MLet", null);
            } catch (javax.management.InstanceAlreadyExistsException iaee) {
                mlet = mbeanServer.getObjectInstance(new ObjectName(mbeanServer.getDefaultDomain() + ":type=MLet"));
            }

            Log.log("invoking MLet.getMBeansFromURL");
            Object result = mbeanServer.invoke(mlet.getObjectName(), "getMBeansFromURL", new Object[]{serverUrl}, new String[]{String.class.getName()});
            HashSet resultSet = ((HashSet) result);
            Iterator itr = resultSet.iterator();
            Object nextObject = itr.next();
            if (nextObject instanceof Exception) {
                throw ((Exception) nextObject);
            }
            bean = ((ObjectInstance) nextObject);
            Log.log("mbean " + objectName + " has been created");
        }
    }
}
