/*
 * ome.services.hooks.StartupHook
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.hooks;

// Java imports

// Third-party libraries
import org.hibernate.SessionFactory;
import org.jboss.annotation.ejb.Service;
import org.jboss.annotation.ejb.Management;

// Application-internal dependencies
import ome.annotations.RevisionDate;
import ome.annotations.RevisionNumber;
import ome.system.OmeroContext;

/**
 * Hook run after all the application has been deployed to the server. At that
 * point, it can be guaranteed that the Omero classes are available and so
 * attempting to connect to the database "internally" should work.
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision$, $Date$
 * @since 3.0-Beta1
 * @see <a href="https://trac.openmicroscopy.org.uk/omero/ticket/444">ticket:444</a>
 */
@RevisionDate("$Date$")
@RevisionNumber("$Revision$")
@Service(objectName = "omero:service=StartupHook")
@Management(Startup.class)
public class StartupHook implements Startup {

    OmeroContext ctx = OmeroContext.getManagedServerContext();

    SessionFactory sf = (SessionFactory) ctx.getBean("sessionFactory");

    /**
     * Attempts twice to connect to the server to overcome any initial
     * difficulties.
     * 
     * @see <a
     *      href="https://trac.openmicroscopy.org.uk/omero/ticket/444">ticket:444</a>
     */
    public void start() throws Exception {
        System.out.println("Starting Omero...");

        try {
            connect();
        } catch (Exception e) {
            // ok
        }

        connect();
        System.out.println("Ready.");
    }

    /**
     * Attempts a simple database query.
     */
    protected void connect() {
        sf.openSession().createQuery("select count(*) from Experimenter");
    }

}
