package org.neo4j.extension.firehose;

import org.neo4j.helpers.Service;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import static org.neo4j.helpers.Settings.*;

@Service.Implementation(KernelExtensionFactory.class)
public class AuthenticatorKernelExtensionFactory extends KernelExtensionFactory<AuthenticatorKernelExtensionFactory.Dependencies> {

    public interface Dependencies {
        Config getConfig();
    }

    public AuthenticatorKernelExtensionFactory() {
        super("authenticator");
    }

    @Override
    public Lifecycle newInstance(KernelContext context, Dependencies dependencies) throws Throwable {
        return new LifecycleAdapter() {

            @Override
            public void init() throws Throwable {
                final String username = dependencies.getConfig().get(setting( "extension.csv.http.username", STRING, (String)null ));
                final String password = dependencies.getConfig().get(setting( "extension.csv.http.password", STRING, (String)null ));

                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestorType().equals(RequestorType.SERVER) &&
                                (getRequestingProtocol().equals("http") || getRequestingProtocol().equals("https")) &&
                                getRequestingHost().equals("localhost")) {
                            return new PasswordAuthentication(username, password.toCharArray());
                        } else {
                            return null;
                        }
                    }
                });
            }
        };
    }
}