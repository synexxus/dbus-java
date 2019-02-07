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

import java.io.FileDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSerializable;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.DBusMapType;
import org.freedesktop.dbus.types.DBusStructType;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.UInt64;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains static methods for marshalling values.
 */
public final class Marshalling {
    private static final Logger LOGGER = LoggerFactory.getLogger(Marshalling.class);

    private static final Map<Type, String[]> TYPE_CACHE = new HashMap<>();

    private static final Map<Class<?>, Byte> CLASS_TO_ARGUMENTTYPE = new LinkedHashMap<>();
    static {
        CLASS_TO_ARGUMENTTYPE.put(Boolean.class, Message.ArgumentType.BOOLEAN); // class
        CLASS_TO_ARGUMENTTYPE.put(Boolean.TYPE, Message.ArgumentType.BOOLEAN); // primitive type

        CLASS_TO_ARGUMENTTYPE.put(Byte.class, Message.ArgumentType.BYTE);
        CLASS_TO_ARGUMENTTYPE.put(Byte.TYPE, Message.ArgumentType.BYTE);

        CLASS_TO_ARGUMENTTYPE.put(Short.class, Message.ArgumentType.INT16);
        CLASS_TO_ARGUMENTTYPE.put(Short.TYPE, Message.ArgumentType.INT16);

        CLASS_TO_ARGUMENTTYPE.put(Integer.class, Message.ArgumentType.INT32);
        CLASS_TO_ARGUMENTTYPE.put(Integer.TYPE, Message.ArgumentType.INT32);

        CLASS_TO_ARGUMENTTYPE.put(Long.class, Message.ArgumentType.INT64);
        CLASS_TO_ARGUMENTTYPE.put(Long.TYPE, Message.ArgumentType.INT64);

        CLASS_TO_ARGUMENTTYPE.put(Double.class, Message.ArgumentType.DOUBLE);
        CLASS_TO_ARGUMENTTYPE.put(Double.TYPE, Message.ArgumentType.DOUBLE);

        if (AbstractConnection.FLOAT_SUPPORT) {
            CLASS_TO_ARGUMENTTYPE.put(Float.class, Message.ArgumentType.FLOAT);
            CLASS_TO_ARGUMENTTYPE.put(Float.TYPE, Message.ArgumentType.FLOAT);
        } else {
            CLASS_TO_ARGUMENTTYPE.put(Float.class, Message.ArgumentType.DOUBLE);
            CLASS_TO_ARGUMENTTYPE.put(Float.TYPE, Message.ArgumentType.DOUBLE);
        }

        CLASS_TO_ARGUMENTTYPE.put(UInt16.class, Message.ArgumentType.UINT16);
        CLASS_TO_ARGUMENTTYPE.put(UInt32.class, Message.ArgumentType.UINT32);
        CLASS_TO_ARGUMENTTYPE.put(UInt64.class, Message.ArgumentType.UINT64);

        CLASS_TO_ARGUMENTTYPE.put(CharSequence.class, Message.ArgumentType.STRING);
        CLASS_TO_ARGUMENTTYPE.put(Variant.class, Message.ArgumentType.VARIANT);

        CLASS_TO_ARGUMENTTYPE.put(FileDescriptor.class, Message.ArgumentType.FILEDESCRIPTOR);

        CLASS_TO_ARGUMENTTYPE.put(DBusInterface.class, Message.ArgumentType.OBJECT_PATH);
        CLASS_TO_ARGUMENTTYPE.put(DBusPath.class, Message.ArgumentType.OBJECT_PATH);
        CLASS_TO_ARGUMENTTYPE.put(ObjectPath.class, Message.ArgumentType.OBJECT_PATH);
    }

    private Marshalling() {
    }

    /**
    * Will return the DBus type corresponding to the given Java type.
    * Note, container type should have their ParameterizedType not their
    * Class passed in here.
    * @param _javaType The Java types.
    * @return The DBus types.
    * @throws DBusException If the given type cannot be converted to a DBus type.
    */
    public static String getDBusType(Type[] _javaType) throws DBusException {
        StringBuffer sb = new StringBuffer();
        for (Type t : _javaType) {
            for (String s : getDBusType(t)) {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    /**
    * Will return the DBus type corresponding to the given Java type.
    * Note, container type should have their ParameterizedType not their
    * Class passed in here.
    * @param _javaType The Java type.
    * @return The DBus type.
    * @throws DBusException If the given type cannot be converted to a DBus type.
    */
    public static String[] getDBusType(Type _javaType) throws DBusException {
        String[] cached = TYPE_CACHE.get(_javaType);
        if (null != cached) {
            return cached;
        }
        cached = getDBusType(_javaType, false);
        TYPE_CACHE.put(_javaType, cached);
        return cached;
    }

    /**
    * Will return the DBus type corresponding to the given Java type.
    * Note, container type should have their ParameterizedType not their
    * Class passed in here.
    * @param _dataType The Java type.
    * @param _basic If true enforces this to be a non-compound type. (compound types are Maps, Structs and Lists/arrays).
    * @return The DBus type.
    * @throws DBusException If the given type cannot be converted to a DBus type.
    */
    public static String[] getDBusType(Type _dataType, boolean _basic) throws DBusException {
        return recursiveGetDBusType(new StringBuffer[10], _dataType, _basic, 0);
    }

    private static String[] recursiveGetDBusType(StringBuffer[] _out, Type _dataType, boolean _basic, int _level) throws DBusException {
        if (_out.length <= _level) {
            StringBuffer[] newout = new StringBuffer[_out.length];
            System.arraycopy(_out, 0, newout, 0, _out.length);
            _out = newout;
        }
        if (null == _out[_level]) {
            _out[_level] = new StringBuffer();
        } else {
            _out[_level].delete(0, _out[_level].length());
        }

        if (_basic && !(_dataType instanceof Class<?>)) {
            throw new DBusException(_dataType + " is not a basic type");
        }

        if (_dataType instanceof TypeVariable) {
            _out[_level].append((char) Message.ArgumentType.VARIANT);
        } else if (_dataType instanceof GenericArrayType) {
            _out[_level].append((char) Message.ArgumentType.ARRAY);
            String[] s = recursiveGetDBusType(_out, ((GenericArrayType) _dataType).getGenericComponentType(), false, _level + 1);
            if (s.length != 1) {
                throw new DBusException("Multi-valued array types not permitted");
            }
            _out[_level].append(s[0]);
        } else if ((_dataType instanceof Class<?> && DBusSerializable.class.isAssignableFrom((Class<?>) _dataType)) || (_dataType instanceof ParameterizedType && DBusSerializable.class.isAssignableFrom((Class<?>) ((ParameterizedType) _dataType).getRawType()))) {
            // it's a custom serializable type
            Type[] newtypes = null;
            if (_dataType instanceof Class) {
                for (Method m : ((Class<?>) _dataType).getDeclaredMethods()) {
                    if (m.getName().equals("deserialize")) {
                        newtypes = m.getGenericParameterTypes();
                    }
                }
            } else {
                for (Method m : ((Class<?>) ((ParameterizedType) _dataType).getRawType()).getDeclaredMethods()) {
                    if (m.getName().equals("deserialize")) {
                        newtypes = m.getGenericParameterTypes();
                    }
                }
            }

            if (null == newtypes) {
                throw new DBusException("Serializable classes must implement a deserialize method");
            }

            String[] sigs = new String[newtypes.length];
            for (int j = 0; j < sigs.length; j++) {
                String[] ss = recursiveGetDBusType(_out, newtypes[j], false, _level + 1);
                if (1 != ss.length) {
                    throw new DBusException("Serializable classes must serialize to native DBus types");
                }
                sigs[j] = ss[0];
            }
            return sigs;
        } else if (_dataType instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) _dataType;
            if (p.getRawType().equals(Map.class)) {
                _out[_level].append("a{");
                Type[] t = p.getActualTypeArguments();
                try {
                    String[] s = recursiveGetDBusType(_out, t[0], true, _level + 1);
                    if (s.length != 1) {
                        throw new DBusException("Multi-valued array types not permitted");
                    }
                    _out[_level].append(s[0]);
                    s = recursiveGetDBusType(_out, t[1], false, _level + 1);
                    if (s.length != 1) {
                        throw new DBusException("Multi-valued array types not permitted");
                    }
                    _out[_level].append(s[0]);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                    LOGGER.debug("", aioobe);
                    throw new DBusException("Map must have 2 parameters");
                }
                _out[_level].append('}');
            } else if (List.class.isAssignableFrom((Class<?>) p.getRawType())) {
                for (Type t : p.getActualTypeArguments()) {
                    if (Type.class.equals(t)) {
                        _out[_level].append((char) Message.ArgumentType.SIGNATURE);
                    } else {
                        String[] s = recursiveGetDBusType(_out, t, false, _level + 1);
                        if (s.length != 1) {
                            throw new DBusException("Multi-valued array types not permitted");
                        }
                        _out[_level].append((char) Message.ArgumentType.ARRAY);
                        _out[_level].append(s[0]);
                    }
                }
            } else if (p.getRawType().equals(Variant.class)) {
                _out[_level].append((char) Message.ArgumentType.VARIANT);
            } else if (DBusInterface.class.isAssignableFrom((Class<?>) p.getRawType())) {
                _out[_level].append((char) Message.ArgumentType.OBJECT_PATH);
            } else if (Tuple.class.isAssignableFrom((Class<?>) p.getRawType())) {
                Type[] ts = p.getActualTypeArguments();
                List<String> vs = new ArrayList<>();
                for (Type t : ts) {
                    for (String s : recursiveGetDBusType(_out, t, false, _level + 1)) {
                        vs.add(s);
                    }
                }
                return vs.toArray(new String[0]);
            } else {
                throw new DBusException("Exporting non-exportable parameterized type " + _dataType);
            }
        } else if (_dataType instanceof Class<?>) {
            Class<?> dataTypeClazz = (Class<?>) _dataType;

            if (dataTypeClazz.isArray()) {
                if (Type.class.equals(((Class<?>) _dataType).getComponentType())) {
                    _out[_level].append((char) Message.ArgumentType.SIGNATURE);
                } else {
                    _out[_level].append((char) Message.ArgumentType.ARRAY);
                    String[] s = recursiveGetDBusType(_out, ((Class<?>) _dataType).getComponentType(), false, _level + 1);
                    if (s.length != 1) {
                        throw new DBusException("Multi-valued array types not permitted");
                    }
                    _out[_level].append(s[0]);
                }
            } else if (Struct.class.isAssignableFrom((Class<?>) _dataType)) {
                _out[_level].append((char) Message.ArgumentType.STRUCT1);
                Type[] ts = Container.getTypeCache(_dataType);
                if (null == ts) {
                    Field[] fs = ((Class<?>) _dataType).getDeclaredFields();
                    ts = new Type[fs.length];
                    for (Field f : fs) {
                        Position p = f.getAnnotation(Position.class);
                        if (null == p) {
                            continue;
                        }
                        ts[p.value()] = f.getGenericType();
                    }
                    Container.putTypeCache(_dataType, ts);
                }

                for (Type t : ts) {
                    if (t != null) {
                        for (String s : recursiveGetDBusType(_out, t, false, _level + 1)) {
                            _out[_level].append(s);
                        }
                    }
                }
                _out[_level].append(')');

            } else {
                boolean found = false;

                for (Entry<Class<?>, Byte> entry : CLASS_TO_ARGUMENTTYPE.entrySet()) {
                    if (entry.getKey().isAssignableFrom(dataTypeClazz)) {
                        _out[_level].append((char) entry.getValue().byteValue());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new DBusException("Exporting non-exportable type: " + _dataType);
                }
            }
        }

        LOGGER.trace("Converted Java type: {} to D-Bus Type: {}", _dataType, _out[_level]);

        return new String[] {
                _out[_level].toString()
        };
    }

    /**
    * Converts a dbus type string into Java Type objects,
    * @param _dbusType The DBus type or types.
    * @param _resultValue List to return the types in.
    * @param _limit Maximum number of types to parse (-1 == nolimit).
    * @return number of characters parsed from the type string.
    * @throws DBusException on error
    */
    public static int getJavaType(String _dbusType, List<Type> _resultValue, int _limit) throws DBusException {
        if (null == _dbusType || "".equals(_dbusType) || 0 == _limit) {
            return 0;
        }

        try {
            int idx = 0;
            for (; idx < _dbusType.length() && (-1 == _limit || _limit > _resultValue.size()); idx++) {
                switch (_dbusType.charAt(idx)) {
                case Message.ArgumentType.STRUCT1:
                    int structIdx = idx + 1;
                    for (int structLen = 1; structLen > 0; structIdx++) {
                        if (Message.ArgumentType.STRUCT2 == _dbusType.charAt(structIdx)) {
                            structLen--;
                        } else if (Message.ArgumentType.STRUCT1 == _dbusType.charAt(structIdx)) {
                            structLen++;
                        }
                    }

                    List<Type> contained = new ArrayList<>();
                    int javaType = getJavaType(_dbusType.substring(idx + 1, structIdx - 1), contained, -1);
                    _resultValue.add(new DBusStructType(contained.toArray(new Type[0])));
                    idx = structIdx - 1; //-1 because j already points to the next signature char
                    break;
                case Message.ArgumentType.ARRAY:
                    if (Message.ArgumentType.DICT_ENTRY1 == _dbusType.charAt(idx + 1)) {
                        contained = new ArrayList<>();
                        javaType = getJavaType(_dbusType.substring(idx + 2), contained, 2);
                        _resultValue.add(new DBusMapType(contained.get(0), contained.get(1)));
                        idx += (javaType + 2);
                    } else {
                        contained = new ArrayList<>();
                        javaType = getJavaType(_dbusType.substring(idx + 1), contained, 1);
                        _resultValue.add(new DBusListType(contained.get(0)));
                        idx += javaType;
                    }
                    break;
                case Message.ArgumentType.VARIANT:
                    _resultValue.add(Variant.class);
                    break;
                case Message.ArgumentType.BOOLEAN:
                    _resultValue.add(Boolean.class);
                    break;
                case Message.ArgumentType.INT16:
                    _resultValue.add(Short.class);
                    break;
                case Message.ArgumentType.BYTE:
                    _resultValue.add(Byte.class);
                    break;
                case Message.ArgumentType.OBJECT_PATH:
                    _resultValue.add(DBusInterface.class);
                    break;
                case Message.ArgumentType.UINT16:
                    _resultValue.add(UInt16.class);
                    break;
                case Message.ArgumentType.INT32:
                    _resultValue.add(Integer.class);
                    break;
                case Message.ArgumentType.UINT32:
                    _resultValue.add(UInt32.class);
                    break;
                case Message.ArgumentType.INT64:
                    _resultValue.add(Long.class);
                    break;
                case Message.ArgumentType.UINT64:
                    _resultValue.add(UInt64.class);
                    break;
                case Message.ArgumentType.DOUBLE:
                    _resultValue.add(Double.class);
                    break;
                case Message.ArgumentType.FLOAT:
                    _resultValue.add(Float.class);
                    break;
                case Message.ArgumentType.STRING:
                    _resultValue.add(CharSequence.class);
                    break;
                case Message.ArgumentType.FILEDESCRIPTOR:
                    _resultValue.add(FileDescriptor.class);
                    break;
                case Message.ArgumentType.SIGNATURE:
                    _resultValue.add(Type[].class);
                    break;
                case Message.ArgumentType.DICT_ENTRY1:
                    _resultValue.add(Map.Entry.class);
                    contained = new ArrayList<>();
                    javaType = getJavaType(_dbusType.substring(idx + 1), contained, 2);
                    idx += javaType + 1;
                    break;
                default:
                    throw new DBusException(String.format("Failed to parse DBus type signature: %s (%s).", _dbusType, _dbusType.charAt(idx)));
                }
            }
            return idx;
        } catch (IndexOutOfBoundsException _ex) {
            LOGGER.debug("Failed to parse DBus type signature.", _ex);
            throw new DBusException("Failed to parse DBus type signature: " + _dbusType);
        }
    }

    /**
    * Recursively converts types for serialization onto DBus.
    * @param _parameters The parameters to convert.
    * @param _types The (possibly generic) types of the parameters.
    * @param _conn the connection
    * @return The converted parameters.
    * @throws DBusException Thrown if there is an error in converting the objects.
    */
    public static Object[] convertParameters(Object[] _parameters, Type[] _types, AbstractConnection _conn) throws DBusException {
        if (null == _parameters) {
            return null;
        }
        for (int i = 0; i < _parameters.length; i++) {
            if (null == _parameters[i]) {
                continue;
            }
            LOGGER.trace("Converting {} from {} to {}", i, _parameters[i], _types[i]);

            if (_parameters[i] instanceof DBusSerializable) {
                for (Method m : _parameters[i].getClass().getDeclaredMethods()) {
                    if (m.getName().equals("deserialize")) {
                        Type[] newtypes = m.getParameterTypes();
                        Type[] expand = new Type[_types.length + newtypes.length - 1];
                        System.arraycopy(_types, 0, expand, 0, i);
                        System.arraycopy(newtypes, 0, expand, i, newtypes.length);
                        System.arraycopy(_types, i + 1, expand, i + newtypes.length, _types.length - i - 1);
                        _types = expand;
                        Object[] newparams = ((DBusSerializable) _parameters[i]).serialize();
                        Object[] exparams = new Object[_parameters.length + newparams.length - 1];
                        System.arraycopy(_parameters, 0, exparams, 0, i);
                        System.arraycopy(newparams, 0, exparams, i, newparams.length);
                        System.arraycopy(_parameters, i + 1, exparams, i + newparams.length, _parameters.length - i - 1);
                        _parameters = exparams;
                    }
                }
                i--;
            } else if (_parameters[i] instanceof Tuple) {
                Type[] newtypes = ((ParameterizedType) _types[i]).getActualTypeArguments();
                Type[] expand = new Type[_types.length + newtypes.length - 1];
                System.arraycopy(_types, 0, expand, 0, i);
                System.arraycopy(newtypes, 0, expand, i, newtypes.length);
                System.arraycopy(_types, i + 1, expand, i + newtypes.length, _types.length - i - 1);
                _types = expand;
                Object[] newparams = ((Tuple) _parameters[i]).getParameters();
                Object[] exparams = new Object[_parameters.length + newparams.length - 1];
                System.arraycopy(_parameters, 0, exparams, 0, i);
                System.arraycopy(newparams, 0, exparams, i, newparams.length);
                System.arraycopy(_parameters, i + 1, exparams, i + newparams.length, _parameters.length - i - 1);
                _parameters = exparams;
                LOGGER.trace("New params: {}, new types: {}", Arrays.deepToString(_parameters), Arrays.deepToString(_types));
                i--;
            } else if (_types[i] instanceof TypeVariable && !(_parameters[i] instanceof Variant)) {
                // its an unwrapped variant, wrap it
                _parameters[i] = new Variant<>(_parameters[i]);
            } else if (_parameters[i] instanceof DBusInterface) {
                _parameters[i] = _conn.getExportedObject((DBusInterface) _parameters[i]);
            }
        }
        return _parameters;
    }

    @SuppressWarnings("unchecked")
    static Object deSerializeParameter(Object _parameter, Type _type, AbstractConnection _conn) throws Exception {
        LOGGER.trace("Deserializing from {} to {}", _parameter.getClass(), _type.getClass());

        // its a wrapped variant, unwrap it
        if (_type instanceof TypeVariable && _parameter instanceof Variant) {
            _parameter = ((Variant<?>) _parameter).getValue();
        }

        // Turn a signature into a Type[]
        if (_type instanceof Class && ((Class<?>) _type).isArray() && ((Class<?>) _type).getComponentType().equals(Type.class) && _parameter instanceof String) {
            List<Type> rv = new ArrayList<>();
            getJavaType((String) _parameter, rv, -1);
            _parameter = rv.toArray(new Type[0]);
        }

        // its an object path, get/create the proxy
        if (_parameter instanceof ObjectPath) {
            if (_type instanceof Class && DBusInterface.class.isAssignableFrom((Class<?>) _type)) {
                _parameter = _conn.getExportedObject(((ObjectPath) _parameter).getSource(), ((ObjectPath) _parameter).getPath());
            } else {
                _parameter = new DBusPath(((ObjectPath) _parameter).getPath());
            }
        }

        // it should be a struct. create it
        if (_parameter instanceof Object[] && _type instanceof Class && Struct.class.isAssignableFrom((Class<?>) _type)) {
            LOGGER.trace("Creating Struct {} from {}", _type, _parameter);
            Type[] ts = Container.getTypeCache(_type);
            if (null == ts) {
                Field[] fs = ((Class<?>) _type).getDeclaredFields();
                ts = new Type[fs.length];
                for (Field f : fs) {
                    Position p = f.getAnnotation(Position.class);
                    if (null == p) {
                        continue;
                    }
                    ts[p.value()] = f.getGenericType();
                }
                Container.putTypeCache(_type, ts);
            }

            // recurse over struct contents
            _parameter = deSerializeParameters((Object[]) _parameter, ts, _conn);
            for (Constructor<?> con : ((Class<?>) _type).getDeclaredConstructors()) {
                try {
                    _parameter = con.newInstance((Object[]) _parameter);
                    break;
                } catch (IllegalArgumentException exIa) {
                }
            }
        }

        // recurse over arrays
        if (_parameter instanceof Object[]) {
            Type[] ts = new Type[((Object[]) _parameter).length];
            Arrays.fill(ts, _parameter.getClass().getComponentType());
            _parameter = deSerializeParameters((Object[]) _parameter, ts, _conn);
        }
        if (_parameter instanceof List) {
            Type type2;
            if (_type instanceof ParameterizedType) {
                type2 = ((ParameterizedType) _type).getActualTypeArguments()[0];
            } else if (_type instanceof GenericArrayType) {
                type2 = ((GenericArrayType) _type).getGenericComponentType();
            } else if (_type instanceof Class && ((Class<?>) _type).isArray()) {
                type2 = ((Class<?>) _type).getComponentType();
            } else {
                type2 = null;
            }
            if (null != type2) {
                _parameter = deSerializeParameters((List<Object>) _parameter, type2, _conn);
            }
        }

        // correct floats if appropriate
        if (_type.equals(Float.class) || _type.equals(Float.TYPE)) {
            if (!(_parameter instanceof Float)) {
                _parameter = ((Number) _parameter).floatValue();
            }
        }

        // make sure arrays are in the correct format
        if (_parameter instanceof Object[] || _parameter instanceof List || _parameter.getClass().isArray()) {
            if (_type instanceof ParameterizedType) {
                _parameter = ArrayFrob.convert(_parameter, (Class<? extends Object>) ((ParameterizedType) _type).getRawType());
            } else if (_type instanceof GenericArrayType) {
                Type ct = ((GenericArrayType) _type).getGenericComponentType();
                Class<?> cc = null;
                if (ct instanceof Class) {
                    cc = (Class<?>) ct;
                }
                if (ct instanceof ParameterizedType) {
                    cc = (Class<?>) ((ParameterizedType) ct).getRawType();
                }
                Object o = Array.newInstance(cc, 0);
                _parameter = ArrayFrob.convert(_parameter, o.getClass());
            } else if (_type instanceof Class && ((Class<?>) _type).isArray()) {
                Class<?> cc = ((Class<?>) _type).getComponentType();
                if ((cc.equals(Float.class) || cc.equals(Float.TYPE)) && (_parameter instanceof double[])) {
                    double[] tmp1 = (double[]) _parameter;
                    float[] tmp2 = new float[tmp1.length];
                    for (int i = 0; i < tmp1.length; i++) {
                        tmp2[i] = (float) tmp1[i];
                    }
                    _parameter = tmp2;
                }
                Object o = Array.newInstance(cc, 0);
                _parameter = ArrayFrob.convert(_parameter, o.getClass());
            }
        }
        if (_parameter instanceof DBusMap) {
            LOGGER.trace("Deserializing a Map");
            DBusMap<?,?> dmap = (DBusMap<?,?>) _parameter;

            Type[] maptypes;
            if (_type instanceof ParameterizedType) {
                maptypes = ((ParameterizedType) _type).getActualTypeArguments();
            } else {
                maptypes = _parameter.getClass().getTypeParameters();
            }

            for (int i = 0; i < dmap.entries.length; i++) {
                dmap.entries[i][0] = deSerializeParameter(dmap.entries[i][0], maptypes[0], _conn);
                dmap.entries[i][1] = deSerializeParameter(dmap.entries[i][1], maptypes[1], _conn);
            }
        }
        return _parameter;
    }

    static List<Object> deSerializeParameters(List<Object> _parameters, Type _type, AbstractConnection _conn) throws Exception {
        LOGGER.trace("Deserializing from {} to {}",_parameters, _type);
        if (null == _parameters) {
            return null;
        }
        for (int i = 0; i < _parameters.size(); i++) {
            if (null == _parameters.get(i)) {
                continue;
            }

            _parameters.set(i, deSerializeParameter(_parameters.get(i), _type, _conn));
        }
        return _parameters;
    }

    @SuppressWarnings("unchecked")
    public static Object[] deSerializeParameters(Object[] _parameters, Type[] _types, AbstractConnection _conn) throws Exception {
        LOGGER.trace("Deserializing from {} to {} ", Arrays.deepToString(_parameters), Arrays.deepToString(_types));
        if (null == _parameters) {
            return null;
        }

        if (_types.length == 1 && _types[0] instanceof ParameterizedType && Tuple.class.isAssignableFrom((Class<?>) ((ParameterizedType) _types[0]).getRawType())) {
            _types = ((ParameterizedType) _types[0]).getActualTypeArguments();
        }

        for (int i = 0; i < _parameters.length; i++) {
            // CHECK IF ARRAYS HAVE THE SAME LENGTH <-- has to happen after expanding parameters
            if (i >= _types.length) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.error("Parameter length differs, expected {} but got {}", _parameters.length, _types.length);
                    for (int j = 0; j < _parameters.length; j++) {
                        LOGGER.error("Error, Parameters differ: {}, '{}'", j, _parameters[j].toString());
                    }
                }
                throw new DBusException("Error deserializing message: number of parameters didn't match receiving signature");
            }
            if (null == _parameters[i]) {
                continue;
            }

            if ((_types[i] instanceof Class && DBusSerializable.class.isAssignableFrom((Class<? extends Object>) _types[i])) || (_types[i] instanceof ParameterizedType && DBusSerializable.class.isAssignableFrom((Class<? extends Object>) ((ParameterizedType) _types[i]).getRawType()))) {
                Class<? extends DBusSerializable> dsc;
                if (_types[i] instanceof Class) {
                    dsc = (Class<? extends DBusSerializable>) _types[i];
                } else {
                    dsc = (Class<? extends DBusSerializable>) ((ParameterizedType) _types[i]).getRawType();
                }
                for (Method m : dsc.getDeclaredMethods()) {
                    if (m.getName().equals("deserialize")) {
                        Type[] newtypes = m.getGenericParameterTypes();
                        try {
                            Object[] sub = new Object[newtypes.length];
                            System.arraycopy(_parameters, i, sub, 0, newtypes.length);
                            sub = deSerializeParameters(sub, newtypes, _conn);
                            DBusSerializable sz = dsc.newInstance();
                            m.invoke(sz, sub);
                            Object[] compress = new Object[_parameters.length - newtypes.length + 1];
                            System.arraycopy(_parameters, 0, compress, 0, i);
                            compress[i] = sz;
                            System.arraycopy(_parameters, i + newtypes.length, compress, i + 1, _parameters.length - i - newtypes.length);
                            _parameters = compress;
                        } catch (ArrayIndexOutOfBoundsException _ex) {
                            LOGGER.debug("", _ex);
                            throw new DBusException(String.format("Not enough elements to create custom object from serialized data (%s < %s).", _parameters.length - i, newtypes.length));
                        }
                    }
                }
            } else {
                _parameters[i] = deSerializeParameter(_parameters[i], _types[i], _conn);
            }
        }
        return _parameters;
    }
}
