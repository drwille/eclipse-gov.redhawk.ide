/*******************************************************************************
 * This file is protected by Copyright. 
 * Please refer to the COPYRIGHT file distributed with this source distribution.
 *
 * This file is part of REDHAWK IDE.
 *
 * All rights reserved.  This program and the accompanying materials are made available under 
 * the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
 // BEGIN GENERATED CODE
package gov.redhawk.ide.codegen;

/**
 * 
 */
public interface IPropertyDescriptor {
	String getKey();

	String getDescription();

	boolean isRequired();
	
	/**
	 * Deprecated properties should not be selectable by the user when creating new projects.
	 * @since 9.0
	 */
	boolean isDeprecated();

	String getDefaultValue();

	/**
	 * @since 6.0
	 */
	String getName();
}
