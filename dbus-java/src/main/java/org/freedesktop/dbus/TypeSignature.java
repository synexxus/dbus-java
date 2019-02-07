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

import java.lang.reflect.Type;

import org.freedesktop.dbus.exceptions.DBusException;

public class TypeSignature {
    // CHECKSTYLE:OFF
    String sig;
    // CHECKSTYLE:ON
    public TypeSignature(String _sig) {
        this.sig = _sig;
    }

    public TypeSignature(Type[] types) throws DBusException {
        StringBuffer sb = new StringBuffer();
        for (Type t : types) {
            String[] ts = Marshalling.getDBusType(t);
            for (String s : ts) {
                sb.append(s);
            }
        }
        this.sig = sb.toString();
    }

    public String getSig() {
        return sig;
    }
}
