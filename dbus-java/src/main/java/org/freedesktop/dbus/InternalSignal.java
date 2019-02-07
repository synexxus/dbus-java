/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.DBusSignal;

public class InternalSignal extends DBusSignal {
    public InternalSignal(String _source, String _objectpath, String _name, String _iface, String _sig, long _serial, Object... _parameters) throws DBusException {
        super(_source, _objectpath, _iface, _name, _sig, _parameters, _serial);
    }
}
