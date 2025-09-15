package br.com.simbasoft.renderer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mesh {
    private final List<Vector3> vertices;
    private final List<Face> faces;
    private final Vector3 scale;
    private final Vector3 translation;
    private final Vector3 rotation;
    private ImageTexture texture;

    public Mesh(String objFileName, String pngFileName, Vector3 scale, Vector3 translation, Vector3 rotation) throws IOException {
        vertices = new ArrayList<>();
        faces = new ArrayList<>();

        this.scale = scale;
        this.translation = translation;
        this.rotation = rotation;

        this.loadMeshObjData(objFileName);
        this.loadMeshPngData(pngFileName);
    }

    public List<Face> getFaces() {
        return faces;
    }

    public List<Vector3> getVertices() {
        return vertices;
    }

    public void addVertex(Vector3 vertex) {
        vertices.add(vertex);
    }

    public void addFace(Face face) {
        faces.add(face);
    }

    public Vector3 getRotation() {
        return rotation;
    }

    public Vector3 getScale() {
        return scale;
    }

    public Vector3 getTranslation() {
        return translation;
    }

    private void loadMeshPngData(String filePath) throws IOException {
        this.texture = new ImageTexture(filePath);
    }

    private void loadMeshObjData(String filePath) throws IOException {
        List<Texture> texcoords = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");

                switch (parts[0]) {
                    case "v" -> {
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        double z = Double.parseDouble(parts[3]);

                        this.addVertex(new Vector3(x, y, z));
                    }
                    case "vt" -> {
                        double u = Double.parseDouble(parts[1]);
                        double v = Double.parseDouble(parts[2]);

                        texcoords.add(new Texture(u, v));
                    }
                    case "f" -> {
                        int[] vertexIndices = new int[3];
                        int[] textureIndices = new int[3];

                        String[] components1 = parts[1].split("/");
                        String[] components2 = parts[2].split("/");
                        String[] components3 = parts[3].split("/");

                        vertexIndices[0] = Integer.parseInt(components1[0]) - 1;
                        vertexIndices[1] = Integer.parseInt(components2[0]) - 1;
                        vertexIndices[2] = Integer.parseInt(components3[0]) - 1;

                        textureIndices[0] = Integer.parseInt(components1[1]) - 1;
                        textureIndices[1] = Integer.parseInt(components2[1]) - 1;
                        textureIndices[2] = Integer.parseInt(components3[1]) - 1;

                        Face face = new Face(
                                vertexIndices[0],
                                vertexIndices[1],
                                vertexIndices[2],
                                texcoords.get(textureIndices[0]),
                                texcoords.get(textureIndices[1]),
                                texcoords.get(textureIndices[2]),
                                0xFFFFFFFF
                        );

                        this.addFace(face);
                    }
                }
            }
        }
    }

    public ImageTexture getTexture() {
        return texture;
    }
}
