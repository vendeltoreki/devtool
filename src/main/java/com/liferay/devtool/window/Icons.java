package com.liferay.devtool.window;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Icons {
	public static BufferedImage IMG_APP;
	public static BufferedImage IMG_PENDING;
	public static BufferedImage IMG_OK;
	public static BufferedImage IMG_ERROR;

	static {
		try {
			IMG_APP = ImageIO.read(new File("resources/icons/tool.png"));
			IMG_PENDING = ImageIO.read(new File("resources/icons/v_pending.png"));
			IMG_OK = ImageIO.read(new File("resources/icons/v_check.png"));
			IMG_ERROR = ImageIO.read(new File("resources/icons/v_error.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
