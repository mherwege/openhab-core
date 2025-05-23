/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a helper class that helps parsing a string into an openHAB type (state or command).
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public final class TypeParser {

    private static final String CORE_LIBRARY_PACKAGE = "org.openhab.core.library.types.";
    private static final String VALUE_OF = "valueOf";

    /**
     * No instances allowed.
     */
    private TypeParser() {
    }

    /**
     * Parses a string into a type.
     *
     * @param typeName name of the type, for example StringType.
     * @param input input string to parse.
     * @return Parsed type or null, if the type couldn't be parsed.
     */
    public static @Nullable Type parseType(String typeName, String input) {
        try {
            Class<?> stateClass = Class.forName(CORE_LIBRARY_PACKAGE + typeName);
            Method valueOfMethod = stateClass.getMethod(VALUE_OF, String.class);
            return (Type) valueOfMethod.invoke(stateClass, input);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
        }
        return null;
    }

    /**
     * <p>
     * Determines a state from a string. Possible state types are passed as a parameter. Note that the order matters
     * here; the first type that accepts the string as a valid value, will be used for the state.
     *
     * <p>
     * Example: The type list is OnOffType.class,StringType.class. The string "ON" is now accepted by the OnOffType and
     * thus OnOffType.ON will be returned (and not a StringType with value "ON").
     *
     * @param types possible types of the state to consider
     * @param s the string to parse
     * @return the corresponding State instance or <code>null</code>
     */
    public static @Nullable State parseState(List<Class<? extends State>> types, String s) {
        for (Class<? extends State> type : types) {
            try {
                Method valueOf = type.getMethod(VALUE_OF, String.class);
                if (valueOf.invoke(type, s) instanceof State state) {
                    return state;
                }
            } catch (NoSuchMethodException | IllegalArgumentException | IllegalAccessException
                    | InvocationTargetException e) {
            }
        }
        return null;
    }

    /**
     * <p>
     * Determines a command from a string. Possible command types are passed as a parameter. Note that the order matters
     * here; the first type that accepts the string as a valid value, will be used for the command.
     *
     * <p>
     * Example: The type list is OnOffType.class,StringType.class. The string "ON" is now accepted by the OnOffType and
     * thus OnOffType.ON will be returned (and not a StringType with value "ON").
     *
     * @param types possible types of the command to consider
     * @param s the string to parse
     * @return the corresponding Command instance or <code>null</code>
     */
    public static @Nullable Command parseCommand(List<Class<? extends Command>> types, String s) {
        for (Class<? extends Command> type : types) {
            try {
                Method valueOf = type.getMethod(VALUE_OF, String.class);
                if (valueOf.invoke(type, s) instanceof Command value) {
                    return value;
                }
            } catch (NoSuchMethodException | IllegalArgumentException | IllegalAccessException
                    | InvocationTargetException e) {
            }
        }
        return null;
    }
}
