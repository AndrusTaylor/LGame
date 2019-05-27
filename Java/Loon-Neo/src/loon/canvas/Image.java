/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon.canvas;

import loon.BaseIO;
import loon.Graphics;
import loon.LRelease;
import loon.LSysException;
import loon.LSystem;
import loon.LTexture;
import loon.canvas.NinePatchAbstract.Repeat;
import loon.geom.Vector2f;
import loon.opengl.Painter;
import loon.opengl.TextureSource;
import loon.utils.ArrayByte;
import loon.utils.CollectionUtils;
import loon.utils.MathUtils;
import loon.utils.Scale;
import loon.utils.TArray;
import loon.utils.reply.Function;
import loon.utils.reply.GoFuture;

/**
 * 图像加载与处理的基础类,Texture由此类排生,内部为具体运行环境的等效对象封装.
 */
public abstract class Image extends TextureSource implements Canvas.Drawable, LRelease {

	private boolean isTexture = false;

	private boolean haveToClose = false;

	public boolean toClose() {
		return haveToClose;
	}

	public static Canvas createCanvas(float w, float h) {
		return createCanvas((int) w, (int) h);
	}

	public static Canvas createCanvas(int w, int h) {
		return LSystem.base().graphics().createCanvas(w, h);
	}

	public static Image createImage(float w, float h) {
		return createImage((int) w, (int) h);
	}

	public static Image createImage(int w, int h) {
		return LSystem.base().graphics().createCanvas(w, h).image;
	}

	public static Image createImageNicePatch(String path, int x, int y, int w, int h) {
		return createImageNicePatch(path, null, x, y, w, h);
	}

	public static Image createImageNicePatch(String path, Repeat repeat, int x, int y, int w, int h) {
		final ImageNinePatch np = new ImageNinePatch(createImage(path), repeat);
		Canvas c = Image.createCanvas(w, h);
		np.drawNinePatch(c, x, y, w, h);
		return c.image;
	}

	public static Image createImage(final String path) {
		return BaseIO.loadImage(path);
	}

	public static Image getResize(final Image image, float w, float h) {
		Canvas canvas = LSystem.base().graphics().createCanvas(w, h);
		canvas.draw(image, 0, 0, w, h, 0, 0, image.width(), image.height());
		return canvas.image;
	}

	public static Image drawClipImage(final Image image, int objectWidth, int objectHeight, int x1, int y1, int x2,
			int y2) {
		Canvas canvas = LSystem.base().graphics().createCanvas(objectWidth, objectHeight);
		canvas.draw(image, 0, 0, objectWidth, objectHeight, x1, y1, x2, y2);
		return canvas.image;
	}

	public static Image drawClipImage(final Image image, int objectWidth, int objectHeight, int x, int y) {
		Canvas canvas = LSystem.base().graphics().createCanvas(objectWidth, objectHeight);
		canvas.draw(image, 0, 0, objectWidth, objectHeight, x, y, x + objectWidth, objectHeight + y);
		return canvas.image;
	}

	Canvas canvas;

	public final GoFuture<Image> state;

	public Canvas getCanvas() {
		if (canvas == null) {
			return canvas = LSystem.base().graphics().createCanvas(width(), height());
		}
		return canvas;
	}

	@Override
	public boolean isLoaded() {
		return state.isCompleteNow();
	}

	public Image scale(float w, float h) {
		return Image.getResize(this, w, h);
	}

	public abstract Scale scale();

	@Override
	public float width() {
		return scale().invScaled(pixelWidth());
	}

	@Override
	public float height() {
		return scale().invScaled(pixelHeight());
	}

	public abstract int pixelWidth();

	public abstract int pixelHeight();

	public abstract Pattern createPattern(boolean repeatX, boolean repeatY);

	public Image setFormat(LTexture.Format config) {
		texconf = config;
		return this;
	}

	public LTexture texture() {
		if (texture == null || texture.disposed()) {
			texture = createTexture(texconf);
		}
		return texture;
	}

	public LTexture updateTexture() {
		if (texture == null || texture.disposed()) {
			texture = createTexture(texconf);
		} else {
			texture.update(this);
		}
		return texture;
	}

	public GoFuture<LTexture> textureAsync() {
		return state.map(new Function<Image, LTexture>() {
			@Override
			public LTexture apply(Image image) {
				return texture();
			}
		});
	}

	public LTexture createTexture(LTexture.Format config) {
		if (!isLoaded()) {
			throw new LSysException("Cannot create texture from unready image: " + this);
		}
		int texWidth = config.toTexWidth(pixelWidth());
		int texHeight = config.toTexHeight(pixelHeight());
		if (texWidth <= 0 || texHeight <= 0) {
			throw new LSysException("Invalid texture size: " + texWidth + "x" + texHeight + " from: " + this);
		}
		this.isTexture = true;
		LTexture tex = new LTexture(gfx, gfx.createTexture(config), config, texWidth, texHeight, scale(), width(),
				height());
		tex.update(this);
		return tex;
	}

	public static abstract class Region extends TextureSource implements Canvas.Drawable {
	}

	public Region region(final float rx, final float ry, final float rwidth, final float rheight) {

		return new Region() {
			private LTexture tile;

			@Override
			public boolean isLoaded() {
				return Image.this.isLoaded();
			}

			@Override
			public LTexture draw() {
				if (tile == null) {
					tile = Image.this.texture().copy(rx, ry, rwidth, rheight);
				}
				return tile;
			}

			@Override
			public GoFuture<Painter> tileAsync() {
				return Image.this.state.map(new Function<Image, Painter>() {
					public Painter apply(Image image) {
						return draw();
					}
				});
			}

			@Override
			public float width() {
				return rwidth;
			}

			@Override
			public float height() {
				return rheight;
			}

			@Override
			public void draw(Object ctx, float x, float y, float width, float height) {
				Image.this.draw(ctx, x, y, width, height, rx, ry, rwidth, rheight);
			}

			@Override
			public void draw(Object ctx, float dx, float dy, float dw, float dh, float sx, float sy, float sw,
					float sh) {
				Image.this.draw(ctx, dx, dy, dw, dh, rx + sx, ry + sy, sw, sh);
			}
		};
	}

	public static interface BitmapTransformer {
	}

	public abstract Image transform(BitmapTransformer xform);

	@Override
	public Painter draw() {
		return texture();
	}

	@Override
	public GoFuture<Painter> tileAsync() {
		return state.map(new Function<Image, Painter>() {
			public Painter apply(Image image) {
				return texture();
			}
		});
	}

	protected final Graphics gfx;
	protected LTexture.Format texconf = LTexture.Format.LINEAR;
	protected LTexture texture;

	protected Image(Graphics gfx, GoFuture<Image> state) {
		this.gfx = gfx;
		this.state = state;
	}

	protected Image(Graphics gfx) {
		this.gfx = gfx;
		this.state = GoFuture.success(this);
	}

	public abstract void upload(Graphics gfx, LTexture tex);

	public abstract void getLight(Image buffer, int v);

	public abstract int getLight(int color, int v);

	public abstract int[] getPixels();

	public abstract int[] getPixels(int pixels[]);

	public abstract int[] getPixels(int x, int y, int w, int h);

	public abstract int[] getPixels(int offset, int stride, int x, int y, int width, int height);

	public abstract int[] getPixels(int pixels[], int offset, int stride, int x, int y, int width, int height);

	public abstract void setPixels(int[] pixels, int width, int height);

	public abstract void setPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height);

	public abstract int[] setPixels(int[] pixels, int x, int y, int w, int h);

	public abstract void setPixel(LColor c, int x, int y);

	public abstract void setPixel(int rgb, int x, int y);

	public abstract int getPixel(int x, int y);

	public abstract int getRGB(int x, int y);

	public abstract void setRGB(int rgb, int x, int y);

	public abstract void getRGB(int startX, int startY, int width, int height, int[] rgbArray, int offset,
			int scanSize);

	public abstract void setRGB(int startX, int startY, int width, int height, int[] rgbArray, int offset,
			int scanSize);

	public abstract boolean hasAlpha();

	public abstract String getSource();

	public abstract Image getSubImage(int x, int y, int width, int height);

	private boolean closed;

	public final boolean isClosed() {
		return closed;
	}

	public int getWidth() {
		return (int) width();
	}

	public int getHeight() {
		return (int) height();
	}

	public TArray<Vector2f> getPoints(final Vector2f size, final int interval, final float scale) {
		final int[] pixels = getPixels();
		final TArray<Vector2f> points = new TArray<Vector2f>();
		for (int y = 0; y < getHeight(); y += interval) {
			for (int x = 0; x < getWidth(); x += interval) {
				int tx = MathUtils.clamp(x + MathUtils.nextInt(-interval / 2, interval / 2), 0, getWidth() - 1);
				int ty = MathUtils.clamp(y + MathUtils.nextInt(-interval / 2, interval / 2), 0, getHeight() - 1);
				int color = pixels[getWidth() * ty + tx];
				if (LColor.getRed(color) == 255) {
					points.add((new Vector2f(tx, ty).sub(size)).mul(scale));
				}
			}
		}
		return points;
	}

	public String getBase64() {
		return getRGBAsToArrayByte().toString();
	}

	public ArrayByte getRGBAsToArrayByte() {
		return new ArrayByte(getRGBABytes());
	}

	public byte[] getBGRABytes() {
		return getRGBABytes(true);
	}

	public byte[] getRGBABytes() {
		return getRGBABytes(false);
	}

	public byte[] getRGBABytes(boolean flag) {
		int idx = 0;
		final int bits = 4;
		final int[] pixesl = getPixels();
		byte[] buffer = new byte[getWidth() * getHeight() * bits];
		for (int i = 0, size = buffer.length; i < size; i += bits) {
			int pixel = pixesl[idx++];
			if (flag) {
				buffer[i + 3] = (byte) (LColor.getAlpha(pixel));
				buffer[i + 2] = (byte) (LColor.getRed(pixel));
				buffer[i + 1] = (byte) (LColor.getGreen(pixel));
				buffer[i] = (byte) (LColor.getBlue(pixel));
			} else {
				buffer[i] = (byte) (LColor.getRed(pixel));
				buffer[i + 1] = (byte) (LColor.getGreen(pixel));
				buffer[i + 2] = (byte) (LColor.getBlue(pixel));
				buffer[i + 3] = (byte) (LColor.getAlpha(pixel));
			}
		}
		return buffer;
	}

	public byte[] getBGRBytes() {
		return getRGBBytes(true);
	}

	public byte[] getRGBBytes() {
		return getRGBBytes(false);
	}

	public byte[] getRGBBytes(boolean flag) {
		int idx = 0;
		final int bits = 3;
		final int[] pixesl = getPixels();
		byte[] buffer = new byte[getWidth() * getHeight() * bits];
		for (int i = 0, size = buffer.length; i < size; i += bits) {
			int pixel = pixesl[idx++];
			if (flag) {
				buffer[i + 2] = (byte) (LColor.getRed(pixel));
				buffer[i + 1] = (byte) (LColor.getGreen(pixel));
				buffer[i] = (byte) (LColor.getBlue(pixel));
			} else {
				buffer[i] = (byte) (LColor.getRed(pixel));
				buffer[i + 1] = (byte) (LColor.getGreen(pixel));
				buffer[i + 2] = (byte) (LColor.getBlue(pixel));
			}
		}
		return buffer;
	}

	public Pixmap getPixmap() {
		int[] pixels = CollectionUtils.copyOf(getPixels());
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = new LColor(pixels[i]).getARGB();
		}
		return new Pixmap(pixels, getWidth(), getHeight(), hasAlpha());
	}

	public void setPixmap(Pixmap pixmap) {
		setPixels(pixmap.getData(), pixmap.getWidth(), pixmap.getHeight());
	}

	public Image onHaveToClose(boolean c) {
		this.haveToClose = c;
		return this;
	}

	public final void close() {
		if (!this.isTexture) {
			this.closeImpl();
		} else {
			this.haveToClose = true;
		}
		this.closed = true;
	}

	public final void destroy() {
		this.closeImpl();
	}

	protected abstract void closeImpl();

	public final Image cpy() {
		return cpy(false);
	}

	public final Image cpy(boolean closed) {
		Canvas canvas = createCanvas(width(), height());
		canvas.draw(this, 0, 0);
		if (closed) {
			this.close();
		}
		return canvas.image;
	}
}
