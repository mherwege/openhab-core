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
package org.openhab.core.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.items.ManagedItemProvider.PersistedItem;
import org.openhab.core.items.dto.GroupFunctionDTO;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ManagedItemProvider} is an OSGi service, that allows to add or remove
 * items at runtime by calling {@link #add} or {@link #remove}.
 * An added item is automatically
 * exposed to the {@link ItemRegistry}. Persistence of added Items is handled by
 * a {@link StorageService}. Items are being restored using the given {@link ItemFactory}s.
 *
 * @author Dennis Nobel - Initial contribution, added support for GroupItems
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Kai Kreuzer - improved return values
 * @author Alex Tugarev - added tags
 */
@NonNullByDefault
@Component(immediate = true, service = { ItemProvider.class, ManagedItemProvider.class })
public class ManagedItemProvider extends AbstractManagedProvider<Item, String, PersistedItem> implements ItemProvider {

    public static class PersistedItem {

        public PersistedItem(String itemType) {
            this.itemType = itemType;
        }

        public @Nullable String baseItemType;
        public @Nullable List<String> groupNames;
        public String itemType;
        public @Nullable Set<String> tags;
        public @Nullable String label;
        public @Nullable String category;
        public @Nullable String functionName;
        public @Nullable List<String> functionParams;
        public @Nullable String dimension;
    }

    private final Logger logger = LoggerFactory.getLogger(ManagedItemProvider.class);

    private final ItemBuilderFactory itemBuilderFactory;
    private final Map<String, PersistedItem> failedToCreate = new ConcurrentHashMap<>();

    @Activate
    public ManagedItemProvider(final @Reference StorageService storageService,
            final @Reference ItemBuilderFactory itemBuilderFactory) {
        super(storageService);
        this.itemBuilderFactory = itemBuilderFactory;
    }

    @Override
    public @Nullable Item remove(String key) {
        Item item = get(key);
        if (item instanceof GroupItem groupItem) {
            removeGroupNameFromMembers(groupItem);
        }

        return super.remove(key);
    }

    /**
     * Removes an item and its member if recursive flag is set to true.
     *
     * @param itemName item name to remove
     * @param recursive if set to true all members of the item will be removed, too.
     * @return removed item or null if no item with that name exists
     */
    public @Nullable Item remove(String itemName, boolean recursive) {
        Item item = get(itemName);
        if (recursive && item instanceof GroupItem groupItem) {
            for (String member : getMemberNamesRecursively(groupItem, getAll())) {
                remove(member);
            }
        }
        if (item != null) {
            remove(item.getName());
            return item;
        } else {
            return null;
        }
    }

    @Override
    public void add(Item element) {
        if (!ItemUtil.isValidItemName(element.getName())) {
            throw new IllegalArgumentException("The item name '" + element.getName() + "' is invalid.");
        }
        super.add(element);
    }

    private List<String> getMemberNamesRecursively(GroupItem groupItem, Collection<Item> allItems) {
        List<String> memberNames = new ArrayList<>();
        for (Item item : allItems) {
            if (item.getGroupNames().contains(groupItem.getName())) {
                memberNames.add(item.getName());
                if (item instanceof GroupItem groupItem1) {
                    memberNames.addAll(getMemberNamesRecursively(groupItem1, allItems));
                }
            }
        }
        return memberNames;
    }

    private Set<Item> getMembers(GroupItem groupItem, Collection<Item> allItems) {
        Set<Item> members = new HashSet<>();
        for (Item item : allItems) {
            if (item.getGroupNames().contains(groupItem.getName())) {
                members.add(item);
            }
        }
        return members;
    }

    private @Nullable Item createItem(String itemType, String itemName) {
        try {
            return itemBuilderFactory.newItemBuilder(itemType, itemName).build();
        } catch (IllegalStateException e) {
            logger.debug("Couldn't create item '{}' of type '{}'", itemName, itemType);
            return null;
        }
    }

    private void removeGroupNameFromMembers(GroupItem groupItem) {
        Set<Item> members = getMembers(groupItem, getAll());
        for (Item member : members) {
            if (member instanceof GenericItem item) {
                item.removeGroupName(groupItem.getUID());
                update(member);
            }
        }
    }

    /**
     * Translates the Items class simple name into a type name understandable by
     * the {@link ItemFactory}s.
     *
     * @param item the Item to translate the name
     * @return the translated ItemTypeName understandable by the {@link ItemFactory}s
     */
    private String toItemFactoryName(Item item) {
        return item.getType();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addItemFactory(ItemFactory itemFactory) {
        if (!failedToCreate.isEmpty()) {
            // retry failed creation attempts
            Iterator<Entry<String, PersistedItem>> iterator = failedToCreate.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, PersistedItem> entry = iterator.next();
                String itemName = entry.getKey();
                PersistedItem persistedItem = entry.getValue();
                Item item = itemFactory.createItem(persistedItem.itemType, itemName);
                if (item instanceof GenericItem genericItem) {
                    iterator.remove();
                    configureItem(persistedItem, genericItem);
                    notifyListenersAboutAddedElement(item);
                } else {
                    logger.debug("The added item factory '{}' still could not instantiate item '{}'.", itemFactory,
                            itemName);
                }
            }

            if (failedToCreate.isEmpty()) {
                logger.info("Finished loading the items which could not have been created before.");
            }
        }
    }

    protected void removeItemFactory(ItemFactory itemFactory) {
    }

    @Override
    protected String getStorageName() {
        return Item.class.getName();
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    @Override
    protected @Nullable Item toElement(String itemName, PersistedItem persistedItem) {
        Item item;

        if (GroupItem.TYPE.equals(persistedItem.itemType)) {
            String baseItemType = persistedItem.baseItemType;
            if (baseItemType != null) {
                Item baseItem = createItem(baseItemType, itemName);
                if (persistedItem.functionName != null) {
                    GroupFunction function = getGroupFunction(persistedItem, baseItem);
                    item = new GroupItem(itemName, baseItem, function);
                } else {
                    item = new GroupItem(itemName, baseItem, new GroupFunction.Equality());
                }
            } else {
                item = new GroupItem(itemName);
            }
        } else {
            item = createItem(persistedItem.itemType, itemName);
        }

        if (item instanceof GenericItem genericItem) {
            configureItem(persistedItem, genericItem);
        }

        if (item == null) {
            failedToCreate.put(itemName, persistedItem);
            logger.debug("Couldn't restore item '{}' of type '{}' ~ there is no appropriate ItemFactory available.",
                    itemName, persistedItem.itemType);
        }

        return item;
    }

    private GroupFunction getGroupFunction(PersistedItem persistedItem, @Nullable Item baseItem) {
        GroupFunctionDTO functionDTO = new GroupFunctionDTO();
        functionDTO.name = persistedItem.functionName;
        if (persistedItem.functionParams instanceof List<?> list) {
            functionDTO.params = list.toArray(new String[list.size()]);
        }
        return ItemDTOMapper.mapFunction(baseItem, functionDTO);
    }

    private void configureItem(PersistedItem persistedItem, GenericItem item) {
        List<String> groupNames = persistedItem.groupNames;
        if (groupNames != null) {
            for (String groupName : groupNames) {
                item.addGroupName(groupName);
            }
        }

        Set<String> tags = persistedItem.tags;
        if (tags != null) {
            for (String tag : tags) {
                item.addTag(tag);
            }
        }

        item.setLabel(persistedItem.label);
        item.setCategory(persistedItem.category);
    }

    @Override
    protected PersistedItem toPersistableElement(Item item) {
        PersistedItem persistedItem = new PersistedItem(
                item instanceof GroupItem ? GroupItem.TYPE : toItemFactoryName(item));

        if (item instanceof GroupItem groupItem) {
            String baseItemType = null;
            Item baseItem = groupItem.getBaseItem();
            if (baseItem != null) {
                baseItemType = toItemFactoryName(baseItem);
            }
            persistedItem.baseItemType = baseItemType;

            addFunctionToPersisedItem(persistedItem, groupItem);
        }

        persistedItem.label = item.getLabel();
        persistedItem.groupNames = new ArrayList<>(item.getGroupNames());
        persistedItem.tags = new HashSet<>(item.getTags());
        persistedItem.category = item.getCategory();

        return persistedItem;
    }

    private void addFunctionToPersisedItem(PersistedItem persistedItem, GroupItem groupItem) {
        if (groupItem.getFunction() != null) {
            GroupFunctionDTO functionDTO = ItemDTOMapper.mapFunction(groupItem.getFunction());
            if (functionDTO != null) {
                persistedItem.functionName = functionDTO.name;
                if (functionDTO.params != null) {
                    persistedItem.functionParams = Arrays.asList(functionDTO.params);
                }
            }
        }
    }
}
