// ========================================================================
// Copyright (c) 2011 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses.
// ========================================================================

package org.eclipse.jetty.servlet.listener;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.util.log.Log;

/**
 * ELContextCleaner
 *
 * Clean up BeanELResolver when the context is going out
 * of service:
 * 
 * See http://java.net/jira/browse/GLASSFISH-1649
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=353095
 */
public class ELContextCleaner implements ServletContextListener
{

    public void contextInitialized(ServletContextEvent sce)
    {
    }

    public void contextDestroyed(ServletContextEvent sce)
    {
        try
        {
            //Check that the BeanELResolver class is on the classpath
            Class beanELResolver = Loader.loadClass(this.getClass(), "javax.el.BeanELResolver");

            //Get a reference via reflection to the properties field which is holding class references
            Field field = getField(beanELResolver);

            //Get rid of references
            purgeEntries(field);
            
            Log.info("javax.el.BeanELResolver purged");
        }
        
        catch (ClassNotFoundException e)
        {
            //BeanELResolver not on classpath, ignore
        }
        catch (SecurityException e)
        {
            Log.warn("Cannot purge classes from javax.el.BeanELResolver", e);
        }
        catch (IllegalArgumentException e)
        {
            Log.warn("Cannot purge classes from javax.el.BeanELResolver", e);
        }
        catch (IllegalAccessException e)
        {
            Log.warn("Cannot purge classes from javax.el.BeanELResolver", e);
        }
        catch (NoSuchFieldException e)
        {
            Log.warn("Cannot purge classes from javax.el.BeanELResolver", e);
        }
       
    }


    protected Field getField (Class beanELResolver) 
    throws SecurityException, NoSuchFieldException
    {
        if (beanELResolver == null)
            return  null;

        return beanELResolver.getDeclaredField("properties");
    }

    protected void purgeEntries (Field properties) 
    throws IllegalArgumentException, IllegalAccessException
    {
        if (properties == null)
            return;

        if (!properties.isAccessible())
            properties.setAccessible(true);

        ConcurrentHashMap map = (ConcurrentHashMap) properties.get(null);
        if (map == null)
            return;
        
        Iterator<Class> itor = map.keySet().iterator();
        while (itor.hasNext()) 
        {
            Class clazz = itor.next();
            Log.info("Clazz: "+clazz+" loaded by "+clazz.getClassLoader());
            if (Thread.currentThread().getContextClassLoader().equals(clazz.getClassLoader()))
            {
                itor.remove();  
                Log.info("removed");
            }
            else
                Log.info("not removed: "+"contextclassloader="+Thread.currentThread().getContextClassLoader()+"clazz's classloader="+clazz.getClassLoader());
        }
    }
}