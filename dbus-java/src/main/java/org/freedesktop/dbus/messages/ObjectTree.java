/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.messages;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of the exported objects for introspection data */
public class ObjectTree {

    private Logger logger = LoggerFactory.getLogger(getClass());

    class TreeNode {
        // CHECKSTYLE:OFF
        String         name;
        ExportedObject object;
        String         data;
        TreeNode       right;
        TreeNode       down;
        // CHECKSTYLE:ON

        TreeNode(String _name) {
            this.name = _name;
        }

        TreeNode(String _name, ExportedObject _object, String _data) {
            this.name = _name;
            this.object = _object;
            this.data = _data;
        }
    }

    private TreeNode root;

    public ObjectTree() {
        root = new TreeNode("");
    }

    public static final Pattern SLASH_PATTERN = Pattern.compile("/");

    private TreeNode recursiveFind(TreeNode current, String path) {
        if ("/".equals(path)) {
            return current;
        }
        String[] elements = path.split("/", 2);
        // this is us or a parent node
        if (path.startsWith(current.name)) {
            // this is us
            if (path.equals(current.name)) {
                return current;
            }
            // recurse down
            else {
                if (current.down == null) {
                    return null;
                } else {
                    return recursiveFind(current.down, elements[1]);
                }
            }
        } else if (current.right == null) {
            return null;
        } else if (0 > current.right.name.compareTo(elements[0])) {
            return null;
        }
        // recurse right
        else {
            return recursiveFind(current.right, path);
        }
    }

    private TreeNode recursiveAdd(TreeNode current, String path, ExportedObject object, String data) {
        String[] elements = SLASH_PATTERN.split(path, 2);
        // this is us or a parent node
        if (path.startsWith(current.name)) {
            // this is us
            if (1 == elements.length || "".equals(elements[1])) {
                current.object = object;
                current.data = data;
            }
            // recurse down
            else {
                if (current.down == null) {
                    String[] el = elements[1].split("/", 2);
                    current.down = new TreeNode(el[0]);
                }
                current.down = recursiveAdd(current.down, elements[1], object, data);
            }
        }
        // need to create a new sub-tree on the end
        else if (current.right == null) {
            current.right = new TreeNode(elements[0]);
            current.right = recursiveAdd(current.right, path, object, data);
        }
        // need to insert here
        else if (0 > current.right.name.compareTo(elements[0])) {
            TreeNode t = new TreeNode(elements[0]);
            t.right = current.right;
            current.right = t;
            current.right = recursiveAdd(current.right, path, object, data);
        }
        // recurse right
        else {
            current.right = recursiveAdd(current.right, path, object, data);
        }
        return current;
    }

    public synchronized void add(String path, ExportedObject object, String data) {
        logger.debug("Adding {} to object tree", path);
        root = recursiveAdd(root, path, object, data);
    }

    public synchronized void remove(String path) {
        logger.debug("Removing {} from object tree", path);
        TreeNode t = recursiveFind(root, path);
        t.object = null;
        t.data = null;
    }

    // CHECKSTYLE:OFF
    public String Introspect(String _path) {
    // CHECKSTYLE:ON
        TreeNode t = recursiveFind(root, _path);
        if (null == t) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<node name=\"");
        sb.append(_path);
        sb.append("\">\n");
        if (null != t.data) {
            sb.append(t.data);
        }
        t = t.down;
        while (null != t) {
            sb.append("<node name=\"");
            sb.append(t.name);
            sb.append("\"/>\n");
            t = t.right;
        }
        sb.append("</node>");
        return sb.toString();
    }

    private String recursivePrint(TreeNode current) {
        String s = "";
        if (null != current) {
            s += current.name;
            if (null != current.object) {
                s += "*";
            }
            if (null != current.down) {
                s += "/{" + recursivePrint(current.down) + "}";
            }
            if (null != current.right) {
                s += ", " + recursivePrint(current.right);
            }
        }
        return s;
    }

    @Override
    public String toString() {
        return recursivePrint(root);
    }
}
