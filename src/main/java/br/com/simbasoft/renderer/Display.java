package br.com.simbasoft.renderer;

import com.sun.jna.Memory;
import io.github.libsdl4j.api.render.SDL_Renderer;
import io.github.libsdl4j.api.render.SDL_Texture;
import io.github.libsdl4j.api.video.SDL_DisplayMode;
import io.github.libsdl4j.api.video.SDL_Window;

import static io.github.libsdl4j.api.Sdl.SDL_Init;
import static io.github.libsdl4j.api.Sdl.SDL_Quit;
import static io.github.libsdl4j.api.SdlSubSystemConst.SDL_INIT_EVERYTHING;
import static io.github.libsdl4j.api.mouse.SdlMouse.SDL_SetRelativeMouseMode;
import static io.github.libsdl4j.api.pixels.SDL_PixelFormatEnum.SDL_PIXELFORMAT_RGB888;
import static io.github.libsdl4j.api.render.SDL_TextureAccess.SDL_TEXTUREACCESS_STREAMING;
import static io.github.libsdl4j.api.render.SdlRender.*;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.*;
import static io.github.libsdl4j.api.video.SdlVideo.*;
import static io.github.libsdl4j.api.video.SdlVideoConst.SDL_WINDOWPOS_CENTERED;

public class Display {
    public static final int FPS = 60;
    public static final int FRAME_TARGET_TIME = 1000 / FPS;

    private SDL_Window window;
    private int windowWidth;
    private int windowHeight;
    private SDL_Renderer renderer;
    private final int[] colorBuffer;
    private final double[] zBuffer;
    private final SDL_Texture colorBufferTexture;
    private ERenderMethod renderMethod;
    private ECullMethod cullMethod;

    public Display() {
        this.initializeWindow();

        this.colorBuffer = new int[windowWidth * windowHeight];
        this.zBuffer = new double[windowWidth * windowHeight];

        this.colorBufferTexture = SDL_CreateTexture(
                renderer,
                SDL_PIXELFORMAT_RGB888,
                SDL_TEXTUREACCESS_STREAMING,
                windowWidth,
                windowHeight
        );
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    private void initializeWindow() {
        if (SDL_Init(SDL_INIT_EVERYTHING) != 0) {
            throw new RuntimeException("Error initializing SDL.");
        }

        SDL_DisplayMode displayMode = new SDL_DisplayMode();
        SDL_GetCurrentDisplayMode(0, displayMode);

        int fullScreenWidth = displayMode.w;
        int fullScreenHeight = displayMode.h;

        windowWidth = fullScreenWidth / 3;
        windowHeight = fullScreenHeight / 3;

        window = SDL_CreateWindow(
                "3drenderer",
                SDL_WINDOWPOS_CENTERED,
                SDL_WINDOWPOS_CENTERED,
                fullScreenWidth,
                fullScreenHeight,
                SDL_WINDOW_MAXIMIZED | SDL_WINDOW_RESIZABLE
        );

        if (window == null) {
            throw new RuntimeException("Error creating SDL window.");
        }

        renderer = SDL_CreateRenderer(window, -1, 0);

        if (renderer == null) {
            throw new RuntimeException("Error creating SDL renderer.");
        }

        SDL_SetWindowFullscreen(window, SDL_WINDOW_FULLSCREEN);
        SDL_SetRelativeMouseMode(true);
    }

    public void destroyWindow() {
        SDL_DestroyRenderer(renderer);
        SDL_DestroyWindow(window);
        SDL_Quit();
    }

    public void drawPixel(int x, int y, int color) {
        if (x >= 0 && y >=0 && x < windowWidth && y < windowHeight) {
            colorBuffer[(windowWidth * y) + x] = color;
        }
    }

    public void drawLine(int x0, int y0, int x1, int y1, int color) {
        int delta_x = x1 - x0;
        int delta_y = y1 - y0;

        int side_length = Math.max(Math.abs(delta_x), Math.abs(delta_y));

        double x_inc = delta_x / (double)side_length;
        double y_inc = delta_y / (double)side_length;

        double current_x = x0;
        double current_y = y0;

        for (int i = 0; i <= side_length; i++) {
            drawPixel((int) Math.round(current_x), (int) Math.round(current_y), color);
            current_x += x_inc;
            current_y += y_inc;
        }
    }

    public void drawGrid(int spacing, int color) {
        int height_spacing = windowHeight / spacing;
        int width_spacing = windowWidth / spacing;

        for (int y = 0; y < windowHeight; y += height_spacing) {
            drawLine(0, y, windowWidth, y, color);
        }

        for (int x = 0; x < windowWidth; x += width_spacing) {
            drawLine(x, 0, x, windowHeight, color);
        }

        drawLine(0, windowHeight - 1, windowWidth - 1, windowHeight - 1, color);
        drawLine(windowWidth - 1, 0, windowWidth - 1, windowHeight - 1, color);
    }

    private void renderPresent() {
        SDL_RenderPresent(renderer);
    }

    public void renderColorBuffer() {
        try (Memory colorBufferMemory = new Memory(colorBuffer.length * 4L)) {
            colorBufferMemory.write(0, colorBuffer, 0, colorBuffer.length);

            SDL_UpdateTexture(
                    colorBufferTexture,
                    null,
                    colorBufferMemory,
                    windowWidth * 4
            );

            SDL_RenderCopy(renderer, colorBufferTexture, null, null);
        }

        this.renderPresent();
    }

    public void clearColorBuffer(int color) {
        for (int y = 0; y < windowHeight; y++) {
            for (int x = 0; x < windowWidth; x++) {
                drawPixel(x, y, color);
            }
        }
    }

    public void clearZBuffer() {
        for (int y = 0; y < windowHeight; y++) {
            for (int x = 0; x < windowWidth; x++) {
                zBuffer[(windowWidth * y) + x] = 1;
            }
        }
    }

    public void drawTriangle(Triangle triangle, int color) {
        drawLine((int) triangle.points()[0].getX(), (int) triangle.points()[0].getY(), (int) triangle.points()[1].getX(), (int) triangle.points()[1].getY(), color);
        drawLine((int) triangle.points()[1].getX(), (int) triangle.points()[1].getY(), (int) triangle.points()[2].getX(), (int) triangle.points()[2].getY(), color);
        drawLine((int) triangle.points()[2].getX(), (int) triangle.points()[2].getY(), (int) triangle.points()[0].getX(), (int) triangle.points()[0].getY(), color);
    }

    public void drawRect(int x, int y, int width, int height, int color) {
        for (int j = y; j < y + height; j++) {
            for (int i = x; i < x + width; i++) {
                drawPixel(i, j, color);
            }
        }
    }

    public void drawFilledTriangle(Triangle t) {
        drawFilledTriangle(
                (int) t.points()[0].getX(), (int) t.points()[0].getY(), t.points()[0].getZ(), t.points()[0].getW(),
                (int) t.points()[1].getX(), (int) t.points()[1].getY(), t.points()[1].getZ(), t.points()[1].getW(),
                (int) t.points()[2].getX(), (int) t.points()[2].getY(), t.points()[2].getZ(), t.points()[2].getW(),
                t.color()
        );
    }

    private void drawFilledTriangle(
            int x0, int y0, double z0, double w0,
            int x1, int y1, double z1, double w1,
            int x2, int y2, double z2, double w2,
            int color
    ) {
        if (y0 > y1) {
            y1 = Swap.intSwap(y0, y0=y1);
            z1 = Swap.doubleSwap(z0, z0=z1);
            w1 = Swap.doubleSwap(w0, w0=w1);
            x1 = Swap.intSwap(x0, x0=x1);
        }

        if (y1 > y2) {
            y2 = Swap.intSwap(y1, y1=y2);
            z2 = Swap.doubleSwap(z1, z1=z2);
            w2 = Swap.doubleSwap(w1, w1=w2);
            x2 = Swap.intSwap(x1, x1=x2);
        }

        if (y0 > y1) {
            y1 = Swap.intSwap(y0, y0=y1);
            z1 = Swap.doubleSwap(z0, z0=z1);
            w1 = Swap.doubleSwap(w0, w0=w1);
            x1 = Swap.intSwap(x0, x0=x1);
        }

        Vector4 point_a = new Vector4(x0, y0, z0, w0);
        Vector4 point_b = new Vector4(x1, y1, z1, w1);
        Vector4 point_c = new Vector4(x2, y2, z2, w2);

        // Flat bottom part
        double inv_slope_1 = 0;
        double inv_slope_2 = 0;

        if (y1 - y0 != 0) inv_slope_1 = (double)(x1 - x0) / Math.abs(y1 - y0);
        if (y2 - y0 != 0) inv_slope_2 = (double)(x2 - x0) / Math.abs(y2 - y0);

        if (y1 - y0 != 0) {
            for (int y = y0; y <= y1; y++) {
                int x_start = (int) (x1 + (y - y1) * inv_slope_1);
                int x_end = (int) (x0 + (y - y0) * inv_slope_2);

                if (x_end < x_start) {
                    x_end = Swap.intSwap(x_start, x_start=x_end);
                }

                for (int x = x_start; x <= x_end; x++) {
                    drawTrianglePixel(x, y, color,
                            point_a, point_b, point_c
                    );
                }
            }
        }

        // Flat top part
        inv_slope_1 = 0;
        inv_slope_2 = 0;

        if (y2 - y1 != 0) inv_slope_1 = (float)(x2 - x1) / Math.abs(y2 - y1);
        if (y2 - y0 != 0) inv_slope_2 = (float)(x2 - x0) / Math.abs(y2 - y0);

        if (y2 - y1 != 0) {
            for (int y = y1; y <= y2; y++) {
                int x_start = (int) (x1 + (y - y1) * inv_slope_1);
                int x_end = (int) (x0 + (y - y0) * inv_slope_2);

                if (x_end < x_start) {
                    x_end = Swap.intSwap(x_start, x_start=x_end);
                }

                for (int x = x_start; x <= x_end; x++) {
                    drawTrianglePixel(x, y, color,
                            point_a, point_b, point_c
                    );
                }
            }
        }
    }

    private void drawTrianglePixel(
            int x, int y, int color,
            Vector4 pointA, Vector4 pointB, Vector4 pointC
    ) {
        Vector2 p = new Vector2(x, y);
        Vector2 a = Vector2.fromVector4(pointA);
        Vector2 b = Vector2.fromVector4(pointB);
        Vector2 c = Vector2.fromVector4(pointC);
        Vector3 weights = Vector3.barycentricWeights(a, b, c, p);

        double alpha = weights.x();
        double beta = weights.y();
        double gamma = weights.z();

        double interpolated_reciprocal_w;

        interpolated_reciprocal_w = (1.0 / pointA.getW()) * alpha + (1.0 / pointB.getW()) * beta + (1.0 / pointC.getW()) * gamma;

        interpolated_reciprocal_w = 1.0 - interpolated_reciprocal_w;

        if (interpolated_reciprocal_w < getZBufferAt(x, y)) {
            drawPixel(x, y, color);
            updateZBufferAt(x, y, interpolated_reciprocal_w);
        }
    }

    private void drawTexel(
        int x, int y, ImageTexture texture,
        Vector4 pointA, Vector4 pointB, Vector4 pointC,
        Texture a_uv, Texture b_uv, Texture c_uv
    ) {
        Vector2 p = new Vector2(x, y);
        Vector2 a = Vector2.fromVector4(pointA);
        Vector2 b = Vector2.fromVector4(pointB);
        Vector2 c = Vector2.fromVector4(pointC);
        Vector3 weights = Vector3.barycentricWeights(a, b, c, p);

        double alpha = weights.x();
        double beta = weights.y();
        double gamma = weights.z();

        double interpolated_u;
        double interpolated_v;
        double interpolated_reciprocal_w;

        interpolated_u = (a_uv.u() / pointA.getW()) * alpha + (b_uv.u() / pointB.getW()) * beta + (c_uv.u() / pointC.getW()) * gamma;
        interpolated_v = (a_uv.v() / pointA.getW()) * alpha + (b_uv.v() / pointB.getW()) * beta + (c_uv.v() / pointC.getW()) * gamma;

        interpolated_reciprocal_w = (1.0 / pointA.getW()) * alpha + (1.0 / pointB.getW()) * beta + (1.0 / pointC.getW()) * gamma;

        interpolated_u /= interpolated_reciprocal_w;
        interpolated_v /= interpolated_reciprocal_w;

        int textureWidth = texture.getWidth();
        int textureHeight = texture.getHeight();

        int tex_x = Math.abs((int)(interpolated_u * textureWidth)) % textureWidth;
        int tex_y = Math.abs((int)(interpolated_v * textureHeight)) % textureHeight;

        interpolated_reciprocal_w = 1.0 - interpolated_reciprocal_w;

        if (interpolated_reciprocal_w < getZBufferAt(x, y)) {
            drawPixel(x, y, texture.getMeshTexture()[(textureWidth * tex_y) + tex_x]);
            updateZBufferAt(x, y, interpolated_reciprocal_w);
        }
    }

    private double getZBufferAt(int x, int y) {
        if (x < 0 || x >= windowWidth || y < 0 || y >= windowHeight) {
            return 1.0;
        }

        return zBuffer[(windowWidth * y) + x];
    }

    private void updateZBufferAt(int x, int y, double z) {
        if (x < 0 || x >= windowWidth || y < 0 || y >= windowHeight) {
            return;
        }

        zBuffer[(windowWidth * y) + x] = z;
    }

    public void drawTexturedTriangle(
        int x0, int y0, double z0, double w0, double u0, double v0,
        int x1, int y1, double z1, double w1, double u1, double v1,
        int x2, int y2, double z2, double w2, double u2, double v2,
        ImageTexture texture
    ) {
        if (y0 > y1) {
            y1 = Swap.intSwap(y0, y0=y1);
            v1 = Swap.doubleSwap(v0, v0=v1);
            z1 = Swap.doubleSwap(z0, z0=z1);
            w1 = Swap.doubleSwap(w0, w0=w1);
            x1 = Swap.intSwap(x0, x0=x1);
            u1 = Swap.doubleSwap(u0, u0=u1);
        }

        if (y1 > y2) {
            y2 = Swap.intSwap(y1, y1=y2);
            v2 = Swap.doubleSwap(v1, v1=v2);
            z2 = Swap.doubleSwap(z1, z1=z2);
            w2 = Swap.doubleSwap(w1, w1=w2);
            x2 = Swap.intSwap(x1, x1=x2);
            u2 = Swap.doubleSwap(u1, u1=u2);
        }

        if (y0 > y1) {
            y1 = Swap.intSwap(y0, y0=y1);
            v1 = Swap.doubleSwap(v0, v0=v1);
            z1 = Swap.doubleSwap(z0, z0=z1);
            w1 = Swap.doubleSwap(w0, w0=w1);
            x1 = Swap.intSwap(x0, x0=x1);
            u1 = Swap.doubleSwap(u0, u0=u1);
        }

        v0 = 1.0 - v0;
        v1 = 1.0 - v1;
        v2 = 1.0 - v2;

        Vector4 point_a = new Vector4(x0, y0, z0, w0);
        Vector4 point_b = new Vector4(x1, y1, z1, w1);
        Vector4 point_c = new Vector4(x2, y2, z2, w2);

        Texture a_uv = new Texture(u0, v0);
        Texture b_uv = new Texture(u1, v1);
        Texture c_uv = new Texture(u2, v2);

        // Flat bottom part
        double inv_slope_1 = 0;
        double inv_slope_2 = 0;

        if (y1 - y0 != 0) inv_slope_1 = (double)(x1 - x0) / Math.abs(y1 - y0);
        if (y2 - y0 != 0) inv_slope_2 = (double)(x2 - x0) / Math.abs(y2 - y0);

        if (y1 - y0 != 0) {
            for (int y = y0; y <= y1; y++) {
                int x_start = (int) (x1 + (y - y1) * inv_slope_1);
                int x_end = (int) (x0 + (y - y0) * inv_slope_2);

                if (x_end < x_start) {
                    x_end = Swap.intSwap(x_start, x_start=x_end);
                }

                for (int x = x_start; x <= x_end; x++) {
                    drawTexel(x, y, texture,
                            point_a, point_b, point_c,
                            a_uv, b_uv, c_uv
                    );
                }
            }
        }

        // Flat top part
        inv_slope_1 = 0;
        inv_slope_2 = 0;

        if (y2 - y1 != 0) inv_slope_1 = (float)(x2 - x1) / Math.abs(y2 - y1);
        if (y2 - y0 != 0) inv_slope_2 = (float)(x2 - x0) / Math.abs(y2 - y0);

        if (y2 - y1 != 0) {
            for (int y = y1; y <= y2; y++) {
                int x_start = (int) (x1 + (y - y1) * inv_slope_1);
                int x_end = (int) (x0 + (y - y0) * inv_slope_2);

                if (x_end < x_start) {
                    x_end = Swap.intSwap(x_start, x_start=x_end);
                }

                for (int x = x_start; x <= x_end; x++) {
                    drawTexel(x, y, texture,
                            point_a, point_b, point_c,
                            a_uv, b_uv, c_uv
                    );
                }
            }
        }
    }

    public void setRenderMethod(ERenderMethod renderMethod) {
        this.renderMethod = renderMethod;
    }

    public void setCullMethod(ECullMethod cullMethod) {
        this.cullMethod = cullMethod;
    }

    public boolean shouldRenderFilledTriangles() {
        return renderMethod == ERenderMethod.RENDER_FILL_TRIANGLE || renderMethod == ERenderMethod.RENDER_FILL_TRIANGLE_WIRE;
    }

    public boolean shouldRenderTexturedTriangles() {
        return renderMethod == ERenderMethod.RENDER_TEXTURED || renderMethod == ERenderMethod.RENDER_TEXTURED_WIRE;
    }

    public boolean shouldRenderWireframe() {
        return (
                renderMethod == ERenderMethod.RENDER_WIRE ||
                renderMethod == ERenderMethod.RENDER_WIRE_VERTEX ||
                renderMethod == ERenderMethod.RENDER_FILL_TRIANGLE_WIRE ||
                renderMethod == ERenderMethod.RENDER_TEXTURED_WIRE
        );
    }

    public boolean shouldRenderVertex() {
        return renderMethod == ERenderMethod.RENDER_WIRE_VERTEX;
    }

    public boolean shouldCullBackfaces() {
        return cullMethod == ECullMethod.CULL_BACKFACE;
    }
}
