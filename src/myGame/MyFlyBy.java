package myGame;

import com.jme3.input.FlyByCamera;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;

public class MyFlyBy extends FlyByCamera{
	
	public MyFlyBy(Camera cam) {
		super(cam);
	}
	
	@Override
	public void onAnalog(String name, float value, float tpf) {
		if (name.equals("FLYCAM_Left")||
			name.equals("FLYCAM_Right")||
			name.equals("FLYCAM_Up")||
			name.equals("FLYCAM_Down"))
				super.onAnalog(name, value, tpf);
		else
			if(isInsideViewport(inputManager.getCursorPosition()))
	    		super.onAnalog(name, value, tpf);
	}

	@Override
	 public void onAction(String name, boolean value, float tpf) {
	        if(isInsideViewport(inputManager.getCursorPosition())||!value)
	        		super.onAction(name, value, tpf);
	 }

	private boolean isInsideViewport(Vector2f cursorPosition) {
		/**LIKE MAH STYLE!*/
		if((cursorPosition.x/cam.getWidth()<=cam.getViewPortRight())&&
				(cursorPosition.x/cam.getWidth()>cam.getViewPortLeft())&&
					(cursorPosition.y/cam.getHeight()<=cam.getViewPortTop())&&
						(cursorPosition.y/cam.getHeight()>cam.getViewPortBottom()))			
							return true;
		
		return false;
	}
}
