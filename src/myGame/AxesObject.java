package myGame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

public class AxesObject {
	
	private Node createGeometry(AssetManager assetManager) {
		
		/** Create meshes for the arms of the quadcopters */
		final Box box1 = new Box(Vector3f.UNIT_Z.mult(5.0f), 0.1f, 0.1f, 5f);
		final Box box2 = new Box(Vector3f.UNIT_X.mult(5.0f), 5f, 0.1f, 0.1f);
		final Box box3 = new Box(Vector3f.UNIT_Y.mult(5.0f), 0.1f,5f,0.1f);
		
		/** Build spatials from them */
		final Geometry arm1 = new Geometry("Arm1", box1);
		final Geometry arm2 = new Geometry("Arm2", box2);
		final Geometry arm3 = new Geometry("Arm3", box3);
		final Material material1 = new Material(assetManager,
				"Common/MatDefs/Misc/Unshaded.j3md");
		final Material material2 = new Material(assetManager,
				"Common/MatDefs/Misc/Unshaded.j3md");
		final Material material3 = new Material(assetManager,
				"Common/MatDefs/Misc/Unshaded.j3md");
		material1.setColor("Color", new ColorRGBA(1, 0, 0, 1));
		material2.setColor("Color", new ColorRGBA(0, 0, 1, 1));
		material3.setColor("Color", new ColorRGBA(0, 1, 0, 1));
		arm1.setMaterial(material1);
		arm2.setMaterial(material2);
		arm3.setMaterial(material3);

		/** Create the quadcopter spatial */
		final Node node = new Node("Axes");
		node.attachChild(arm1);
		node.attachChild(arm2);
		node.attachChild(arm3);
		return node;
	}

	private final Node	geometry;

	public AxesObject(AssetManager assetManager) {
		geometry = createGeometry(assetManager);
	}

	public Node getGeometry() {
		return geometry;
	}

}
