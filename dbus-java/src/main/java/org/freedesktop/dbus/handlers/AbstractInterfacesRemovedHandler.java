package org.freedesktop.dbus.handlers;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.ObjectManager.InterfacesRemoved;


/**
* Subclass this abstract class for creating a callback for InterfaceRemoved signal provided by DBus ObjectManager.
*
* As soon as your callback is registered by calling {@link AbstractConnection#addSigHandler(Class, DBusSigHandler)},
* all property changes by Dbus will be visible in the handle(DBusSigHandler) method of your callback class.
*/
public abstract class AbstractInterfacesRemovedHandler extends AbstractSignalHandlerBase<org.freedesktop.dbus.interfaces.ObjectManager.InterfacesRemoved> {

    @Override
    public final Class<InterfacesRemoved> getImplementationClass() {        
        return org.freedesktop.dbus.interfaces.ObjectManager.InterfacesRemoved.class;
    }

    
}
