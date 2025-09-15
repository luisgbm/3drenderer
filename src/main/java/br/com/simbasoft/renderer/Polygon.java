package br.com.simbasoft.renderer;

import java.util.List;

public class Polygon {
    private List<Vector3> vertices;
    private List<Texture> textureCoordinates;

    public List<Vector3> getVertices() {
        return vertices;
    }

    public static Polygon fromTriangle(Vector3 v0, Vector3 v1, Vector3 v2, Texture t0, Texture t1, Texture t2) {
        Polygon polygon = new Polygon();

        polygon.setVertices(List.of(v0, v1, v2));
        polygon.setTextureCoordinates(List.of(t0, t1, t2));

        return polygon;
    }

    public void setVertices(List<Vector3> vertices) {
        this.vertices = vertices;
    }

    public List<Texture> getTextureCoordinates() {
        return textureCoordinates;
    }

    public void setTextureCoordinates(List<Texture> textureCoordinates) {
        this.textureCoordinates = textureCoordinates;
    }
}
