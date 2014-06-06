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
package gov.redhawk.ide.sad.graphiti.ui.diagram.util;

import gov.redhawk.ide.sad.graphiti.ext.RHContainerShape;
import gov.redhawk.ide.sad.graphiti.ext.impl.RHContainerShapeImpl;
import gov.redhawk.ide.sad.graphiti.ui.diagram.IDiagramUtilHelper;
import gov.redhawk.ide.sad.graphiti.ui.diagram.patterns.AbstractFindByPattern;
import gov.redhawk.ide.sad.graphiti.ui.diagram.providers.SADDiagramTypeProvider;
import gov.redhawk.sca.efs.ScaFileSystemPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import mil.jpeojtrs.sca.partitioning.ComponentFile;
import mil.jpeojtrs.sca.partitioning.FindBy;
import mil.jpeojtrs.sca.partitioning.FindByStub;
import mil.jpeojtrs.sca.partitioning.ProvidesPortStub;
import mil.jpeojtrs.sca.partitioning.UsesPortStub;
import mil.jpeojtrs.sca.sad.HostCollocation;
import mil.jpeojtrs.sca.sad.SadComponentInstantiation;
import mil.jpeojtrs.sca.sad.SadComponentPlacement;
import mil.jpeojtrs.sca.sad.SadConnectInterface;
import mil.jpeojtrs.sca.sad.SoftwareAssembly;
import mil.jpeojtrs.sca.util.ScaResourceFactoryUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IAreaContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.features.context.impl.CreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.UpdateContext;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.internal.datatypes.impl.DimensionImpl;
import org.eclipse.graphiti.mm.Property;
import org.eclipse.graphiti.mm.PropertyContainer;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramLink;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.services.GraphitiUi;

public class DUtil { //SUPPRESS CHECKSTYLE INLINE

	// These are property key/value pairs that help us resize an existing shape by properly identifying
	// graphicsAlgorithms
	public static final String GA_TYPE = "GAType"; // key for gA types

	// Property key/value pairs help us identify Shapes to enable/disable user actions (move, resize, delete, remove
	// etc.)
	public static final String SHAPE_TYPE = "ShapeType"; // key for Shape types

	public static final int DIAGRAM_SHAPE_HORIZONTAL_PADDING = 100;
	public static final int DIAGRAM_SHAPE_SIBLING_VERTICAL_PADDING = 50;
	public static final int DIAGRAM_SHAPE_ROOT_VERTICAL_PADDING = 50;

	// do this because we need to pass it to layout diagram, assumes we already have shapes drawn of a certain
	// size and that we are just moving them
	public static IDimension calculateDiagramBounds(Diagram diagram) {

		// get all shapes in diagram, components, findby's etc
		List<RHContainerShape> rootShapes = new ArrayList<RHContainerShape>();
		for (Shape s : diagram.getChildren()) {
			// RHContainerShape
			if (s instanceof RHContainerShape) {
				RHContainerShape rhContainerShape = (RHContainerShape) s;
				// if it has no provides ports or it has ports WITH NO CONNECTIONS than its a root in the tree
				if (rhContainerShape.getProvidesPortStubs() != null
					&& (rhContainerShape.getProvidesPortStubs().size() < 1 || getIncomingConnectionsContainedInContainerShape(rhContainerShape).size() < 1)) {
					rootShapes.add(rhContainerShape);
				}
			}
		}

		// combine dimensions of each root tree to determine total dimension required to house all shapes in diagram
		int height = 0;
		int width = 0;
		for (RHContainerShape s : rootShapes) {
			IDimension childTreeDimension = calculateTreeDimensions(s);
			height += childTreeDimension.getHeight();
			// use largest width
			width = (childTreeDimension.getWidth() > width) ? childTreeDimension.getWidth() : width;
		}
		// add padding between roots
		height += DIAGRAM_SHAPE_ROOT_VERTICAL_PADDING * rootShapes.size() - 1;

		return new DimensionImpl(width, height);
	}

	/**
	 * Returns dimensions required to contain all shapes aligned in a horizontal tree diagram
	 * beginning with the provided root shape: rhContainerShape
	 * @param rhContainerShape
	 * @return
	 */
	@SuppressWarnings("restriction")
	public static IDimension calculateTreeDimensions(RHContainerShape rhContainerShape) {

		List<Connection> outs = getOutgoingConnectionsContainedInContainerShape(rhContainerShape);

		int height = rhContainerShape.getGraphicsAlgorithm().getHeight();
		int width = rhContainerShape.getGraphicsAlgorithm().getWidth();
		int childWidth = 0;
		int childHeight = 0;
		for (Connection conn : outs) {
			RHContainerShape targetRHContainerShape = RHContainerShapeImpl.findFromChild(conn.getEnd());
			IDimension childDimension = calculateTreeDimensions(targetRHContainerShape);
			childHeight += childDimension.getHeight() + DIAGRAM_SHAPE_SIBLING_VERTICAL_PADDING;
			// use largest width but don't add
			childWidth = (childDimension.getWidth() > childWidth) ? childDimension.getWidth() : childWidth;
		}
		if (outs.size() > 0) {
			width += childWidth + DIAGRAM_SHAPE_HORIZONTAL_PADDING;
		}
		// choose the largest of parent height or combined child height
		height = (childHeight > height) ? childHeight : height;

		return new DimensionImpl(width, height);
	}

	/**
	 * Return all incoming connections originating from within the provided ContainerShape
	 * @param containerShape
	 * @return
	 */
	public static List<Connection> getIncomingConnectionsContainedInContainerShape(ContainerShape containerShape) {
		List<Connection> connections = new ArrayList<Connection>();
		Diagram diagram = findDiagram(containerShape);
		for (Connection conn : diagram.getConnections()) {
			for (PictogramElement e : Graphiti.getPeService().getAllContainedPictogramElements(containerShape)) {
				if (e == conn.getEnd()) {
					connections.add(conn);
				}
			}
		}
		return connections;
	}

	/**
	 * Return all incoming connections originating from within the provided ContainerShape
	 * @param containerShape
	 * @return
	 */
	public static List<Connection> getOutgoingConnectionsContainedInContainerShape(ContainerShape containerShape) {
		List<Connection> connections = new ArrayList<Connection>();
		Diagram diagram = findDiagram(containerShape);
		for (Connection conn : diagram.getConnections()) {
			for (PictogramElement e : Graphiti.getPeService().getAllContainedPictogramElements(containerShape)) {
				if (e == conn.getStart()) {
					connections.add(conn);
				}
			}
		}
		return connections;
	}

	/**
	 * Returns the SoftwareAssembly for the provided diagram
	 * @param featureProvider
	 * @param diagram
	 * @return
	 */
	public static SoftwareAssembly getDiagramSAD(IFeatureProvider featureProvider, Diagram diagram) {

		return (SoftwareAssembly) DUtil.getBusinessObject(diagram, SoftwareAssembly.class);

//		//Used to use this, doesn't work for all cases.  Keep around for other potential cases for now.
//		//You must use the same transactionalEditingDomain and associated resourceSet if you want save/undo/redo to work
//		//properly.  The Graphiti editor will try saving the resourceSet and therefore we want our model to be in the same resourceSet.
//		//The editingDomain below isn't associated with Graphiti model and so it doesn't save the model when the diagram editor saves.
//		TransactionalEditingDomain editingDomain = featureProvider.getDiagramTypeProvider().getDiagramEditor().getEditingDomain();
////kepler		TransactionalEditingDomain editingDomain = featureProvider.getDiagramTypeProvider().getDiagramBehavior().getEditingDomain();
//		ResourceSet resourceSet = editingDomain.getResourceSet();
//		URI uri = diagram.eResource().getURI();
//		uri = uri.trimFragment().trimFileExtension().appendFileExtension("sad.xml");
//		SoftwareAssembly sad = SoftwareAssembly.Util.getSoftwareAssembly(resourceSet.getResource(uri, true));
//		
//		return sad;
	}

	/**
	 * Returns true if the provided context contains a pictogram element with one of the provided property values.
	 * False otherwise.
	 * @param context
	 * @param propertyKeys
	 * @return
	 */
	public static boolean doesPictogramContainProperty(PictogramElement pe, String[] propertyValues) {
		if (pe != null && pe.getProperties() != null) {
			for (Property p : pe.getProperties()) {
				for (String propValue : propertyValues) {
					if (p.getValue().equals(propValue)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Returns true if the provided context contains a pictogram element with one of the provided property values.
	 * False otherwise.
	 * @param context
	 * @param propertyKeys
	 * @return
	 */
	public static boolean doesPictogramContainProperty(IPictogramElementContext context, String[] propertyValues) {
		PictogramElement pe = context.getPictogramElement();
		return doesPictogramContainProperty(pe, propertyValues);
	}

	/**
	 * Returns all of the shape children recursively
	 * @param diagramElement
	 * @return
	 */
	public static List<Shape> collectShapeChildren(Shape diagramElement) {

		List<Shape> children = new ArrayList<Shape>();
		children.add(diagramElement);
		// if containershape, collect children recursively
		if (diagramElement instanceof ContainerShape) {
			ContainerShape cs = (ContainerShape) diagramElement;
			for (Shape c : cs.getChildren()) {
				children.addAll(collectShapeChildren(c));
			}
		}
		return children;
	}

	/**
	 * Returns all of the children contained within the provided PropertyContainer and their children recursively.
	 * @param diagramElement
	 * @return
	 */
	public static List<PropertyContainer> collectPropertyContainerChildren(PropertyContainer diagramElement) {

		List<PropertyContainer> children = new ArrayList<PropertyContainer>();
		children.add(diagramElement);

		// if containershape, collect children recursively
		if (diagramElement instanceof ContainerShape) {
			ContainerShape cs = (ContainerShape) diagramElement;
			for (Shape c : cs.getChildren()) {
				children.addAll(collectPropertyContainerChildren(c));
			}
			for (Anchor a : cs.getAnchors()) {
				children.addAll(collectPropertyContainerChildren(a));
			}
			if (cs.getGraphicsAlgorithm() != null) {
				children.addAll(collectPropertyContainerChildren(cs.getGraphicsAlgorithm()));
			}
			// if containershape, collect children recursively
		} else if (diagramElement instanceof GraphicsAlgorithm) {
			GraphicsAlgorithm ga = (GraphicsAlgorithm) diagramElement;
			for (GraphicsAlgorithm c : ga.getGraphicsAlgorithmChildren()) {
				children.addAll(collectPropertyContainerChildren(c));
			}
		} else if (diagramElement instanceof Shape) {
			Shape shape = (Shape) diagramElement;
			children.add(shape.getGraphicsAlgorithm());
		} else if (diagramElement instanceof AnchorContainer) {
			AnchorContainer anchorContainer = (AnchorContainer) diagramElement;
			for (Anchor a : anchorContainer.getAnchors()) {
				children.addAll(collectPropertyContainerChildren(a));
			}
		} else if (diagramElement instanceof Anchor) {
			Anchor anchor = (Anchor) diagramElement;
			children.add(anchor.getGraphicsAlgorithm());
		}

		return children;
	}

	/**
	 * Remove Business object from all linked PictogramElement
	 * @param diagram
	 * @param eObject
	 */
	public static void removeBusinessObjectFromAllPictogramElements(Diagram diagram, EObject eObject) {
		// get pe with link to bo
		List<PictogramElement> pictogramElements = Graphiti.getLinkService().getPictogramElements(diagram, eObject);

		// remove link
		for (PictogramElement pe : pictogramElements) {
			pe.getLink().getBusinessObjects().remove(eObject);
		}
	}

	/**
	 * Return first matched child with property value
	 * @param diagramElement
	 * @return
	 */
	public static PropertyContainer findFirstPropertyContainer(PropertyContainer diagramElement, String propertyValue) {

		PropertyContainer p = null;

		if (DUtil.isPropertyElementType(diagramElement, propertyValue)) {
			return diagramElement;
		}

		// if containershape, iterate through children recursively
		if (diagramElement instanceof ContainerShape) {
			ContainerShape cs = (ContainerShape) diagramElement;
			for (Shape c : cs.getChildren()) {
				p = findFirstPropertyContainer(c, propertyValue);
				if (p != null) {
					return p;
				}
			}
			if (cs.getGraphicsAlgorithm() != null) {
				p = findFirstPropertyContainer(cs.getGraphicsAlgorithm(), propertyValue);
				if (p != null) {
					return p;
				}
			}
			// if GraphicsAlgorithm, iterate through children recursively
		} else if (diagramElement instanceof GraphicsAlgorithm) {
			GraphicsAlgorithm ga = (GraphicsAlgorithm) diagramElement;
			for (GraphicsAlgorithm c : ga.getGraphicsAlgorithmChildren()) {
				p = findFirstPropertyContainer(c, propertyValue);
				if (p != null) {
					return p;
				}
			}
		} else if (diagramElement instanceof Shape) {
			Shape shape = (Shape) diagramElement;
			if (DUtil.isPropertyElementType(shape.getGraphicsAlgorithm(), propertyValue)) {
				return shape.getGraphicsAlgorithm();
			}
		}

		return null;
	}

	/**
	 * Returns the ancestor (parent chain) of the provided diagramElement with the provided PropertyContainer
	 * @param diagramElement
	 * @return
	 */
	public static ContainerShape findContainerShapeParentWithProperty(Shape shape, String propertyValue) {

		if (shape instanceof Diagram) {
			return null;
		}
		if (shape instanceof ContainerShape && DUtil.isPropertyElementType(shape, propertyValue)) {
			return (ContainerShape) shape;
		}
		if (DUtil.isPropertyElementType(shape.getContainer(), propertyValue)) {
			return shape.getContainer();
		}
		return findContainerShapeParentWithProperty(shape.getContainer(), propertyValue);

	}

	/**
	 * Returns the ancestor (parent chain) of the provided diagramElement with the provided PropertyContainer
	 * First checks self to see if it is a container with matching property
	 * @param diagramElement
	 * @return
	 */
	public static ContainerShape findContainerShapeParentWithProperty(PictogramElement pe, String propertyValue) {
		if (pe instanceof ContainerShape && DUtil.isPropertyElementType(pe, propertyValue)) {
			return (ContainerShape) pe;
		}
		PictogramElement peContainer = Graphiti.getPeService().getActiveContainerPe(pe);
		if (peContainer instanceof ContainerShape) {
			ContainerShape outerContainerShape = DUtil.findContainerShapeParentWithProperty((ContainerShape) peContainer, propertyValue);
			return outerContainerShape;
		}
		return null;
	}

	/**
	 * Returns list of Shapes that are contained in selected diagram context area
	 * @param diagram
	 * @param context
	 * @return
	 */
	public static List<Shape> getContainersInArea(final Diagram diagram, final IAreaContext context) {

		List<Shape> retList = new ArrayList<Shape>();

		EList<Shape> shapes = diagram.getChildren();
		for (Shape s : shapes) {
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();
			if (context.getX() <= ga.getX() && context.getWidth() >= ga.getWidth() && context.getY() <= ga.getY() && context.getHeight() >= ga.getHeight()) {
				retList.add(s);
			}
		}
		return retList;
	}

	/**
	 * Returns list of ContainerShape in provided AreaContext with
	 * property key DiagramUtil.GA_TYPE and provided propertyValue
	 * @param containerShape
	 * @param context
	 * @return
	 */
	public static List<Shape> getContainersInArea(final ContainerShape containerShape, final IAreaContext context, String propertyValue) {

		List<Shape> retList = new ArrayList<Shape>();

		EList<Shape> shapes = containerShape.getChildren();
		for (Shape s : shapes) {
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();
			if (gaExistInArea(ga, context) && DUtil.isPropertyElementType(ga, propertyValue)) {
				retList.add(s);
			}
		}
		return retList;
	}

	/**
	 * Returns list of ContainerShape outside of provided AreaContext on containerShape with
	 * property key DiagramUtil.GA_TYPE and provided propertyValue
	 * @param containerShape
	 * @param context
	 * @return
	 */
	public static List<Shape> getContainersOutsideArea(final ContainerShape containerShape, final IAreaContext context, String propertyValue) {

		List<Shape> retList = new ArrayList<Shape>();

		EList<Shape> shapes = containerShape.getChildren();
		for (Shape s : shapes) {
			GraphicsAlgorithm ga = s.getGraphicsAlgorithm();
			if (!gaFitsInParentGA(ga, context) && DUtil.isPropertyElementType(ga, propertyValue)) {
				retList.add(s);
			}
		}
		return retList;
	}

	/**
	 * Return true if GraphicsAlgorithm exists within IAreaContext
	 * @param ga
	 * @param context
	 * @return
	 */
	public static boolean gaExistInArea(final GraphicsAlgorithm ga, final IAreaContext context) {
		if (context.getX() <= ga.getX() && context.getWidth() >= ga.getWidth() && context.getY() <= ga.getY() && context.getHeight() >= ga.getHeight()) {
			return true;
		}
		return false;
	}

	/**
	 * Return true if GraphicsAlgorithm fits within parent. Context represents the parent width/height
	 * @param ga
	 * @param context
	 * @return
	 */
	public static boolean gaFitsInParentGA(final GraphicsAlgorithm childGA, final IAreaContext context) {
		if (context.getWidth() >= childGA.getX() && context.getHeight() >= childGA.getY()) {
			return true;
		}
		return false;
	}

	// returns width required to support longest provides port name
	// 4 used as minimum, characters cut off otherwise
	public static int getLongestProvidesPortWidth(final EList<ProvidesPortStub> providesPortStubs, Diagram diagram) {
		String longest = "four";
		if (providesPortStubs != null) {
			for (final ProvidesPortStub provides : providesPortStubs) {
				if (provides.getName().length() > longest.length()) {
					longest = provides.getName();
				}
			}
		}

		IDimension requiredWidth = GraphitiUi.getUiLayoutService().calculateTextSize(longest, StyleUtil.getPortFont(diagram));

		return requiredWidth.getWidth() + 5; // 5 gives us some breathing room
	}

	// returns width required to support longest uses port name
	// 4 used as minimum, characters cut off otherwise
	public static int getLongestUsesPortWidth(final EList<UsesPortStub> usesPortsStubs, Diagram diagram) {
		String longest = "four";
		if (usesPortsStubs != null) {
			for (final UsesPortStub uses : usesPortsStubs) {
				if (uses.getName().length() > longest.length()) {
					longest = uses.getName();
				}
			}
		}

		IDimension requiredWidth = GraphitiUi.getUiLayoutService().calculateTextSize(longest, StyleUtil.getPortFont(diagram));

		return requiredWidth.getWidth() + 20;
	}

	/**
	 * Returns true if the property container contains a property key DiagramUtil.GA_TYPE and with propertyValue as
	 * value
	 * @param pc
	 * @param propertyValue
	 * @return
	 */
	public static boolean isPropertyElementType(PropertyContainer pc, String propertyValue) {
		for (Property p : pc.getProperties()) {
			if ((GA_TYPE.equals(p.getKey()) || SHAPE_TYPE.equals(p.getKey())) && propertyValue.equals(p.getValue())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all ContainerShapes with the provided property value
	 * @param containerShape
	 * @param propertyValue
	 * @return
	 */
	public static List<ContainerShape> getAllContainerShapes(ContainerShape containerShape, String propertyValue) {
		List<ContainerShape> children = new ArrayList<ContainerShape>();
		if (containerShape instanceof ContainerShape && isPropertyElementType(containerShape, propertyValue)) {
			children.add(containerShape);
		} else {
			for (Shape s : containerShape.getChildren()) {
				if (s instanceof ContainerShape) {
					children.addAll(getAllContainerShapes((ContainerShape) s, propertyValue));
				}
			}
		}
		return children;
	}

	/**
	 * Returns true if Pictogram Link contains an object of the provided Class
	 * @param <T>
	 * @param link
	 * @param cls
	 * @return
	 */
	public static < T > boolean doesLinkContainObjectTypeInstance(PictogramLink link, Class<T> cls) {
		if (link != null) {
			for (EObject eObj : link.getBusinessObjects()) {
				if (cls.isInstance(eObj)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Add PictogramElement via feature for the provided object.
	 * Relies on the framework determining which feature should be used and whether it can be added to diagram
	 * @param featureProvider
	 * @param object
	 * @return
	 */
	public static PictogramElement addShapeViaFeature(IFeatureProvider featureProvider, Diagram diagram, Object object) {
		AddContext addContext = new AddContext();
		addContext.setNewObject(object);
		addContext.setTargetContainer(diagram);
		addContext.setX(0);
		addContext.setY(0);
		IAddFeature addFeature = featureProvider.getAddFeature(addContext);
		if (addFeature.canAdd(addContext)) {
			return addFeature.add(addContext);
		}
		return null;
	}

	/**
	 * Update PictogramElement via feature
	 * Relies on the framework determining which feature should be used and whether it can be added to diagram
	 * @param featureProvider
	 * @param pe
	 * @return
	 */
	public static boolean updateShapeViaFeature(IFeatureProvider featureProvider, Diagram diagram, PictogramElement pe) {
		UpdateContext updateContext = new UpdateContext(pe);
		IUpdateFeature updateFeature = featureProvider.getUpdateFeature(updateContext);
		if (updateFeature.canUpdate(updateContext)) {
			return updateFeature.update(updateContext);
		}
		return false;
	}

	/**
	 * Add PictogramElement Connection via feature for the provided object and anchors.
	 * Relies on the framework determining which feature should be used and whether it can be added to diagram
	 * @param featureProvider
	 * @param object
	 * @param sourceAnchor
	 * @param targetAnchor
	 * @return
	 */
	public static PictogramElement addConnectionViaFeature(IFeatureProvider featureProvider, Object object, Anchor sourceAnchor, Anchor targetAnchor) {
		AddConnectionContext addConnectionContext = new AddConnectionContext(sourceAnchor, targetAnchor);
		addConnectionContext.setNewObject(object);
		IAddFeature addFeature = featureProvider.getAddFeature(addConnectionContext);
		if (addFeature.canAdd(addConnectionContext)) {
			return addFeature.add(addConnectionContext);
		}
		return null;
	}

	/**
	 * Returns Business object of specified class type if it exists
	 * @param pe
	 * @param cls
	 * @return
	 */
	public static < T > EObject getBusinessObject(PictogramElement pe, Class<T> cls) {
		if (pe != null && pe.getLink() != null) {
			for (EObject eObj : pe.getLink().getBusinessObjects()) {
				if (cls.isInstance(eObj)) {
					return eObj;
				}
			}
		}
		return null;
	}

	/**
	 * Examines a list of PictogramElements (pes) and ensures there is an associated object in the model (objects).
	 * If the PictogramElement has an associated object than it is updated (if necessary) otherwise the PictogramElement
	 * is removed
	 * @param pes
	 * @param objects
	 * @param objectClass
	 * @param pictogramLabel
	 * @param featureProvider
	 * @param performUpdate
	 * @return
	 */
	public static Reason removeUpdatePictogramElement(List<PictogramElement> pes, List<EObject> objects, Class objectClass, String pictogramLabel,
		IFeatureProvider featureProvider, boolean performUpdate) {

		boolean updateStatus = false;

		// update PictogramElements if in model, if not in model remove from diagram
		for (Iterator<PictogramElement> peIter = pes.iterator(); peIter.hasNext();) {
			// in model?
			PictogramElement pe = peIter.next();
			boolean found = false;
			for (Object obj : objects) {
				if (obj.equals(DUtil.getBusinessObject(pe, objectClass))) {
					found = true;
					// update Shape
					featureProvider.updateIfPossible(new UpdateContext(pe));
				}
			}
			if (!found) {
				// wasn't found, deleting shape
				if (performUpdate) {
					updateStatus = true;
					// delete shape
					peIter.remove();
					EcoreUtil.delete(pe, true);
				} else {
					return new Reason(true, "A " + pictogramLabel + " in diagram no longer has an associated business object");
				}
			}
		}

		if (updateStatus && performUpdate) {
			return new Reason(true, "Update successful");
		}

		return new Reason(false, "No updates required");
	}

	/**
	 * Examines a list of Shapes and ensures there is an associated object in the model (objects).
	 * If the Shape has an associated object than it is updated (if necessary) otherwise the Shape is removed
	 * Next if there are new objects that do not have Shapes, then a new Shape is added using
	 * the features associated the provided object type.
	 * @param shapes
	 * @param objects
	 * @param pictogramLabel
	 * @param featureProvider
	 * @param performUpdate
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Reason addRemoveUpdateShapes(List<Shape> shapes, List<EObject> objects, Class objectClass, String pictogramLabel, Diagram diagram,
		IFeatureProvider featureProvider, boolean performUpdate) {

		boolean updateStatus = false;

		// remove shapes on diagram if no longer in model, update all other shapes if necessary
		Reason updateShapesReason = removeUpdatePictogramElement((List<PictogramElement>) (List< ? >) shapes, objects, objectClass, pictogramLabel,
			featureProvider, performUpdate);
		if (!performUpdate && updateShapesReason.toBoolean()) {
			return updateShapesReason;
		} else if (updateShapesReason.toBoolean() == true) {
			updateStatus = true;
		}

		// add Shapes found in model, but not in diagram
		for (Object obj : objects) {
			// in diagram
			boolean found = false;
			for (Shape pe : shapes) {
				if (obj.equals(DUtil.getBusinessObject(pe, objectClass))) {
					found = true;
				}
			}
			if (!found) {
				// wasn't found, add shape
				if (performUpdate) {
					updateStatus = true;
					// add shape
					DUtil.addShapeViaFeature(featureProvider, diagram, obj);
				} else {
					return new Reason(true, "A " + pictogramLabel + " in model isn't displayed in diagram");
				}
			}
		}

		if (updateStatus && performUpdate) {
			return new Reason(true, "Update successful");
		}

		return new Reason(false, "No updates required");
	}

	/**
	 * Iterates through all sadConnectInterfaces to look for connections that use findBy tag that are not contained
	 * within ComponentInterface or Provides
	 * All FindBy tags without one of the two parents will be contained within a ComponentInterface afterwards. The
	 * purpose of this is to draw connections
	 * to the lollipop port in the diagram as apposed to directly connecting to the FindBy Box.
	 * @param sadConnectInterfaces
	 * @param objectClass
	 * @param pictogramLabel
	 * @param diagram
	 * @param featureProvider
	 * @param performUpdate
	 * @return
	 */
	public static Reason replaceDirectFindByConnection(List<SadConnectInterface> sadConnectInterfaces, Class objectClass, String pictogramLabel,
		Diagram diagram, IFeatureProvider featureProvider, boolean performUpdate) {

		boolean updateStatus = false;

		// iterate through all connections
		for (ListIterator<SadConnectInterface> iter = sadConnectInterfaces.listIterator(); iter.hasNext();) {
			SadConnectInterface sadConnectInterface = iter.next();

			if (sadConnectInterface.getFindBy() != null && sadConnectInterface.getFindBy() instanceof FindBy) {

				// The Graphiti SAD diagram doesn't support this type of connection and therefore it will not exist
				// in the diagram, it may however exist in the model and we will convert it to use
				// ComponentSupportedInterface

				// lookup source anchor
				Anchor sourceAnchor = lookupSourceAnchor(sadConnectInterface, diagram);

				// if sourceAnchor wasn't found its because the findBy needs to be added to the diagram
				if (sourceAnchor == null) {

					// FindBy is always used inside usesPort
					if (sadConnectInterface.getUsesPort() != null && sadConnectInterface.getUsesPort().getFindBy() != null) {
						if (performUpdate) {
							updateStatus = true;
							FindBy findBy = (FindBy) sadConnectInterface.getUsesPort().getFindBy();

							// create FindBy Shape for Source
							FindByStub findByStub = AbstractFindByPattern.createFindByStub(findBy, featureProvider, diagram);

							// add FindBYShape for Target
							if (findByStub != null) {
								PictogramElement pe = DUtil.addShapeViaFeature(featureProvider, diagram, findByStub);
							}
						} else {
							return new Reason(true, "Add FindBy Shape");
						}
					}
				}

				// The model isn't providing us with an interface for the FindBy. First step is to modify the model and
				// select an interface
				FindBy findBy = (FindBy) sadConnectInterface.getFindBy();

				// create FindBy Shape for Target
				FindByStub findByStub = AbstractFindByPattern.createFindByStub(findBy, featureProvider, diagram);

				// add FindBYShape for Target
				PictogramElement pe = DUtil.addShapeViaFeature(featureProvider, diagram, findByStub);

				Anchor targetAnchor = null;
				if (pe != null && pe instanceof RHContainerShape) {
					// determine lollipop anchor
					PictogramElement peAnchor = DUtil.getPictogramElementForBusinessObject(diagram, (EObject) findByStub.getInterface(), Anchor.class);
					targetAnchor = (Anchor) peAnchor;
				}

				// create a new connection and remove the connection we are currently analyzing
				if (sourceAnchor == null || targetAnchor == null) {
					// TODO: we might want to alert user of this...log maybe
					break;
				}

				// remove the current connection, and replace it with this new connection
				// remove
				iter.remove();

				// create
				CreateConnectionContext createContext = new CreateConnectionContext();
				createContext.setSourceAnchor(sourceAnchor);
				createContext.setTargetAnchor(targetAnchor);
				for (ICreateConnectionFeature createFeature : featureProvider.getCreateConnectionFeatures()) {
					if (createFeature.canCreate(createContext)) {
						featureProvider.getDiagramTypeProvider().getDiagramBehavior().executeFeature(createFeature, createContext);
					}
				}
			}
		}

		if (updateStatus && performUpdate) {
			return new Reason(true, "Update successful");
		}

		return new Reason(false, "No updates required");
	}

	/**
	 * Lookup SourceAnchor for connection. Examines uses ports on Components as well as FindBys
	 * @param sadConnectInterface
	 * @param diagram
	 * @return
	 */
	public static Anchor lookupSourceAnchor(SadConnectInterface sadConnectInterface, Diagram diagram) {
		// lookup sourceAnchor
		PictogramElement sourceAnchorPe = DUtil.getPictogramElementForBusinessObject(diagram, sadConnectInterface.getSource(), Anchor.class);
		if (sourceAnchorPe != null) {
			return (Anchor) sourceAnchorPe;
		} else {
			// All components have been created so source Anchor is likely null because provides findBy
			// <uses><findby>something</findby></uses>
			// or something is wrong with the xml
			if (sadConnectInterface.getUsesPort() != null && sadConnectInterface.getUsesPort().getFindBy() != null) {
				FindBy findBy = (FindBy) sadConnectInterface.getUsesPort().getFindBy();

				// iterate through all FindByStub objects stored in diagram and set sourceAnchor that matches findBy
				List<RHContainerShape> findByContainerShapes = AbstractFindByPattern.getAllFindByShapes(diagram);
				for (RHContainerShape findByShape : findByContainerShapes) {
					FindByStub findByStub = (FindByStub) DUtil.getBusinessObject(findByShape);

					// determine findBy match
					if (AbstractFindByPattern.doFindByObjectsMatch(findBy, findByStub)) {

						// determine which usesPortStub we are targeting
						UsesPortStub usesPortStub = null;
						for (UsesPortStub p : findByStub.getUses()) {
							if (p != null && sadConnectInterface.getUsesPort().getUsesIndentifier() != null
								&& p.getName().equals(sadConnectInterface.getUsesPort().getUsesIndentifier())) {
								usesPortStub = p;
							}
						}

						// determine port anchor for FindByMatch
						if (usesPortStub != null) {
							PictogramElement pe = DUtil.getPictogramElementForBusinessObject(diagram, usesPortStub, Anchor.class);
							return (Anchor) pe;
						}
					}
				}

			}
		}
		return null;
	}

	/**
	 * Search for the FindByStub in the diagram given the findBy object
	 * @param findBy
	 * @param diagram
	 * @return
	 */
	public static FindByStub findFindByStub(FindBy findBy, Diagram diagram) {
		for (RHContainerShape findByShape : AbstractFindByPattern.getAllFindByShapes(diagram)) {
			FindByStub findByStub = (FindByStub) DUtil.getBusinessObject(findByShape);
			// determine findBy match
			if (findByStub != null && AbstractFindByPattern.doFindByObjectsMatch(findBy, findByStub)) {
				// it matches
				return findByStub;
			}
		}
		return null;
	}

	/**
	 * Returns the PictogramElement of class pictogramClass who is linked to business object eObj
	 * @param diagram
	 * @param eObj
	 * @param pictogramClass
	 * @return
	 */
	public static < T > PictogramElement getPictogramElementForBusinessObject(Diagram diagram, EObject eObj, Class<T> pictogramClass) {
		List<PictogramElement> pes = Graphiti.getLinkService().getPictogramElements(diagram, eObj);
		if (pes != null && pes.size() > 0) {
			for (PictogramElement p : pes) {
				if (pictogramClass.isInstance(p)) {
					return p;
				}
			}
		}
		return null;
	}

	/**
	 * Return true if target is HostCollocation ContainerShape
	 * @param context
	 */
	public static HostCollocation getHostCollocation(final ContainerShape targetContainerShape) {
		if (targetContainerShape instanceof ContainerShape) {
			if (targetContainerShape.getLink() != null && targetContainerShape.getLink().getBusinessObjects() != null) {
				for (EObject obj : targetContainerShape.getLink().getBusinessObjects()) {
					if (obj instanceof HostCollocation) {
						return (HostCollocation) obj;
					}
				}
			}
		}
		return null;
	}

	// convenient method for getting diagram for a ContainerShape
	public static Diagram findDiagram(ContainerShape containerShape) {
		return Graphiti.getPeService().getDiagramForShape(containerShape);
	}

	// convenient method for getting business object for PictogramElement
	public static Object getBusinessObject(PictogramElement pe) {
		return GraphitiUi.getLinkService().getBusinessObjectForLinkedPictogramElement(pe);
	}

	/**
	 * Delete SadComponentInstantiation and corresponding SadComponentPlacement business object from SoftwareAssembly
	 * This method should be executed within a RecordingCommand.
	 * @param ciToDelete
	 * @param diagram
	 */
	public static void deleteComponentInstantiation(final SadComponentInstantiation ciToDelete, final SoftwareAssembly sad) {

		// assembly controller may reference componentInstantiation
		// delete reference if applicable
		if (sad.getAssemblyController() != null && sad.getAssemblyController().getComponentInstantiationRef() != null
			&& sad.getAssemblyController().getComponentInstantiationRef().getInstantiation().equals(ciToDelete)) {
			// TODO: how should this be handled? We need to test this out
			EcoreUtil.delete(sad.getAssemblyController().getComponentInstantiationRef());
			sad.getAssemblyController().setComponentInstantiationRef(null);
		}

		// get placement for instantiation and delete it from sad partitioning after we look at removing the component
		// file ref.
		SadComponentPlacement placement = (SadComponentPlacement) ciToDelete.getPlacement();

		// find and remove any attached connections
		// gather connections
		List<SadConnectInterface> connectionsToRemove = new ArrayList<SadConnectInterface>();
		if (sad.getConnections() != null) {
			for (SadConnectInterface connectionInterface : sad.getConnections().getConnectInterface()) {
				// we need to do thorough null checks here because of the many connection possibilities. Firstly a
				// connection requires only a usesPort and either (providesPort || componentSupportedInterface)
				// and therefore null checks need to be performed.
				// FindBy connections don't have ComponentInstantiationRefs and so they can also be null
				if ((connectionInterface.getComponentSupportedInterface() != null
					&& connectionInterface.getComponentSupportedInterface().getComponentInstantiationRef() != null && ciToDelete.getId().equals(
					connectionInterface.getComponentSupportedInterface().getComponentInstantiationRef().getRefid()))
					|| (connectionInterface.getUsesPort() != null && connectionInterface.getUsesPort().getComponentInstantiationRef() != null && ciToDelete.getId().equals(
						connectionInterface.getUsesPort().getComponentInstantiationRef().getRefid()))
					|| (connectionInterface.getProvidesPort() != null && connectionInterface.getProvidesPort().getComponentInstantiationRef() != null && ciToDelete.getId().equals(
						connectionInterface.getProvidesPort().getComponentInstantiationRef().getRefid()))) {
					connectionsToRemove.add(connectionInterface);
				}
			}
		}
		// remove gathered connections
		if (sad.getConnections() != null) {
			sad.getConnections().getConnectInterface().removeAll(connectionsToRemove);
		}

		// delete component file if applicable
		// figure out which component file we are using and if no other component placements using it then remove it.
		ComponentFile componentFileToRemove = placement.getComponentFileRef().getFile();
		for (SadComponentPlacement p : sad.getPartitioning().getComponentPlacement()) {
			if (p != placement && p.getComponentFileRef().getRefid().equals(placement.getComponentFileRef().getRefid())) {
				componentFileToRemove = null;
			}
		}
		if (componentFileToRemove != null) {
			sad.getComponentFiles().getComponentFile().remove(componentFileToRemove);
		}

		// delete component placement
		sad.getPartitioning().getComponentPlacement().remove(placement);
	}

	public static URI getDiagramResourceURI(final IDiagramUtilHelper options, final Resource resource) throws IOException {
		if (resource != null) {
			final URI uri = resource.getURI();
			if (uri.isPlatformResource()) {
				final IFile file = options.getResource(resource);
				return DUtil.getRelativeDiagramResourceURI(options, file);
			} else {
				return DUtil.getTemporaryDiagramResourceURI(options, uri);
			}
		}
		return null;
	}

	/**
	 * Initialize sad diagram.
	 * 
	 * @param b
	 */
	private static URI getRelativeDiagramResourceURI(final IDiagramUtilHelper options, final IFile file) {
		final IFile diagramFile = file.getParent().getFile(
			new Path(file.getName().substring(0, file.getName().length() - options.getSemanticFileExtension().length()) + options.getDiagramFileExtension()));
		final URI uri = URI.createPlatformResourceURI(diagramFile.getFullPath().toString(), true);
		return uri;
	}

	/**
	 * Initialize sad diagram.
	 * 
	 * @param b
	 * @throws IOException
	 */
	private static URI getTemporaryDiagramResourceURI(final IDiagramUtilHelper options, final URI uri) throws IOException {
		final String name = uri.lastSegment();
		String tmpName = "rh_" + name.substring(0, name.length() - options.getSemanticFileExtension().length());
		File tempDir = ScaFileSystemPlugin.getDefault().getTempDirectory();
		final File tempFile = File.createTempFile(tmpName, options.getDiagramFileExtension(), tempDir);
		tempFile.deleteOnExit();

		final URI retVal = URI.createURI(tempFile.toURI().toString());

		return retVal;
	}

	/**
	 * 
	 */
	public static void initializeDiagramResource(final IDiagramUtilHelper options, final URI diagramURI, final Resource sadResource) throws IOException,
		CoreException {
		if (diagramURI.isPlatform()) {
			final IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(diagramURI.toPlatformString(true)));

			file.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());

			if (!file.exists()) {
				final IWorkspaceRunnable operation = new IWorkspaceRunnable() {

					@Override
					public void run(final IProgressMonitor monitor) throws CoreException {
						final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
						try {
							DUtil.populateDiagram(options, diagramURI, sadResource, buffer);
						} catch (final IOException e) {
							// PASS
						}
						file.create(new ByteArrayInputStream(buffer.toByteArray()), true, monitor);
					}

				};
				final ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRuleFactory().createRule(file);

				ResourcesPlugin.getWorkspace().run(operation, rule, 0, null);
			}
		} else {
			DUtil.populateDiagram(options, diagramURI, sadResource, null);
		}
	}

	// creates new diagram from provided model resource
	private static void populateDiagram(final IDiagramUtilHelper options, final URI diagramURI, final Resource resource, final OutputStream buffer)
		throws IOException {

		// Create a resource set
		final ResourceSet resourceSet = ScaResourceFactoryUtil.createResourceSet();

		// Create a resource for this file.
		final Resource diagramResource = resourceSet.createResource(diagramURI);

		// extract name for diagram from uri
		final String diagramName = diagramURI.lastSegment();

		// create diagram
		Diagram diagram = Graphiti.getPeCreateService().createDiagram(SADDiagramTypeProvider.DIAGRAM_TYPE_ID, diagramName, true);
		diagramResource.getContents().add(diagram);

		// TODO:we will want to move this logic somewhere else
		IDiagramTypeProvider dtp = GraphitiUi.getExtensionManager().createDiagramTypeProvider(diagram, SADDiagramTypeProvider.PROVIDER_ID);
		IFeatureProvider featureProvider = dtp.getFeatureProvider();

		// iterate over each component, both in an out of host collocations
		// passing in assembly controller and external ports information

		// WE WANT OUR UPDATE diagram capabilities to handle things like this though right?

		if (buffer != null) {
			diagramResource.save(buffer, options.getSaveOptions());
		} else {
			diagramResource.save(options.getSaveOptions());
		}
	}

	/**
	 * 
	 * @param resource
	 * @return
	 */
	public static boolean isDiagramLocalSandbox(final Resource resource) {
		return ".LocalSca.sad.xml".equals(resource.getURI().lastSegment());
	}

}
