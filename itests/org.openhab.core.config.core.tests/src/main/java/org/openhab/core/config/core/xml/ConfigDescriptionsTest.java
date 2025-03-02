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
package org.openhab.core.config.core.xml;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterGroup;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.test.BundleCloseable;
import org.openhab.core.test.SyntheticBundleInstaller;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * The ConfigDescriptionsTest is a test for loading of configuration description from XML documents.
 *
 * @author Alex Tugarev - Initial contribution; Extended tests for options and filters
 * @author Thomas Höfer - Added unit
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public class ConfigDescriptionsTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "ConfigDescriptionsTest.bundle";
    private static final String FRAGMENT_TEST_HOST_NAME = "ConfigDescriptionsFragmentTest.host";
    private static final String FRAGMENT_TEST_FRAGMENT_NAME = "ConfigDescriptionsFragmentTest.fragment";

    private @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistry;
    private @NonNullByDefault({}) BindingInstaller bindingInstaller;

    @BeforeEach
    public void setUp() {
        configDescriptionRegistry = getService(ConfigDescriptionRegistry.class);
        assertThat(configDescriptionRegistry, is(notNullValue()));
        bindingInstaller = new BindingInstaller(this::waitForAssert, configDescriptionRegistry, bundleContext);
    }

    @Test
    public void assertThatConfigDescriptionsAreLoadedProperly() throws Exception {
        bindingInstaller.exec(TEST_BUNDLE_NAME, () -> {
            Collection<ConfigDescription> englishConfigDescriptions = configDescriptionRegistry
                    .getConfigDescriptions(Locale.ENGLISH);

            ConfigDescription englishDescription = Objects
                    .requireNonNull(findDescription(englishConfigDescriptions, "config:dummyConfig"));

            List<ConfigDescriptionParameter> parameters = englishDescription.getParameters();
            assertThat(parameters.size(), is(14));

            ConfigDescriptionParameter ipParameter = findParameter(englishDescription, "ip");
            assertThat(ipParameter, is(notNullValue()));
            assertThat(ipParameter.getType(), is(Type.TEXT));
            assertThat(ipParameter.getGroupName(), is(nullValue()));
            assertThat(ipParameter.getContext(), is("network-address"));
            assertThat(ipParameter.getLabel(), is("Network Address"));
            assertThat(ipParameter.getDescription(), is("Network address of the hue bridge."));
            assertThat(ipParameter.getPattern(), is("[0-9]{3}.[0-9]{3}.[0-9]{3}.[0-9]{3}"));
            assertThat(ipParameter.isRequired(), is(true));
            assertThat(ipParameter.isMultiple(), is(false));
            assertThat(ipParameter.isReadOnly(), is(true));
            assertThat(ipParameter.getUnit(), is(nullValue()));
            assertThat(ipParameter.getUnitLabel(), is(nullValue()));

            ConfigDescriptionParameter usernameParameter = findParameter(englishDescription, "username");
            assertThat(usernameParameter, is(notNullValue()));
            assertThat(usernameParameter.getType(), is(Type.TEXT));
            assertThat(usernameParameter.getGroupName(), is("user"));
            assertThat(usernameParameter.getContext(), is("password"));
            assertThat(usernameParameter.getLabel(), is("Username"));
            assertThat(usernameParameter.isRequired(), is(false));
            assertThat(usernameParameter.isMultiple(), is(false));
            assertThat(usernameParameter.isReadOnly(), is(false));
            assertThat(usernameParameter.getDescription(),
                    is("Name of a registered hue bridge user, that allows to access the API."));

            ConfigDescriptionParameter userPassParameter = findParameter(englishDescription, "user-pass");
            assertThat(userPassParameter, is(notNullValue()));
            assertThat(userPassParameter.getType(), is(Type.TEXT));
            assertThat(userPassParameter.getMinimum(), is(BigDecimal.valueOf(8)));
            assertThat(userPassParameter.getMaximum(), is(BigDecimal.valueOf(16)));
            assertThat(userPassParameter.isRequired(), is(true));
            assertThat(userPassParameter.isVerifyable(), is(true));
            assertThat(userPassParameter.isMultiple(), is(false));
            assertThat(userPassParameter.isReadOnly(), is(false));
            assertThat(userPassParameter.getContext(), is("password"));
            assertThat(userPassParameter.getLabel(), is("Password"));

            ConfigDescriptionParameter colorItemParameter = findParameter(englishDescription, "color-alarming-light");
            assertThat(colorItemParameter, is(notNullValue()));
            assertThat(colorItemParameter.getType(), is(Type.TEXT));
            assertThat(colorItemParameter.isRequired(), is(false));
            assertThat(colorItemParameter.isReadOnly(), is(false));
            assertThat(colorItemParameter.getContext(), is("item"));
            assertThat(colorItemParameter.getFilterCriteria(), is(notNullValue()));
            assertThat(
                    colorItemParameter.getFilterCriteria().stream().map(FilterCriteria::toString)
                            .collect(Collectors.joining(", ")),
                    is("FilterCriteria [name=\"tags\", value=\"alarm, light\"], FilterCriteria [name=\"type\", value=\"color\"], FilterCriteria [name=\"binding-id\", value=\"hue\"]"));

            ConfigDescriptionParameter listParameter1 = findParameter(englishDescription, "list1");
            assertThat(listParameter1, is(notNullValue()));
            assertThat(listParameter1.getType(), is(Type.TEXT));
            assertThat(listParameter1.isRequired(), is(false));
            assertThat(listParameter1.isMultiple(), is(true));
            assertThat(listParameter1.isReadOnly(), is(false));
            assertThat(listParameter1.getMinimum(), is(BigDecimal.valueOf(2)));
            assertThat(listParameter1.getMaximum(), is(BigDecimal.valueOf(3)));
            assertThat(listParameter1.getOptions(), is(notNullValue()));
            assertThat(listParameter1.isAdvanced(), is(false));
            assertThat(listParameter1.isVerifyable(), is(false));
            assertThat(listParameter1.getLimitToOptions(), is(true));
            assertThat(listParameter1.getMultipleLimit(), is(nullValue()));
            assertThat(listParameter1.getOptions().stream().map(ParameterOption::toString).collect(joining(", ")), is(
                    "ParameterOption [value=\"key1\", label=\"label1\"], ParameterOption [value=\"key2\", label=\"label2\"]"));

            ConfigDescriptionParameter listParameter2 = findParameter(englishDescription, "list2");
            assertThat(listParameter2, is(notNullValue()));
            assertThat(listParameter2.getType(), is(Type.TEXT));
            assertThat(listParameter2.isRequired(), is(false));
            assertThat(listParameter2.isMultiple(), is(true));
            assertThat(listParameter2.isReadOnly(), is(false));
            assertThat(listParameter2.getOptions(), is(notNullValue()));
            assertThat(listParameter2.isAdvanced(), is(true));
            assertThat(listParameter2.getLimitToOptions(), is(false));
            assertThat(listParameter2.getMultipleLimit(), is(4));

            ConfigDescriptionParameter unitParameter = findParameter(englishDescription, "unit");
            assertThat(unitParameter, is(notNullValue()));
            assertThat(unitParameter.getUnit(), is("m"));
            assertThat(unitParameter.getUnitLabel(), is(nullValue()));

            ConfigDescriptionParameter unitLabelParameter = findParameter(englishDescription, "unit-label");
            assertThat(unitLabelParameter, is(notNullValue()));
            assertThat(unitLabelParameter.getUnit(), is(nullValue()));
            assertThat(unitLabelParameter.getUnitLabel(), is("Runs"));

            ConfigDescriptionParameter unitOhmParameter = findParameter(englishDescription, "unit-ohm");
            assertThat(unitOhmParameter, is(notNullValue()));
            assertThat(unitOhmParameter.getUnit(), is("Ω"));
            assertThat(unitOhmParameter.getUnitLabel(), is(nullValue()));

            ConfigDescriptionParameter unitAccelerationParameter = findParameter(englishDescription,
                    "unit-acceleration");
            assertThat(unitAccelerationParameter, is(notNullValue()));
            assertThat(unitAccelerationParameter.getUnit(), is("m/s2"));
            assertThat(unitAccelerationParameter.getUnitLabel(), is("m/s\u00B2"));

            ConfigDescriptionParameter unitCelcius = findParameter(englishDescription, "unit-celcius");
            assertThat(unitCelcius, is(notNullValue()));
            assertThat(unitCelcius.getUnit(), is("Cel"));
            assertThat(unitCelcius.getUnitLabel(), is("°C"));

            ConfigDescriptionParameter unitSeconds = findParameter(englishDescription, "unit-seconds");
            assertThat(unitSeconds, is(notNullValue()));
            assertThat(unitSeconds.getUnit(), is("s"));
            assertThat(unitSeconds.getUnitLabel(), is("seconds"));

            ConfigDescriptionParameter unitMovements = findParameter(englishDescription, "unit-movements");
            assertThat(unitMovements, is(notNullValue()));
            assertThat(unitMovements.getUnit(), is(nullValue()));
            assertThat(unitMovements.getUnitLabel(), is("Movements"));

            ConfigDescriptionParameter unitKph = findParameter(englishDescription, "unit-kph");
            assertThat(unitKph, is(notNullValue()));
            assertThat(unitKph.getUnit(), is("kph"));
            assertThat(unitKph.getUnitLabel(), is("km/h"));

            assertThat(englishDescription.getParameterGroups().size(), is(2));

            ConfigDescriptionParameterGroup group1 = findParameterGroup(englishDescription, "group1");
            assertThat(group1, is(notNullValue()));
            assertThat(group1.getLabel(), is("Group 1"));
            assertThat(group1.getDescription(), is("Description Group 1"));
            assertThat(group1.isAdvanced(), is(false));
            assertThat(group1.getContext(), is("Context-Group1"));

            ConfigDescriptionParameterGroup group2 = findParameterGroup(englishDescription, "group2");
            assertThat(group2, is(notNullValue()));
            assertThat(group2.getLabel(), is("Group 2"));
            assertThat(group2.getDescription(), is("Description Group 2"));
            assertThat(group2.isAdvanced(), is(true));
            assertThat(group2.getContext(), is("Context-Group2"));

            ConfigDescription germanDescription = Objects.requireNonNull(findDescription(
                    configDescriptionRegistry.getConfigDescriptions(Locale.GERMAN), "config:dummyConfig"));

            unitSeconds = findParameter(germanDescription, "unit-seconds");
            assertThat(unitSeconds, is(notNullValue()));
            assertThat(unitSeconds.getUnit(), is("s"));
            assertThat(unitSeconds.getUnitLabel(), is("Sekunden"));

            unitMovements = findParameter(germanDescription, "unit-movements");
            assertThat(unitMovements, is(notNullValue()));
            assertThat(unitMovements.getUnit(), is(nullValue()));
            assertThat(unitMovements.getUnitLabel(), is("Bewegungen"));

            unitKph = findParameter(germanDescription, "unit-kph");
            assertThat(unitKph, is(notNullValue()));
            assertThat(unitKph.getUnit(), is("kph"));
            assertThat(unitKph.getUnitLabel(), is("km/h"));
        });
    }

    @Test
    public void assertThatConfigDescriptionsOfFragmentHostAreLoadedProperly() throws Exception {
        bindingInstaller.exec(FRAGMENT_TEST_FRAGMENT_NAME, () -> {
            try {
                try (BundleCloseable bundle = new BundleCloseable(
                        SyntheticBundleInstaller.install(bundleContext, FRAGMENT_TEST_HOST_NAME))) {
                    assertThat(bundle, is(notNullValue()));

                    Collection<ConfigDescription> configDescriptions = configDescriptionRegistry
                            .getConfigDescriptions();

                    ConfigDescription description = Objects
                            .requireNonNull(findDescription(configDescriptions, "config:fragmentConfig"));

                    List<ConfigDescriptionParameter> parameters = description.getParameters();
                    assertThat(parameters.size(), is(1));

                    ConfigDescriptionParameter usernameParameter = findParameter(description, "testParam");
                    assertThat(usernameParameter, is(notNullValue()));
                    assertThat(usernameParameter.getType(), is(Type.TEXT));
                    assertThat(usernameParameter.getLabel(), is("Test"));
                    assertThat(usernameParameter.isRequired(), is(false));
                    assertThat(usernameParameter.isMultiple(), is(false));
                    assertThat(usernameParameter.isReadOnly(), is(false));
                    assertThat(usernameParameter.getDescription(), is("Test Parameter."));
                }
            } catch (Exception e) {
                // do nothing: handle exception
            }
        });
    }

    private static @Nullable ConfigDescription findDescription(Collection<ConfigDescription> descriptions, String uri) {
        return findDescription(descriptions, URI.create(uri));
    }

    private static ConfigDescription findDescription(Collection<ConfigDescription> descriptions, URI uri) {
        return descriptions.stream().filter(d -> uri.equals(d.getUID())).findFirst().get();
    }

    private static ConfigDescriptionParameter findParameter(ConfigDescription description, String parameterName) {
        return description.getParameters().stream().filter(p -> parameterName.equals(p.getName())).findFirst().get();
    }

    private static ConfigDescriptionParameterGroup findParameterGroup(ConfigDescription description,
            String parameterGroupName) {
        return description.getParameterGroups().stream().filter(g -> parameterGroupName.equals(g.getName())).findFirst()
                .get();
    }
}
