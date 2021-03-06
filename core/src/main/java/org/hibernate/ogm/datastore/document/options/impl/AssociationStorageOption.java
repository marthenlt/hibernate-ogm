/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.document.options.impl;

import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;

/**
 * Represents the type of association storage as configured via the API or annotations for a given element.
 *
 * @author Gunnar Morling
 */
public class AssociationStorageOption extends UniqueOption<AssociationStorageType> {

	private static final AssociationStorageType DEFAULT_ASSOCIATION_STORAGE = AssociationStorageType.IN_ENTITY;

	@Override
	public AssociationStorageType getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( DocumentStoreProperties.ASSOCIATIONS_STORE, AssociationStorageType.class )
				.withDefault( DEFAULT_ASSOCIATION_STORAGE )
				.getValue();
	}
}
