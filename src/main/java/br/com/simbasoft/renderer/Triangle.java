package br.com.simbasoft.renderer;

import java.util.ArrayList;
import java.util.List;

public record Triangle(
        Vector4[] points,
        Texture[] textureCoordinates,
        int color,
        ImageTexture texture) {

    public Triangle(Vector4[] points, Texture[] textureCoordinates) {
        this(points, textureCoordinates, 0, null);
    }

    public static List<Triangle> createTrianglesFromPolygon(Polygon polygon) {
        List<Triangle> triangles = new ArrayList<>();

        for (int i = 0; i < polygon.getVertices().size() - 2; i++) {
            int index0 = 0;
            int index1 = i + 1;
            int index2 = i + 2;

            Vector4[] points = new Vector4[]{
                    Vector4.fromVector3(polygon.getVertices().get(index0)),
                    Vector4.fromVector3(polygon.getVertices().get(index1)),
                    Vector4.fromVector3(polygon.getVertices().get(index2))
            };

            Texture[] textureCoordinates = new Texture[]{
                    polygon.getTextureCoordinates().get(index0),
                    polygon.getTextureCoordinates().get(index1),
                    polygon.getTextureCoordinates().get(index2)
            };

            Triangle t = new Triangle(
                    points,
                    textureCoordinates
            );

            triangles.add(t);
        }

        return triangles;
    }

    public static Vector3 getTriangleNormal(Vector4[] transformedVertices) {
        Vector3 vertexA = Vector3.fromVector4(transformedVertices[0]);
        Vector3 vertexB = Vector3.fromVector4(transformedVertices[1]);
        Vector3 vertexC = Vector3.fromVector4(transformedVertices[2]);

        Vector3 vectorAB = Vector3.subtract(vertexB, vertexA);
        vectorAB = Vector3.normalize(vectorAB);

        Vector3 vectorAC = Vector3.subtract(vertexC, vertexA);
        vectorAC = Vector3.normalize(vectorAC);

        Vector3 normal = Vector3.cross(vectorAB, vectorAC);
        normal = Vector3.normalize(normal);

        return normal;
    }
}
