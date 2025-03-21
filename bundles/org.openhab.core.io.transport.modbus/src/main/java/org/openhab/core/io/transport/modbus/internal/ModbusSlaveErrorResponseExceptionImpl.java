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
package org.openhab.core.io.transport.modbus.internal;

import java.io.Serial;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.modbus.exception.ModbusSlaveErrorResponseException;

import net.wimpi.modbus.ModbusSlaveException;

/**
 * Exception for explicit exception responses from Modbus slave
 *
 * @author Sami Salonen - Initial contribution
 * @author Nagy Attila Gabor - added getter for error type
 *
 */
@NonNullByDefault
public class ModbusSlaveErrorResponseExceptionImpl extends ModbusSlaveErrorResponseException {

    @Serial
    private static final long serialVersionUID = 6334580162425192133L;
    private int rawCode;
    private Optional<KnownExceptionCode> exceptionCode;

    public ModbusSlaveErrorResponseExceptionImpl(ModbusSlaveException e) {
        rawCode = e.getType();
        exceptionCode = KnownExceptionCode.tryFromExceptionCode(rawCode);
    }

    /**
     * @return the Modbus exception code that happened
     */
    @Override
    public int getExceptionCode() {
        return rawCode;
    }

    @Override
    public @Nullable String getMessage() {
        return String.format("Slave responded with error=%d (%s)", rawCode,
                exceptionCode.map(Enum::name).orElse("unknown error code"));
    }

    @Override
    public String toString() {
        return String.format("ModbusSlaveErrorResponseException(error=%d)", rawCode);
    }
}
