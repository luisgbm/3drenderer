package br.com.simbasoft.renderer;

public record Vector2(double x, double y) {

    public static Vector2 fromVector4(Vector4 v) {
        return new Vector2(
                v.getX(),
                v.getY()
        );
    }

    public static Vector2 subtract(Vector2 a, Vector2 b) {
        return new Vector2(
                a.x - b.x,
                a.y - b.y
        );
    }
}
