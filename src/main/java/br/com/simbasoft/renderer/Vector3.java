package br.com.simbasoft.renderer;

public record Vector3(double x, double y, double z) {

    public static Vector3 fromVector4(Vector4 vector4) {
        return new Vector3(
                vector4.getX(),
                vector4.getY(),
                vector4.getZ()
        );
    }

    public Vector3 normalize() {
        return Vector3.normalize(this);
    }

    public static Vector3 subtract(Vector3 a, Vector3 b) {
        return new Vector3(
                a.x - b.x,
                a.y - b.y,
                a.z - b.z
        );
    }

    public static Vector3 add(Vector3 a, Vector3 b) {
        return new Vector3(
                a.x + b.x,
                a.y + b.y,
                a.z + b.z
        );
    }

    public static Vector3 multiply(Vector3 a, double factor) {
        return new Vector3(
                a.x * factor,
                a.y * factor,
                a.z * factor
        );
    }

    public double getLength() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static Vector3 normalize(Vector3 v) {
        double length = v.getLength();

        return new Vector3(
                v.x / length,
                v.y / length,
                v.z / length
        );
    }

    public static Vector3 cross(Vector3 a, Vector3 b) {
        return new Vector3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
        );
    }

    public static double dot(Vector3 a, Vector3 b) {
        return (a.x * b.x) + (a.y * b.y) + (a.z * b.z);
    }

    public static Vector3 barycentricWeights(Vector2 a, Vector2 b, Vector2 c, Vector2 p) {
        Vector2 ac = Vector2.subtract(c, a);
        Vector2 ab = Vector2.subtract(b, a);
        Vector2 pc = Vector2.subtract(c, p);
        Vector2 pb = Vector2.subtract(b, p);
        Vector2 ap = Vector2.subtract(p, a);

        double areaParallelogramABC = (ac.x() * ab.y() - ac.y() * ab.x());
        double alpha = (pc.x() * pb.y() - pc.y() * pb.x()) / areaParallelogramABC;
        double beta = (ac.x() * ap.y() - ac.y() * ap.x()) / areaParallelogramABC;
        double gamma = 1.0 - alpha - beta;

        return new Vector3(alpha, beta, gamma);
    }
}
