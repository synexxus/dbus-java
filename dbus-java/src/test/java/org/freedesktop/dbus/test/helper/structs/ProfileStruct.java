/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.helper.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;

//CHECKSTYLE:OFF
public final class ProfileStruct extends Struct {
    @Position(0)
    private final String first;
    @Position(1)
    private final UInt32 second;
    @Position(2)
    private final long   third;

    public ProfileStruct(String _a, UInt32 _b, long _c) {
        this.first = _a;
        this.second = _b;
        this.third = _c;
    }

    public String getFirst() {
        return first;
    }

    public UInt32 getSecond() {
        return second;
    }

    public long getThird() {
        return third;
    }
}
//CHECKSTYLE:ON
