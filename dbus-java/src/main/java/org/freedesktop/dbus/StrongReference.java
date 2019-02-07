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

import java.lang.ref.WeakReference;

/**
 * An alternative to a WeakReference when you don't want
 * that behaviour.
 */
public class StrongReference<T> extends WeakReference<T> {
    private T referant;

    public StrongReference(T _referant) {
        super(_referant);
        this.referant = _referant;
    }

    @Override
    public void clear() {
        referant = null;
    }

    @Override
    public boolean enqueue() {
        return false;
    }

    @Override
    public T get() {
        return referant;
    }

    @Override
    public boolean isEnqueued() {
        return false;
    }
}
