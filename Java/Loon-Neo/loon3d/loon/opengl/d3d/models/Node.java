package loon.opengl.d3d.models;

import loon.geom.BoundingBox;
import loon.geom.Matrix4;
import loon.geom.Quaternion;
import loon.geom.Vector3f;
import loon.utils.TArray;

public class Node {
	
	public String id;

	public Node parent;

	public final TArray<Node> children = new TArray<Node>(2);
	/** Whether this node is currently being animated, if so the translation, rotation and scale values are not used. */
	public boolean isAnimated;
	/** the translation, relative to the parent, not modified by animations **/
	public final Vector3f translation = new Vector3f();
	/** the rotation, relative to the parent, not modified by animations **/
	public final Quaternion rotation = new Quaternion(0, 0, 0, 1);
	/** the scale, relative to the parent, not modified by animations **/
	public final Vector3f scale = new Vector3f(1, 1, 1);
	/** the local transform, based on translation/rotation/scale ({@link #calculateLocalTransform()}) or any applied animation **/
	public final Matrix4 localTransform = new Matrix4();
	/** the global transform, product of local transform and transform of the parent node, calculated via {@link #calculateWorldTransform()}**/
	public final Matrix4 globalTransform = new Matrix4();
	
	public TArray<NodePart> parts = new TArray<NodePart>(2);
	
	/**
	 * Calculates the local transform based on the translation, scale and rotation
	 * @return the local transform
	 */
	public Matrix4 calculateLocalTransform() {
		if (!isAnimated) {
			localTransform.idt();
			localTransform.translate(translation);
			localTransform.rotate(rotation);
			localTransform.scale(scale.x, scale.y, scale.z);
		}
		return localTransform;
	}

	/**
	 * Calculates the world transform; the product of local transform and the
	 * parent's world transform. 
	 * @return the world transform
	 */
	public Matrix4 calculateWorldTransform() {
		if (parent == null) 
			globalTransform.set(localTransform);
		else
			globalTransform.set(parent.globalTransform).mul(localTransform);
		return globalTransform;
	}
	
	/**
	 * Calculates the local and world transform of this node and optionally all
	 * its children.
	 * 
	 * @param recursive whether to calculate the local/world transforms for children.
	 */
	public void calculateTransforms(boolean recursive) {
		calculateLocalTransform();
		calculateWorldTransform();
		
		if(recursive) {
			for(Node child: children) {
				child.calculateTransforms(true);
			}
		}
	}
	
	public void calculateBoneTransforms(boolean recursive) {
		for (final NodePart part : parts) {
			if (part.invBoneBindTransforms == null || part.bones == null || part.invBoneBindTransforms.size != part.bones.length)
				continue;
			final int n = part.invBoneBindTransforms.size;
			for (int i = 0; i < n; i++)
				part.bones[i].set(part.invBoneBindTransforms.keys[i].globalTransform).mul(part.invBoneBindTransforms.values[i]);
		}
		if(recursive) {
			for(Node child: children) {
				child.calculateBoneTransforms(true);
			}
		}
	}
	
	/** Calculate the bounding box of this Node.
	 * This is a potential slow operation, it is advised to cache the result. */
	public BoundingBox calculateBoundingBox(final BoundingBox out) {
		out.inf();
		return extendBoundingBox(out);
	}
	
	/** Extends the bounding box with the bounds of this Node.
	 * This is a potential slow operation, it is advised to cache the result. */
	public BoundingBox extendBoundingBox(final BoundingBox out) {
		final int partCount = parts.size;
		for (int i = 0; i < partCount; i++) {
			final MeshPart meshPart = parts.get(i).meshPart;
			meshPart.mesh.extendBoundingBox(out, meshPart.indexOffset, meshPart.numVertices, globalTransform);
		}
		final int childCount = children.size;
		for (int i = 0; i < childCount; i++)
			children.get(i).extendBoundingBox(out);
		return out;
	}
	
	/** @param recursive false to fetch a root child only, true to search the entire node tree for the specified node.
	 * @return The node with the specified id, or null if not found. */
	public Node getChild(final String id, boolean recursive, boolean ignoreCase) {
		return getNode(children, id, recursive, ignoreCase);
	}
	
	/** Helper method to recursive fetch a node from an array
	 * @param recursive false to fetch a root node only, true to search the entire node tree for the specified node.
	 * @return The node with the specified id, or null if not found. */
	public static Node getNode(final TArray<Node> nodes, final String id, boolean recursive, boolean ignoreCase) {
		final int n = nodes.size;
		Node node;
		if (ignoreCase) {
			for (int i = 0; i < n; i++)
				if ((node = nodes.get(i)).id.equalsIgnoreCase(id))
					return node;
		} else {
			for (int i = 0; i < n; i++)
				if ((node = nodes.get(i)).id.equals(id))
					return node;
		}
		if (recursive) {
			for (int i = 0; i < n; i++)
				if ((node = getNode(nodes.get(i).children, id, true, ignoreCase)) != null)
					return node;
		}
		return null;
	}
}
