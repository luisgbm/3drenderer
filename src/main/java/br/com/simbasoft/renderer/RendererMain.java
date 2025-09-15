package br.com.simbasoft.renderer;

import io.github.libsdl4j.api.event.SDL_Event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.libsdl4j.api.event.SDL_EventType.SDL_KEYDOWN;
import static io.github.libsdl4j.api.event.SDL_EventType.SDL_QUIT;
import static io.github.libsdl4j.api.event.SdlEvents.SDL_PollEvent;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;
import static io.github.libsdl4j.api.timer.SdlTimer.SDL_Delay;
import static io.github.libsdl4j.api.timer.SdlTimer.SDL_GetTicks;

public class RendererMain {
    private Display display;
    private boolean isRunning;
    private Matrix4 projectionMatrix;
    private List<Mesh> meshes;
    private int previousFrameTime;
    private List<Triangle> trianglesToRender;
    private Light light;
    private Camera camera;
    private Clipping clipping;
    private double deltaTime;

    public RendererMain() throws IOException {
        setup();

        while (isRunning) {
            processInput();
            update();
            render();
        }

        display.destroyWindow();
    }

    private void render() {
        display.clearColorBuffer(0xFF000000);
        display.clearZBuffer();

        display.drawGrid(15, 0xFF303030);

        for (Triangle triangle : trianglesToRender) {
            if (display.shouldRenderFilledTriangles()) {
                display.drawFilledTriangle(triangle);
            }

            if (display.shouldRenderTexturedTriangles()) {
                display.drawTexturedTriangle(
                        (int) triangle.points()[0].getX(), (int) triangle.points()[0].getY(), triangle.points()[0].getZ(), triangle.points()[0].getW(), triangle.textureCoordinates()[0].u(), triangle.textureCoordinates()[0].v(),
                        (int) triangle.points()[1].getX(), (int) triangle.points()[1].getY(), triangle.points()[1].getZ(), triangle.points()[1].getW(), triangle.textureCoordinates()[1].u(), triangle.textureCoordinates()[1].v(),
                        (int) triangle.points()[2].getX(), (int) triangle.points()[2].getY(), triangle.points()[2].getZ(), triangle.points()[2].getW(), triangle.textureCoordinates()[2].u(), triangle.textureCoordinates()[2].v(),
                        triangle.texture()
                );
            }

            if (display.shouldRenderWireframe()) {
                display.drawTriangle(triangle, 0xFFFFFFFF);
            }

            if (display.shouldRenderVertex()) {
                for (int j = 0; j < 3; j++) {
                    Vector4 vertex = triangle.points()[j];
                    display.drawRect((int) (vertex.getX() - 3), (int) (vertex.getY() - 3), 6, 6, 0xFFFF0000);
                }
            }
        }

        display.renderColorBuffer();
    }

    // Model space -> World space -> Camera space -> Clipping -> Projection -> Image space -> Screen space
    private void processGraphicsPipelineStages(Mesh mesh) {
        Vector3 target = camera.getLookAtTarget();

        Vector3 upDirection = new Vector3(0, 1, 0);

        Matrix4 viewMatrix = Matrix4.lookAt(camera.getPosition(), target, upDirection);

        Matrix4 scaleMatrix = Matrix4.makeScale(mesh.getScale().x(), mesh.getScale().y(), mesh.getScale().z());
        Matrix4 translationMatrix = Matrix4.makeTranslation(mesh.getTranslation().x(), mesh.getTranslation().y(), mesh.getTranslation().z());
        Matrix4 rotationMatrixX = Matrix4.makeRotationX(mesh.getRotation().x());
        Matrix4 rotationMatrixY = Matrix4.makeRotationY(mesh.getRotation().y());
        Matrix4 rotationMatrixZ = Matrix4.makeRotationZ(mesh.getRotation().z());

        for (int i = 0; i < mesh.getFaces().size(); i++) {
            Face meshFace = mesh.getFaces().get(i);

            Vector3[] faceVertices = new Vector3[3];
            faceVertices[0] = mesh.getVertices().get(meshFace.a());
            faceVertices[1] = mesh.getVertices().get(meshFace.b());
            faceVertices[2] = mesh.getVertices().get(meshFace.c());

            Vector4[] transformedVertices = new Vector4[3];

            for (int j = 0; j < 3; j++) {
                Vector4 transformedVertex = Vector4.fromVector3(faceVertices[j]);

                Matrix4 worldMatrix = Matrix4.makeIdentity();

                worldMatrix = Matrix4.multiply(scaleMatrix, worldMatrix);
                worldMatrix = Matrix4.multiply(rotationMatrixX, worldMatrix);
                worldMatrix = Matrix4.multiply(rotationMatrixY, worldMatrix);
                worldMatrix = Matrix4.multiply(rotationMatrixZ, worldMatrix);
                worldMatrix = Matrix4.multiply(translationMatrix, worldMatrix);

                transformedVertex = Matrix4.multiplyVector4(worldMatrix, transformedVertex);

                transformedVertex = Matrix4.multiplyVector4(viewMatrix, transformedVertex);

                transformedVertices[j] = transformedVertex;
            }

            Vector3 faceNormal = Triangle.getTriangleNormal(transformedVertices);

            if (display.shouldCullBackfaces()) {
                Vector3 cameraRay = Vector3.subtract(new Vector3(0, 0, 0), Vector3.fromVector4(transformedVertices[0]));

                double dotNormalCamera = Vector3.dot(faceNormal, cameraRay);

                if (dotNormalCamera < 0) {
                    continue;
                }
            }

            Polygon polygon = Polygon.fromTriangle(
                    Vector3.fromVector4(transformedVertices[0]),
                    Vector3.fromVector4(transformedVertices[1]),
                    Vector3.fromVector4(transformedVertices[2]),
                    meshFace.auv(),
                    meshFace.buv(),
                    meshFace.cuv()
            );

            clipping.clipPolygon(polygon);

            List<Triangle> trianglesAfterClipping = Triangle.createTrianglesFromPolygon(polygon);

            for (Triangle triangleAfterClipping : trianglesAfterClipping) {
                Vector4[] projectedPoints = new Vector4[3];

                for (int j = 0; j < 3; j++) {
                    projectedPoints[j] = Matrix4.multiplyVector4(projectionMatrix, triangleAfterClipping.points()[j]);

                    if (projectedPoints[j].getW() != 0) {
                        projectedPoints[j].setX(projectedPoints[j].getX() / projectedPoints[j].getW());
                        projectedPoints[j].setY(projectedPoints[j].getY() / projectedPoints[j].getW());
                        projectedPoints[j].setZ(projectedPoints[j].getZ() / projectedPoints[j].getW());
                    }

                    projectedPoints[j].setY(projectedPoints[j].getY() * (-1));

                    projectedPoints[j].setX(projectedPoints[j].getX() * (display.getWindowWidth() / 2.0));
                    projectedPoints[j].setY(projectedPoints[j].getY() * (display.getWindowHeight() / 2.0));

                    projectedPoints[j].setX(projectedPoints[j].getX() + (display.getWindowWidth() / 2.0));
                    projectedPoints[j].setY(projectedPoints[j].getY() + (display.getWindowHeight() / 2.0));
                }

                double lightIntensityFactor = -Vector3.dot(faceNormal, light.direction());

                int triangleColor = Light.applyIntensity(meshFace.color(), lightIntensityFactor);

                Triangle triangleToRender = new Triangle(
                        projectedPoints,
                        triangleAfterClipping.textureCoordinates(),
                        triangleColor,
                        mesh.getTexture()
                );

                trianglesToRender.add(triangleToRender);
            }
        }
    }

    private void update() {
        int timeToWait = Display.FRAME_TARGET_TIME - (SDL_GetTicks() - previousFrameTime);

        if (timeToWait > 0 && timeToWait <= Display.FRAME_TARGET_TIME) {
            SDL_Delay(timeToWait);
        }

        deltaTime = (SDL_GetTicks() - previousFrameTime) / 1000.0;

        previousFrameTime = SDL_GetTicks();

        trianglesToRender = new ArrayList<>();

        for (Mesh mesh : meshes) {
            processGraphicsPipelineStages(mesh);
        }
    }

    private void processInput() {
        SDL_Event event = new SDL_Event();

        while (SDL_PollEvent(event) > 0) {
            switch(event.type) {
                case SDL_QUIT -> isRunning = false;
                case SDL_KEYDOWN -> {
                    switch (event.key.keysym.sym) {
                        case SDLK_ESCAPE -> isRunning = false;
                        case SDLK_1 -> display.setRenderMethod(ERenderMethod.RENDER_WIRE_VERTEX);
                        case SDLK_2 -> display.setRenderMethod(ERenderMethod.RENDER_WIRE);
                        case SDLK_3 -> display.setRenderMethod(ERenderMethod.RENDER_FILL_TRIANGLE);
                        case SDLK_4 -> display.setRenderMethod(ERenderMethod.RENDER_FILL_TRIANGLE_WIRE);
                        case SDLK_5 -> display.setRenderMethod(ERenderMethod.RENDER_TEXTURED);
                        case SDLK_6 -> display.setRenderMethod(ERenderMethod.RENDER_TEXTURED_WIRE);
                        case SDLK_C -> display.setCullMethod(ECullMethod.CULL_BACKFACE);
                        case SDLK_X -> display.setCullMethod(ECullMethod.CULL_NONE);
                        case SDLK_I -> camera.rotateCameraPitch(3.0 * deltaTime);
                        case SDLK_K -> camera.rotateCameraPitch(-3.0 * deltaTime);
                        case SDLK_L -> camera.rotateCameraYaw(deltaTime);
                        case SDLK_J -> camera.rotateCameraYaw(-1 * deltaTime);
                        case SDLK_W -> {
                            camera.updateCameraForwardVelocity(Vector3.multiply(camera.getDirection(), 5.0 * deltaTime));
                            camera.updateCameraPosition(Vector3.add(camera.getPosition(), camera.getForwardVelocity()));
                        }
                        case SDLK_S -> {
                            camera.updateCameraForwardVelocity(Vector3.multiply(camera.getDirection(), 5.0 * deltaTime));
                            camera.updateCameraPosition(Vector3.subtract(camera.getPosition(), camera.getForwardVelocity()));
                        }
                    }
                }
            }
        }
    }

    private void setup() throws IOException {
        previousFrameTime = 0;
        display = new Display();
        isRunning = true;

        display.setRenderMethod(ERenderMethod.RENDER_TEXTURED);
        display.setCullMethod(ECullMethod.CULL_BACKFACE);

        light = new Light(new Vector3(0, 0, 1));
        camera = new Camera(new Vector3(0, 0, 0), new Vector3(0, 0, 1));

        double aspectY = (double) display.getWindowHeight() / display.getWindowWidth();
        double aspectX = (double) display.getWindowWidth() / display.getWindowHeight();

        double fovY = Math.PI / 3; // 60 degrees in rad
        double fovX = 2 * Math.atan(Math.tan(fovY / 2.0) * aspectX);

        double zNear = 1;
        double zFar = 20;

        projectionMatrix = Matrix4.makePerspective(fovY, aspectY, zNear, zFar);

        clipping = new Clipping(fovX, fovY, zNear, zFar);

        meshes = new ArrayList<>();

        meshes.add(new Mesh(
                        "src/main/resources/runway.obj",
                        "src/main/resources/runway.png",
                        new Vector3(1, 1, 1),
                        new Vector3(0, -1.5, 23),
                        new Vector3(0, 0, 0)
                )
        );

        meshes.add(new Mesh(
                "src/main/resources/f22.obj",
                "src/main/resources/f22.png",
                new Vector3(1, 1, 1),
                new Vector3(0, -1.3, 5),
                new Vector3(0, -Math.PI/2, 0)
            )
        );

        meshes.add(new Mesh(
                        "src/main/resources/efa.obj",
                        "src/main/resources/efa.png",
                        new Vector3(1, 1, 1),
                        new Vector3(-2, -1.3, 9),
                        new Vector3(0, -Math.PI/2, 0)
                )
        );

        meshes.add(new Mesh(
                        "src/main/resources/f117.obj",
                        "src/main/resources/f117.png",
                        new Vector3(1, 1, 1),
                        new Vector3(2, -1.3, 9),
                        new Vector3(0, -Math.PI/2, 0)
                )
        );
    }

    public static void main(String[] args) throws IOException {
        new RendererMain();
    }
}