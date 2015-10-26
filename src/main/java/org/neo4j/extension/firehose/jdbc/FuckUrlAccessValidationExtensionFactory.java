package org.neo4j.extension.firehose.jdbc;

import org.neo4j.helpers.Service;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.impl.factory.PlatformModule;
import org.neo4j.kernel.impl.security.URLAccessRules;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;

import java.lang.reflect.Field;

/**
 * In Neo4j 2.3 a new "feature" called URLAccessRules has been added. The idea is to sandbox accesses
 * to file:// URLs for LOAD CSV. However the implementation is not extensible: it allows any http, https and ftp.
 * file access depends on a config option. NO OTHER URL TYPE IS PERMITTED OR CAN BE CONFIGURED!
 * Using some reflection hacking we can fix that - this is the purpose of this class.
 */
@Service.Implementation(KernelExtensionFactory.class)
public class FuckUrlAccessValidationExtensionFactory extends KernelExtensionFactory<FuckUrlAccessValidationExtensionFactory.Dependencies> {

    public interface Dependencies {
        GraphDatabaseFacade getGraphDatabaseFacade();
    }

    public FuckUrlAccessValidationExtensionFactory() {
        super("fuckUrlAccessValidationExtension");
    }

    @Override
    public Lifecycle newInstance(KernelContext context, Dependencies dependencies) throws Throwable {
        return new LifecycleAdapter() {
            @Override
            public void init() throws Throwable {
                GraphDatabaseFacade facade = dependencies.getGraphDatabaseFacade();
                PlatformModule platformModule = facade.platformModule;
                Field field = platformModule.getClass().getField("urlAccessRule");
                field.setAccessible(true);
                field.set(platformModule, URLAccessRules.alwaysPermitted());
            }
        };
    }
}