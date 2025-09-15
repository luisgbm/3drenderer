package br.com.simbasoft.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Clipping {
    private Map<EFrustumPlane, Plane> frustumPlanes;

    public Clipping(double fovX, double fovY, double zNear, double zFar) {
        this.initFrustumPlanes(fovX, fovY, zNear, zFar);
    }

    private void initFrustumPlanes(double fovX, double fovY, double zNear, double zFar) {
        frustumPlanes = new HashMap<>();

        double cosHalfFovY = Math.cos(fovY / 2);
        double sinHalfFovY = Math.sin(fovY / 2);

        double cosHalfFovX = Math.cos(fovX / 2);
        double sinHalfFovX = Math.sin(fovX / 2);

        Plane leftFrustumPlane = new Plane(
                new Vector3(0, 0, 0),
                new Vector3(cosHalfFovX, 0, sinHalfFovX)
        );

        frustumPlanes.put(EFrustumPlane.LEFT_FRUSTUM_PLANE, leftFrustumPlane);

        Plane rightFrustumPlane = new Plane(
                new Vector3(0, 0, 0),
                new Vector3(-cosHalfFovX, 0, sinHalfFovX)
        );

        frustumPlanes.put(EFrustumPlane.RIGHT_FRUSTUM_PLANE, rightFrustumPlane);

        Plane topFrustumPlane = new Plane(
                new Vector3(0, 0, 0),
                new Vector3(0, -cosHalfFovY, sinHalfFovY)
        );

        frustumPlanes.put(EFrustumPlane.TOP_FRUSTUM_PLANE, topFrustumPlane);

        Plane bottomFrustumPlane = new Plane(
                new Vector3(0, 0, 0),
                new Vector3(0, cosHalfFovY, sinHalfFovY)
        );

        frustumPlanes.put(EFrustumPlane.BOTTOM_FRUSTUM_PLANE, bottomFrustumPlane);

        Plane nearFrustumPlane = new Plane(
                new Vector3(0, 0, zNear),
                new Vector3(0, 0, 1)
        );

        frustumPlanes.put(EFrustumPlane.NEAR_FRUSTUM_PLANE, nearFrustumPlane);

        Plane farFrustumPlane = new Plane(
                new Vector3(0, 0, zFar),
                new Vector3(0, 0, -1)
        );

        frustumPlanes.put(EFrustumPlane.FAR_FRUSTUM_PLANE, farFrustumPlane);
    }

    double doubleLerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public void clipPolygon(Polygon polygon) {
        for (Plane plane : frustumPlanes.values()) {
            clipPolygonAgainstPlane(polygon, plane);
        }
    }

    private void clipPolygonAgainstPlane(Polygon polygon, Plane plane) {
        if (polygon.getVertices().isEmpty()) {
            return;
        }

        Vector3 planePoint = plane.point();
        Vector3 planeNormal = plane.normal();

        List<Vector3> insideVertices = new ArrayList<>();
        List<Texture> insideTextureCoordinates = new ArrayList<>();

        Vector3 previousVertex = polygon.getVertices().get(polygon.getVertices().size() - 1);
        Texture previousTextureCoordinate = polygon.getTextureCoordinates().get(polygon.getTextureCoordinates().size() - 1);

        double previousDot = Vector3.dot(Vector3.subtract(previousVertex, planePoint), planeNormal);

        for (int i = 0; i < polygon.getVertices().size(); i++) {
            Vector3 currentVertex = polygon.getVertices().get(i);
            Texture currentTextureCoordinate = polygon.getTextureCoordinates().get(i);

            double currentDot = Vector3.dot(Vector3.subtract(currentVertex, planePoint), planeNormal);

            if (currentDot * previousDot < 0) {
                double t = previousDot / (previousDot - currentDot);

                Vector3 intersectionPoint = new Vector3(
                        doubleLerp(previousVertex.x(), currentVertex.x(), t),
                        doubleLerp(previousVertex.y(), currentVertex.y(), t),
				        doubleLerp(previousVertex.z(), currentVertex.z(), t)
                );

                Texture interpolatedTextureCoordinate = new Texture(
                        doubleLerp(previousTextureCoordinate.u(), currentTextureCoordinate.u(), t),
                        doubleLerp(previousTextureCoordinate.v(), currentTextureCoordinate.v(), t)
                );

                insideVertices.add(intersectionPoint);
                insideTextureCoordinates.add(interpolatedTextureCoordinate);
            }

            if (currentDot > 0) {
                insideVertices.add(currentVertex);
                insideTextureCoordinates.add(currentTextureCoordinate);
            }

            previousDot = currentDot;

            previousVertex = currentVertex;
            previousTextureCoordinate = currentTextureCoordinate;
        }

        polygon.setVertices(insideVertices);
        polygon.setTextureCoordinates(insideTextureCoordinates);
    }
}
