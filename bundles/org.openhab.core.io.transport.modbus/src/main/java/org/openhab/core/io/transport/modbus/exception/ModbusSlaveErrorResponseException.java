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
package org.openhab.core.io.transport.modbus.exception;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Exception for explicit exception responses from Modbus slave
 *
 * @author Sami Salonen - Initial contribution
 * @author Nagy Attila Gabor - added getter for error type
 *
 */
@NonNullByDefault
public abstract class ModbusSlaveErrorResponseException extends ModbusTransportException {

    private static final Map<Integer, KnownExceptionCode> EXCEPTION_CODES_INDEX = new HashMap<>(10);
    static {
        for (KnownExceptionCode code : KnownExceptionCode.values()) {
            EXCEPTION_CODES_INDEX.put(code.exceptionCode, code);
        }
    }

    /**
     * Enum for known exception codes that modbus slaves (servers) can used to indicate exceptions
     *
     * @author Sami Salonen - Initial contribution
     *
     */
    public enum KnownExceptionCode {
        ILLEGAL_FUNCTION(1),
        ILLEGAL_DATA_ACCESS(2),
        ILLEGAL_DATA_VALUE(3),
        SLAVE_DEVICE_FAILURE(4),
        ACKNOWLEDGE(5),
        SLAVE_DEVICE_BUSY(6),
        NEGATIVE_ACKNOWLEDGE(7),
        MEMORY_PARITY_ERROR(8),
        GATEWAY_PATH_UNVAVAILABLE(10),
        GATEWAY_TARGET_DEVICE_FAILED_TO_RESPOND(11);

        private final int exceptionCode;

        KnownExceptionCode(int exceptionCode) {
            this.exceptionCode = exceptionCode;
        }

        public int getExceptionCode() {
            return exceptionCode;
        }

        public static Optional<KnownExceptionCode> tryFromExceptionCode(int exceptionCode) {
            return Optional.ofNullable(EXCEPTION_CODES_INDEX.get(exceptionCode));
        }
    }

    /**
     * The function code received in the query is not an allowable action for the slave. This may be because the
     * function code is only applicable to newer devices, and was not implemented in the unit selected. It could
     * also
     * indicate that the slave is in the wrong state to process a request of this type, for example because it is
     * unconfigured and is being asked to return register values. If a Poll Program Complete command was issued,
     * this
     * code indicates that no program function preceded it.
     */
    public static final int ILLEGAL_FUNCTION = KnownExceptionCode.ILLEGAL_FUNCTION.getExceptionCode();

    /**
     * The data address received in the query is not an allowable address for the slave. More specifically, the
     * combination of reference number and transfer length is invalid. For a controller with 100 registers, a
     * request
     * with offset 96 and length 4 would succeed, a request with offset 96 and length 5 will generate exception 02.
     */
    public static final int ILLEGAL_DATA_ACCESS = KnownExceptionCode.ILLEGAL_DATA_ACCESS.getExceptionCode();

    /**
     * A value contained in the query data field is not an allowable value for the slave. This indicates a fault in
     * the
     * structure of remainder of a complex request, such as that the implied length is incorrect. It specifically
     * does
     * NOT mean that a data item submitted for storage in a register has a value outside the expectation of the
     * application program, since the Modbus protocol is unaware of the significance of any particular value of any
     * particular register.
     */
    public static final int ILLEGAL_DATA_VALUE = KnownExceptionCode.ILLEGAL_DATA_VALUE.getExceptionCode();

    /**
     * An unrecoverable error occurred while the slave was attempting to perform the requested action.
     */
    public static final int SLAVE_DEVICE_FAILURE = KnownExceptionCode.SLAVE_DEVICE_FAILURE.getExceptionCode();

    /**
     * Specialized use in conjunction with programming commands.
     * The slave has accepted the request and is processing it, but a long duration of time will be required to do
     * so.
     * This response is returned to prevent a timeout error from occurring in the master. The master can next issue
     * a
     * Poll Program Complete message to determine if processing is completed.
     */
    public static final int ACKNOWLEDGE = KnownExceptionCode.ACKNOWLEDGE.getExceptionCode();

    /**
     * Specialized use in conjunction with programming commands.
     * The slave is engaged in processing a long-duration program command. The master should retransmit the message
     * later when the slave is free.
     */
    public static final int SLAVE_DEVICE_BUSY = KnownExceptionCode.SLAVE_DEVICE_BUSY.getExceptionCode();

    /**
     * The slave cannot perform the program function received in the query. This code is returned for an
     * unsuccessful
     * programming request using function code 13 or 14 decimal. The master should request diagnostic or error
     * information from the slave.
     */
    public static final int NEGATIVE_ACKNOWLEDGE = KnownExceptionCode.ACKNOWLEDGE.getExceptionCode();

    /**
     * Specialized use in conjunction with function codes 20 and 21 and reference type 6, to indicate that the
     * extended
     * file area failed to pass a consistency check.
     * The slave attempted to read extended memory or record file, but detected a parity error in memory. The master
     * can
     * retry the request, but service may be required on the slave device.
     */
    public static final int MEMORY_PARITY_ERROR = KnownExceptionCode.MEMORY_PARITY_ERROR.getExceptionCode();

    /**
     * Specialized use in conjunction with gateways, indicates that the gateway was unable to allocate an internal
     * communication path from the input port to the output port for processing the request. Usually means the
     * gateway
     * is misconfigured or overloaded.
     */
    public static final int GATEWAY_PATH_UNVAVAILABLE = KnownExceptionCode.GATEWAY_PATH_UNVAVAILABLE.getExceptionCode();

    /**
     * Specialized use in conjunction with gateways, indicates that no response was obtained from the target device.
     * Usually means that the device is not present on the network.
     */
    public static final int GATEWAY_TARGET_DEVICE_FAILED_TO_RESPOND = KnownExceptionCode.GATEWAY_TARGET_DEVICE_FAILED_TO_RESPOND
            .getExceptionCode();

    @Serial
    private static final long serialVersionUID = -1435199498550990487L;

    /**
     * @return the Modbus exception code that happened
     */
    public abstract int getExceptionCode();
}
