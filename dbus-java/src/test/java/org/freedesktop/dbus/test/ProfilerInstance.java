/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.test.helper.interfaces.Profiler;
import org.freedesktop.dbus.test.helper.structs.ProfileStruct;

public class ProfilerInstance implements Profiler {
    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }

    @Override
    public void array(int[] v) {
        return;
    }

    @Override
    public void stringarray(String[] v) {
        return;
    }

    @Override
    public void map(Map<String, String> m) {
        return;
    }

    @Override
    public void list(List<String> l) {
        return;
    }

    @Override
    public void bytes(byte[] b) {
        return;
    }

    @Override
    public void struct(ProfileStruct ps) {
        return;
    }

    @Override
    public void string(String s) {
        return;
    }

    @Override
    public void NoReply() {
        return;
    }

    @Override
    public void Pong() {
        return;
    }
}
