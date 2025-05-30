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
package org.openhab.core.ui.internal.chart.defaultchartprovider;

import java.awt.Color;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Implementation of the white {@link ChartTheme chart theme} with transparent background.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class ChartThemeWhiteTransparent extends ChartThemeWhite {

    private static final String THEME_NAME = "white_transparent";

    @Override
    public String getThemeName() {
        return THEME_NAME;
    }

    @Override
    public Color getPlotBackgroundColor() {
        return new Color(0, 0, 0, 0);
    }

    @Override
    public Color getChartBackgroundColor() {
        return new Color(0, 0, 0, 0);
    }
}
