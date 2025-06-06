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
package org.openhab.core.automation.module.script.rulesupport.loader;

import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.service.ReadyMarker;

/**
 * The {@link ScriptFileWatcher} interface needs to be implemented by script file watchers. Services that implement this
 * interface can be tracked to set a {@link ReadyMarker} once all services have completed their initial loading.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ScriptFileWatcher {

    /**
     * Returns a {@link CompletableFuture<Void>} that completes when the {@link ScriptFileWatcher} has completed it's
     * initial loading of files.
     *
     * @return the {@link CompletableFuture}
     */
    CompletableFuture<@Nullable Void> ifInitialized();
}
