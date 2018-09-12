package qupath.lib.classifiers.pixel;

import qupath.lib.common.ColorTools;

public class PixelClassifierOutputChannel {
	
	public static final Integer TRANSPARENT = ColorTools.makeRGBA(255, 253, 254, 0);
	
	private String name;
	private Integer color;
	
	public PixelClassifierOutputChannel(String name, Integer color) {
		this.name = name;
		this.color = color;
	}
	
	/**
	 * Check if the color is 'transparent'; this is used for background/ignored channels.
	 * @return
	 */
	public boolean isTransparent() {
		return TRANSPARENT.equals(this.color);
	}
	
	/**
	 * Name of the output channel
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Color used to display the output channel
	 */
	public Integer getColor() {
		return color;
	}

}