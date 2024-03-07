/**
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
package org.openhab.core.internal.library.unit;

import static org.openhab.core.library.unit.CurrencyUnits.BASE_CURRENCY;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.dimension.Currency;
import org.openhab.core.library.unit.CurrencyProvider;
import org.openhab.core.library.unit.CurrencyUnit;
import org.openhab.core.library.unit.CurrencyUnits;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.units.indriya.format.SimpleUnitFormat;

/**
 * The {@link CurrencyService} allows to register and switch {@link CurrencyProvider}s and provides exchange rates
 * for currencies
 *
 * @author Jan N. Klug - Initial contribution
 */
@Component(immediate = true, service = CurrencyService.class, name = CurrencyService.SERVICE_NAME, configurationPid = CurrencyService.CONFIG_PID)
@NonNullByDefault
public class CurrencyService {
    public static final String SERVICE_NAME = "currency-service";
    public static final String CONFIG_PID = "org.openhab.regional";

    public static final String CONFIG_OPTION_CURRENCY_PROVIDER = "currencyProvider";

    private final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    private final ConfigurationAdmin configurationAdmin;

    public static Function<Unit<Currency>, @Nullable BigDecimal> FACTOR_FCN = unit -> null;

    private final Map<String, CurrencyProvider> currencyProviders = new ConcurrentHashMap<>();

    private CurrencyProvider enabledCurrencyProvider = DefaultCurrencyProvider.getInstance();
    private String configuredCurrencyProvider = DefaultCurrencyProvider.getInstance().getName();

    private @Nullable Map<String, Object> config;
    private final ScheduledExecutorService scheduler;
    private @Nullable ScheduledFuture<?> configSyncTask = null;

    @Activate
    public CurrencyService(final @Reference ConfigurationAdmin configurationAdmin,
            @Nullable Map<String, Object> config) {
        this.configurationAdmin = configurationAdmin;
        modified(config);

        // Changes to the configuration are expected to call the {@link modified} method. This works well when running
        // in Eclipse. Running in Karaf, the method was not consistently called. Therefore regularly check for changes
        // in configuration.
        // This pattern and code was re-used from {@link org.openhab.core.karaf.internal.FeatureInstaller}
        scheduler = ThreadPoolManager.getScheduledPool(ThreadPoolManager.THREAD_POOL_NAME_COMMON);
        this.configSyncTask = scheduler.scheduleWithFixedDelay(this::syncConfiguration, 1, 1, TimeUnit.MINUTES);
    }

    @Deactivate
    protected void deactivate() {
        ScheduledFuture<?> task = this.configSyncTask;
        if (task != null) {
            task.cancel(true);
        }
        this.configSyncTask = null;
    }

    @Modified
    public void modified(@Nullable Map<String, Object> config) {
        String configOption = (String) config.get(CONFIG_OPTION_CURRENCY_PROVIDER);
        configuredCurrencyProvider = Objects.requireNonNullElse(configOption,
                DefaultCurrencyProvider.getInstance().getName());
        CurrencyProvider currencyProvider = currencyProviders.getOrDefault(configuredCurrencyProvider,
                DefaultCurrencyProvider.getInstance());
        enableProvider(currencyProvider);
    }

    private void syncConfiguration() {
        try {
            Dictionary<String, Object> cfg = configurationAdmin.getConfiguration(CONFIG_PID).getProperties();
            if (cfg == null) {
                return;
            }
            final Map<String, Object> cfgMap = new HashMap<>();
            final Enumeration<String> enumeration = cfg.keys();
            while (enumeration.hasMoreElements()) {
                final String key = enumeration.nextElement();
                cfgMap.put(key, cfg.get(key));
            }
            if (!cfgMap.equals(config)) {
                modified(cfgMap);
            }
        } catch (IOException | IllegalStateException e) {
            logger.debug("Exception occurred while trying to sync the configuration: {}", e.getMessage());
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addCurrencyProvider(CurrencyProvider currencyProvider) {
        currencyProviders.put(currencyProvider.getName(), currencyProvider);
        if (configuredCurrencyProvider.equals(currencyProvider.getName())) {
            enableProvider(currencyProvider);
        }
    }

    public void removeCurrencyProvider(CurrencyProvider currencyProvider) {
        if (currencyProvider.equals(enabledCurrencyProvider)) {
            logger.warn("The currently activated currency provider is being removed. Enabling default.");
            enableProvider(DefaultCurrencyProvider.getInstance());
        }
        currencyProviders.remove(currencyProvider.getName());
    }

    private synchronized void enableProvider(CurrencyProvider currencyProvider) {
        SimpleUnitFormat unitFormatter = SimpleUnitFormat.getInstance();
        // remove units from old provider
        enabledCurrencyProvider.getAdditionalCurrencies().forEach(CurrencyUnits::removeUnit);
        unitFormatter.removeLabel(enabledCurrencyProvider.getBaseCurrency());

        // add new units
        FACTOR_FCN = currencyProvider.getExchangeRateFunction();
        Unit<Currency> baseCurrency = currencyProvider.getBaseCurrency();
        ((CurrencyUnit) BASE_CURRENCY).setSymbol(baseCurrency.getSymbol());
        ((CurrencyUnit) BASE_CURRENCY).setName(baseCurrency.getName());
        unitFormatter.label(BASE_CURRENCY, baseCurrency.getName());
        if (baseCurrency.getSymbol() != null) {
            unitFormatter.alias(BASE_CURRENCY, baseCurrency.getSymbol());
        }

        currencyProvider.getAdditionalCurrencies().forEach(CurrencyUnits::addUnit);

        this.enabledCurrencyProvider = currencyProvider;
    }

    private static class DefaultCurrencyProvider implements CurrencyProvider {
        private static final CurrencyProvider INSTANCE = new DefaultCurrencyProvider();

        @Override
        public Unit<Currency> getBaseCurrency() {
            return new CurrencyUnit("DEF", null);
        }

        @Override
        public Collection<Unit<Currency>> getAdditionalCurrencies() {
            return Set.of();
        }

        @Override
        public Function<Unit<Currency>, @Nullable BigDecimal> getExchangeRateFunction() {
            return unit -> null;
        }

        public static CurrencyProvider getInstance() {
            return INSTANCE;
        }
    }
}
